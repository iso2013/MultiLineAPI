package net.blitzcube.mlapi.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.collect.Maps;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.kitteh.vanish.VanishPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Created by iso2013 on 1/26/2017.
 */
public class PacketListener implements com.comphenix.protocol.events.PacketListener {
    public final MultiLineAPI inst;
    public final ProtocolManager protocol;
    public VanishManager vnsh;
    private Map<String, Integer> trackingRanges;

    public PacketListener(MultiLineAPI inst) {
        this.inst = inst;
        this.protocol = ProtocolLibrary.getProtocolManager();
        this.protocol.addPacketListener(this);
        this.vnsh = new VanishManager(inst);
        this.trackingRanges = Maps.newHashMap();

        ConfigurationSection section = Bukkit.spigot().getSpigotConfig().getConfigurationSection("world-settings");
        for (String s : section.getKeys(false)) {
            trackingRanges.put(s, section.getInt(s + ".entity-tracking-range.players"));
        }
    }

    @Override
    public void onPacketSending(PacketEvent e) {
        PacketContainer p = e.getPacket();
        //Handle entity spawns:
        if (p.getType().equals(PacketType.Play.Server.SPAWN_ENTITY)
                || p.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_LIVING)
                || p.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_PAINTING)
                || p.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            UUID entity = p.getUUIDs().read(0);
            if (inst.tags.containsKey(entity)) {
                Tag t = inst.tags.get(entity);
                if (e.getPlayer() != null && vnsh.canSee(t.getOwner(), e.getPlayer())) {
                    Bukkit.getScheduler().runTaskLater(inst, () -> {
                        //t.updateEntityLoc();
                        //t.getProtocol().sendPairs(e.getPlayer(), this, true);
                        MultiLineAPI.refreshOthers(e.getPlayer());
                        t.getProtocol().unhideTags(e.getPlayer(), inst.pckt);
                    }, 2L);
                }
            }
            return;
        }
        //Handle entity mount changes:
        if (p.getType().equals(PacketType.Play.Server.MOUNT)) {
            int base = p.getIntegers().read(0);
            if (base == e.getPlayer().getEntityId()) return;
            int range = getMaxTrackingRange(e.getPlayer().getWorld().getName());
            range += range * 0.2;
            Optional<Entity> entity = e.getPlayer().getNearbyEntities(range, 250, range).stream()
                    .filter(en -> en.getEntityId() == base)
                    .filter(en -> en.getWorld().getUID().equals(e.getPlayer().getWorld().getUID()))
                    .findAny();
            if (!entity.isPresent()) return;
            if (!inst.tags.containsKey(entity.get().getUniqueId())) return;
            Tag t;
            if ((t = inst.tags.get(entity.get().getUniqueId())) != null) {
                int[] passengers = p.getIntegerArrays().read(0);
                if (passengers.length == 0) {
                    e.setCancelled(true);
                    Bukkit.getScheduler().runTask(inst, () -> {
                        t.respawn();
                        t.getProtocol().sendPairs(e.getPlayer(), this, false);
                    });
                } else {
                    if (IntStream.of(passengers).filter(i -> !ArrayUtils.contains(t.getEntities(), i)).findAny()
                            .isPresent()) {
                        t.despawn();
                    }
                }
            }
            return;
        }

        //Handle entity visibility changes:
        if (p.getType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            int range = getMaxTrackingRange(e.getPlayer().getWorld().getName());
            range *= 1.2;
            int entityId = p.getIntegers().read(0);
            Optional<Entity> entity = e.getPlayer().getNearbyEntities(range, 250, range).stream()
                    .filter(en -> en.getEntityId() == entityId)
                    .filter(en -> en.getWorld().getUID().equals(e.getPlayer().getWorld().getUID()))
                    .findAny();
            if (!entity.isPresent()) return;
            if (!inst.tags.containsKey(entity.get().getUniqueId())) return;
            List<WrappedWatchableObject> l = p.getWatchableCollectionModifier().read(0);
            byte val = -1;
            for (WrappedWatchableObject o : l) {
                if (o.getValue().getClass().equals(Byte.class)) {
                    val = (byte) o.getValue();
                }
            }
            if (val == -1) return;
            if ((val & 0x20) != 0) { //Entity is being made invisible
                Bukkit.getScheduler().runTask(inst, () -> inst.tags.get(entity.get().getUniqueId()).despawn());
            } else { //Entity is being made visible
                Bukkit.getScheduler().runTask(inst, () -> {
                    inst.tags.get(entity.get().getUniqueId()).respawn();
                    MultiLineAPI.refresh(entity.get());
                });
            }
        }

        //Handle entity despawning
        if (p.getType().equals(PacketType.Play.Server.ENTITY_DESTROY)) {
            int[] toDestroy = p.getIntegerArrays().read(0);
            for (int id : toDestroy) {
                int range = getMaxTrackingRange(e.getPlayer().getWorld().getName());
                range *= 1.2;
                Optional<Entity> entity = e.getPlayer().getNearbyEntities(range, 250, range).stream()
                        .filter(en -> en.getEntityId() == id)
                        .filter(en -> en.getWorld().getUID().equals(e.getPlayer().getWorld().getUID()))
                        .findAny();
                if (!entity.isPresent()) continue;
                if (!inst.tags.containsKey(entity.get().getUniqueId())) continue;
                for (int toHide : inst.tags.get(entity.get().getUniqueId()).getEntities()) {
                    PacketContainer hidePacket = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    WrappedDataWatcher watcher = new WrappedDataWatcher();
                    WrappedDataWatcher.WrappedDataWatcherObject object = new WrappedDataWatcher
                            .WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class));
                    watcher.setObject(object, new WrappedWatchableObject(object, false));
                    hidePacket.getIntegers().write(0, toHide);
                    hidePacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                    try {
                        protocol.sendServerPacket(e.getPlayer(), hidePacket);
                    } catch (InvocationTargetException e1) {
                        Bukkit.getLogger().info("Failed to send a hide packet to " + e.getPlayer().getName());
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.MOUNT,
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_DESTROY
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return null;
    }

    public int getMaxTrackingRange(String name) {
        Integer dist = trackingRanges.get(name);
        if (dist == null) dist = trackingRanges.get("default");
        return dist;
    }

    public class VanishManager {
        boolean vanishNoPacket;

        org.kitteh.vanish.VanishManager manager;

        VanishManager(JavaPlugin parent) {
            vanishNoPacket = parent.getServer().getPluginManager().isPluginEnabled("VanishNoPacket");
            if (vanishNoPacket) {
                manager = ((VanishPlugin) parent.getServer().getPluginManager().getPlugin("VanishNoPacket"))
                        .getManager();
            }
        }

        public boolean canSee(Entity who, Player forWho) {
            if (who instanceof Player) {
                //Check VanishNoPacket
                if (manager != null) {
                    if (manager.isVanished((Player) who)) return false;
                }
                //Check Bukkit's vanish system
                if (!forWho.canSee((Player) who)) return false;
                //Check potion effects
                if (Bukkit.getPlayer(who.getUniqueId()) != null) {
                    if (((Player) who).hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
                }
                //Disable tags for users in spectator mode
                if (forWho.getGameMode() == GameMode.SPECTATOR) return false;
                if (((Player) who).getGameMode() == GameMode.SPECTATOR) return false;
                return true;
            } else {
                //Disable tags for users in spectator mode
                if (forWho.getGameMode() == GameMode.SPECTATOR) return false;
                return true;
            }
        }
    }
}
