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

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by iso2013 on 5/4/2017.
 */
public class PacketListener implements com.comphenix.protocol.events.PacketListener {
    private final String INVISIBLE_CONST = "MLAPI_INVISIBLE";
    private MultiLineAPI plugin;
    private ProtocolManager manager;
    private Map<Integer, UUID> tagMap;

    public PacketListener(MultiLineAPI plugin) {
        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();
        this.tagMap = Maps.newHashMap();
        this.manager.addPacketListener(this);
        PacketUtil.init(this.manager);
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
            spawnStack(p, EntityUtil.getEntities(p, 1, packet.getIntegers().read(0)).findAny().orElse(null))
                    .forEach(packetContainer -> packetEvent.schedule(new ScheduledPacket(packetContainer, p, false)));
        } else if (packet.getType().equals(PacketType.Play.Server.MOUNT)) {
            Entity e = EntityUtil.getEntities(p, 1, packet.getIntegers().read(0)).findAny().orElse(null);
            if (e == null) return;
            int[] passengers = packet.getIntegerArrays().read(0);
            LinkedList<PacketUtil.FakeEntity> stack = plugin.tags.get(e.getUniqueId()).last();
            if (stack == null || stack.isEmpty()) return;
            if (passengers.length == 0) {
                spawnStack(p, e).forEach(packetContainer -> packetEvent.schedule(new ScheduledPacket(packetContainer,
                        p, false)));
            } else if (passengers.length > 1 || passengers.length == 1 && stack.size() >= 1 && stack.get(0)
                    .getEntityId() != passengers[0]) {
                despawnStack(p, e);
            }
        } else if (packet.getType().equals(PacketType.Play.Server.ENTITY_DESTROY)) {
            EntityUtil.getEntities(p, 1, packet.getIntegerArrays().read(0))
                    .filter(entity -> plugin.tags.containsKey(entity.getUniqueId()))
                    .forEach(entity -> despawnStack(p, entity));
        } else if (packet.getType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            boolean invisible = VisibilityUtil.isMetadataInvisible(packet.getWatchableCollectionModifier().read(0));
            Entity e = EntityUtil.getEntities(p, 1, packet.getIntegers().read(0)).findAny().orElse(null);
            if (e == null) return;
            boolean current = e.hasMetadata(INVISIBLE_CONST) && e.getMetadata(INVISIBLE_CONST).get(0).asBoolean();
            e.removeMetadata(INVISIBLE_CONST, this.plugin);
            if (invisible != current) {
                if (invisible) {
                    despawnStack(p, e);
                } else {
                    spawnStack(p, e).forEach(packetContainer -> packetEvent.schedule(new ScheduledPacket
                            (packetContainer,
                            p, false)));
                }
                e.setMetadata(INVISIBLE_CONST, new FixedMetadataValue(this.plugin, invisible));
            }
        }
    }

    void spawnAllStacks(Player forWho, boolean bypassGamemode) {
        Set<PacketContainer> spawnPackets = Sets.newHashSet();
        EntityUtil.getEntities(forWho, 1)
                .filter(entity -> plugin.tags.containsKey(entity.getUniqueId()))
                .forEach(entity -> spawnPackets.addAll(spawnStack(forWho, entity, bypassGamemode)));
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnPackets.forEach(packetContainer -> {
            try {
                manager.sendServerPacket(forWho, packetContainer);
            } catch (InvocationTargetException e) {
                Bukkit.getLogger().info("Failed to send mount packet to " + forWho.getName());
            }
        }), 1L);
    }

    void despawnAllStacks(Player forWho) {
        Set<PacketUtil.FakeEntity> mount = Sets.newHashSet();
        EntityUtil.getEntities(forWho, 1.1)
                .filter(entity -> plugin.tags.containsKey(entity.getUniqueId()))
                .forEach(entity -> mount.addAll(plugin.tags.get(entity.getUniqueId()).last()));
        try {
            manager.sendServerPacket(forWho, PacketUtil.getDespawnPacket(mount.toArray(new PacketUtil
                    .FakeEntity[mount.size()])));
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Set<PacketContainer> spawnStack(Player forWho, Entity forWhat) {
        return spawnStack(forWho, forWhat, false);
    }

    private Set<PacketContainer> spawnStack(Player forWho, Entity forWhat, boolean bypassGamemode) {
        if (forWhat == null || !plugin.tags.containsKey(forWhat.getUniqueId()) || !VisibilityUtil.isViewable(forWho,
                forWhat, bypassGamemode))
            return Sets.newHashSet();
        LinkedList<PacketUtil.FakeEntity> stack = plugin.tags.get(forWhat.getUniqueId()).render(forWhat, forWho);
        Set<PacketContainer> mount = Sets.newHashSet();
        PacketUtil.FakeEntity last = null;
        for (PacketUtil.FakeEntity e : stack) {
            this.tagMap.put(e.getEntityId(), forWhat.getUniqueId());
            for (PacketContainer c : PacketUtil.getSpawnPacket(e, forWhat instanceof LivingEntity ? ((LivingEntity)
                    forWhat).getEyeLocation() : forWhat.getLocation())) {
                try {
                    manager.sendServerPacket(forWho, c);
                } catch (InvocationTargetException e1) {
                    plugin.getLogger().severe("Failed to send spawn packet to " + forWho.getName());
                }
            }
            if (last != null) {
                mount.add(PacketUtil.getPassengerPacket(last.getEntityId(), e.getEntityId()));
            } else {
                mount.add(PacketUtil.getPassengerPacket(forWhat.getEntityId(), e.getEntityId()));
            }
            last = e;
        }
        return mount;
    }

    void despawnStack(Player forWho, Entity forWhat) {
        if (!plugin.tags.containsKey(forWhat.getUniqueId())) return;
        LinkedList<PacketUtil.FakeEntity> stack = plugin.tags.get(forWhat.getUniqueId()).last();
        if (stack == null || stack.isEmpty()) return;
        try {
            manager.sendServerPacket(forWho, PacketUtil.getDespawnPacket(stack.toArray(new PacketUtil
                    .FakeEntity[stack.size()])), false);
        } catch (InvocationTargetException e) {
            plugin.getLogger().severe("Failed to send de-spawn packet to " + forWho.getName());
        }
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
