package net.blitzcube.mlapi.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ScheduledPacket;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.HitboxUtil;
import net.blitzcube.mlapi.util.PacketUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class PacketListener implements com.comphenix.protocol.events.PacketListener {
    private static final String INVISIBLE_CONST = "MLAPI_INVISIBLE";
    private static final double LINE_OFFSET = 0.065;
    private static final double LINE_START = 0.065;
    private final MultiLineAPI plugin;
    private final Map<Integer, UUID> tagMap;

    public PacketListener(MultiLineAPI plugin) {
        this.plugin = plugin;
        this.tagMap = Maps.newHashMap();
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(this);
        PacketUtil.init(manager, plugin.getLogger(), false);
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player p = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();
        if (Bukkit.getPlayer(p.getUniqueId()) == null) return;
        if (packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY)
                || packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_LIVING)
                || packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_PAINTING)
                || packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)
                || packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB)
                || packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            spawnStack(p, EntityUtil.getEntities(p, 1.05, packet.getIntegers().read(0)).findAny().orElse(null))
                    .forEach(packetContainer -> packetEvent.schedule(new ScheduledPacket(packetContainer, p, false)));
        } else if (packet.getType().equals(PacketType.Play.Server.MOUNT)) {
            Entity e = EntityUtil.getEntities(p, 1.05, packet.getIntegers().read(0)).findAny().orElse(null);
            if (e == null) return;
            int[] passengers = packet.getIntegerArrays().read(0);
            List<PacketUtil.FakeEntity> stack = plugin.tags.get(e.getUniqueId()).last();
            if (stack == null || stack.isEmpty()) return;
            if (passengers.length == 0) {
                spawnStack(p, e).forEach(packetContainer -> packetEvent.schedule(new ScheduledPacket(packetContainer,
                        p, false)));
            } else if (passengers.length > 1 || passengers.length == 1 && stack.size() >= 1 && stack.get(0)
                    .getEntityId() != passengers[0]) {
                despawnStack(p, e.getUniqueId());
            }
        } else if (packet.getType().equals(PacketType.Play.Server.ENTITY_DESTROY)) {
            EntityUtil.getEntities(p, 1, packet.getIntegerArrays().read(0))
                    .filter(e -> plugin.tags.containsKey(e.getUniqueId()))
                    .forEach(e -> despawnStack(p, e.getUniqueId()));
        } else if (packet.getType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            boolean invisible = VisibilityUtil.isMetadataInvisible(packet.getWatchableCollectionModifier().read(0));
            Entity e = EntityUtil.getEntities(p, 1, packet.getIntegers().read(0)).findAny().orElse(null);
            if (e == null) return;
            boolean current = e.hasMetadata(INVISIBLE_CONST) && e.getMetadata(INVISIBLE_CONST).get(0).asBoolean();
            e.removeMetadata(INVISIBLE_CONST, this.plugin);
            if (invisible != current) {
                if (invisible) {
                    despawnStack(p, e.getUniqueId());
                } else {
                    Set<PacketContainer> pc = spawnStack(p, e);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> pc.forEach(packetContainer -> PacketUtil.trySend
                            (packetContainer, p, Level.SEVERE, false)), 1L);
                }
                e.setMetadata(INVISIBLE_CONST, new FixedMetadataValue(this.plugin, invisible));
            }
        }
    }

    void spawnAllStacks(Player forWho) {
        Set<PacketContainer> spawnPackets = Sets.newHashSet();
        EntityUtil.getEntities(forWho, 1.05)
                .filter(entity -> plugin.tags.containsKey(entity.getUniqueId()))
                .forEach(entity -> spawnPackets.addAll(spawnStack(forWho, entity, true)));
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnPackets.forEach(c -> PacketUtil.trySend(c, forWho,
                Level.WARNING, false)), 1L);
    }

    void despawnAllStacks(Player forWho) {
        Set<PacketUtil.FakeEntity> mount = Sets.newHashSet();
        EntityUtil.getEntities(forWho, 1.05)
                .filter(entity -> plugin.tags.containsKey(entity.getUniqueId()))
                .forEach(entity -> mount.addAll(plugin.tags.get(entity.getUniqueId()).last()));
        PacketUtil.trySend(
                PacketUtil.getDespawnPacket(mount.toArray(new PacketUtil.FakeEntity[mount.size()])),
                forWho, Level.SEVERE,
                false
        );
    }

    public void spawnStackAndSend(Player forWho, Entity forWhat) {
        Set<PacketContainer> spawnPackets = Sets.newHashSet();
        spawnPackets.addAll(spawnStack(forWho, forWhat));
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnPackets.forEach(c -> PacketUtil.trySend(c, forWho,
                Level.SEVERE, false)), 1L);
    }

    public Set<PacketContainer> spawnStack(Player forWho, Entity forWhat) {
        return spawnStack(forWho, forWhat, false);
    }

    private Set<PacketContainer> spawnStack(Player forWho, Entity forWhat, boolean bypassGamemode) {
        if (forWhat == null || !plugin.tags.containsKey(forWhat.getUniqueId())
                || plugin.tags.get(forWhat.getUniqueId()).isVisible(forWho.getUniqueId())
                || !VisibilityUtil.isViewable(forWho, forWhat, bypassGamemode))
            return Sets.newHashSet();
        Tag.TagRender render = plugin.tags.get(forWhat.getUniqueId()).render(forWhat, forWho);
        Set<PacketContainer> delayed = Sets.newHashSet();
        render.getRemoved().forEach((e) -> this.tagMap.remove(e.getEntityId()));
        PacketUtil.trySend(PacketUtil.getDespawnPacket(render.getRemoved().toArray(new PacketUtil.FakeEntity[render
                .getRemoved().size()])), forWho, Level.SEVERE, false);
        PacketUtil.FakeEntity last = null;
        double yOffset = LINE_START;
        for (PacketUtil.FakeEntity e : render.getEntities()) {
            this.tagMap.putIfAbsent(e.getEntityId(), forWhat.getUniqueId());
            for (PacketContainer c :
                    PacketUtil.getSpawnPacket(e, (forWhat instanceof LivingEntity ?
                            ((LivingEntity) forWhat).getEyeLocation() : forWhat.getLocation()).clone().add(0,
                            yOffset, 0)
                    )) {
                PacketUtil.trySend(c, forWho, Level.SEVERE, false);
            }
            PacketContainer p;
            if (last != null) {
                p = PacketUtil.getPassengerPacket(last.getEntityId(), e);
            } else {
                p = PacketUtil.getPassengerPacket(forWhat.getEntityId(), e);
            }
            if (p != null) delayed.add(p);
            last = e;
            yOffset += LINE_OFFSET;
        }
        return delayed;
    }

    public void refreshLines(Player forWho, Entity forWhat) {
        if (forWhat == null || !plugin.tags.containsKey(forWhat.getUniqueId())
                || plugin.tags.get(forWhat.getUniqueId()).isVisible(forWho.getUniqueId())
                || !VisibilityUtil.isViewable(forWho, forWhat, false))
            return;
        for (PacketContainer c : plugin.tags.get(forWhat.getUniqueId()).refreshLines(forWhat, forWho)) {
            PacketUtil.trySend(c, forWho, Level.WARNING, false);
        }
    }

    public void refreshName(Player forWho, Entity forWhat) {
        if (forWhat == null || !plugin.tags.containsKey(forWhat.getUniqueId())
                || plugin.tags.get(forWhat.getUniqueId()).isVisible(forWho.getUniqueId())
                || !VisibilityUtil.isViewable(forWho, forWhat, false))
            return;
        PacketUtil.trySend(plugin.tags.get(forWhat.getUniqueId()).refreshName(forWhat, forWho), forWho, Level
                .WARNING, false);
    }

    public void despawnStack(Player forWho, UUID forWhat) {
        if (!plugin.tags.containsKey(forWhat)) return;
        List<PacketUtil.FakeEntity> stack = plugin.tags.get(forWhat).last();
        if (stack == null || stack.isEmpty()) return;
        PacketUtil.trySend(
                PacketUtil.getDespawnPacket(stack.toArray(new PacketUtil.FakeEntity[stack.size()])),
                forWho,
                Level.SEVERE,
                false
        );
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player p = packetEvent.getPlayer();
        PacketContainer packet = packetEvent.getPacket();
        if (packet.getType().equals(PacketType.Play.Client.USE_ENTITY)) {
            UUID u = tagMap.get(packet.getIntegers().read(0));
            if (u == null) return;
            Entity e = p.getNearbyEntities(8, 8, 8).stream().filter(entity -> entity.getUniqueId().equals(u)).findAny
                    ().orElse(null);
            if (e == null) return;
            if (!HitboxUtil.isLookingAt(p, e)) return;
            packet.getIntegers().write(0, e.getEntityId());
            if (e.getType() == EntityType.ARMOR_STAND)
                packet.getEntityUseActions().write(0, EnumWrappers.EntityUseAction.ATTACK);
        }
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
                PacketType.Play.Server.SPAWN_ENTITY_WEATHER,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.MOUNT,
                PacketType.Play.Server.ENTITY_DESTROY,
                PacketType.Play.Server.ENTITY_METADATA
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Client.USE_ENTITY
        ).build();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}
