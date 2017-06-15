package net.blitzcube.mlapi.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class PacketUtil {
    private static final Map<EntityType, Integer> objects = new HashMap<EntityType, Integer>() {{
        put(EntityType.BOAT, 1);
        put(EntityType.DROPPED_ITEM, 2);
        put(EntityType.AREA_EFFECT_CLOUD, 3);
        put(EntityType.MINECART, 10);
        put(EntityType.PRIMED_TNT, 50);
        put(EntityType.ENDER_CRYSTAL, 51);
        put(EntityType.TIPPED_ARROW, 60);
        put(EntityType.SNOWBALL, 61);
        put(EntityType.EGG, 62);
        put(EntityType.FIREBALL, 63);
        put(EntityType.SMALL_FIREBALL, 64);
        put(EntityType.ENDER_PEARL, 65);
        put(EntityType.WITHER_SKULL, 66);
        put(EntityType.SHULKER_BULLET, 67);
        put(EntityType.FALLING_BLOCK, 70);
        put(EntityType.ITEM_FRAME, 71);
        put(EntityType.ENDER_SIGNAL, 72);
        put(EntityType.SPLASH_POTION, 73);
        put(EntityType.THROWN_EXP_BOTTLE, 75);
        put(EntityType.FIREWORK, 76);
        put(EntityType.LEASH_HITCH, 77);
        put(EntityType.ARMOR_STAND, 78);
        put(EntityType.FISHING_HOOK, 90);
        put(EntityType.SPECTRAL_ARROW, 91);
        put(EntityType.DRAGON_FIREBALL, 93);
    }};
    private static ProtocolManager manager;
    private static Logger errorLogger;

    public static void init(ProtocolManager manager, Logger errorLogger) {
        PacketUtil.manager = manager;
        PacketUtil.errorLogger = errorLogger;
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, Location l) {
        return getSpawnPacket(entity, l, entity.watcher != null ? entity.watcher : new WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, Location l, WrappedDataWatcher metadata) {
        return getSpawnPacket(entity, l.getX(), l.getY(), l.getZ(), l.getPitch(), l.getYaw(), metadata);
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z) {
        return getSpawnPacket(entity, x, y, z, 0.0, 0.0, entity.watcher != null ? entity.watcher : new
                WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z,
                                                   WrappedDataWatcher metadata) {
        return getSpawnPacket(entity, x, y, z, 0.0, 0.0, metadata);
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z, double pitch,
                                                   double yaw) {
        return getSpawnPacket(entity, x, y, z, pitch, yaw, entity.watcher != null ? entity.watcher : new
                WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z, double pitch,
                                                   double yaw, WrappedDataWatcher metadata) {
        PacketContainer packet = new PacketContainer(entity.object ? PacketType.Play.Server.SPAWN_ENTITY : PacketType
                .Play.Server.SPAWN_ENTITY_LIVING);
        packet.getIntegers().write(0, entity.entityId);
        packet.getModifier().writeDefaults();
        packet.getUUIDs().write(0, entity.uniqueId);
        packet.getDoubles().write(0, x);
        packet.getDoubles().write(1, y);
        packet.getDoubles().write(2, z);
        if (entity.object) {
            packet.getIntegers().write(6, entity.typeId);
            packet.getIntegers().write(4, (int) (pitch * 256.0F / 360.0F));
            packet.getIntegers().write(5, (int) (yaw * 256.0F / 360.0F));
            packet.getIntegers().write(7, entity.objectData);
            return new PacketContainer[]{packet, getMetadataPacket(entity)};
        } else {
            packet.getIntegers().write(1, (int) entity.type.getTypeId());
            packet.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
            packet.getBytes().write(1, (byte) (pitch * 256.0F / 360.0F));
            packet.getBytes().write(2, (byte) (pitch * 256.0F / 360.0F));
            packet.getIntegers().write(2, 0);
            packet.getIntegers().write(3, 0);
            packet.getIntegers().write(4, 0);
            packet.getDataWatcherModifier().write(0, metadata);
        }
        return new PacketContainer[]{packet};
    }

    public static PacketContainer getMetadataPacket(FakeEntity entity) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getModifier().writeDefaults();
        packet.getModifier().write(0, entity.entityId);
        packet.getWatchableCollectionModifier().write(0, entity.watcher.getWatchableObjects());
        return packet;
    }

    public static PacketContainer getPassengerPacket(int entity, int... passengers) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.MOUNT);
        packet.getIntegers().write(0, entity);
        packet.getIntegerArrays().write(0, passengers);
        return packet;
    }

    public static PacketContainer getDespawnPacket(FakeEntity... entities) {
        int[] entityIds = new int[entities.length];
        for (int i = 0; i < entities.length; i++) entityIds[i] = entities[i].entityId;
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getModifier().writeDefaults();
        packet.getIntegerArrays().write(0, entityIds);
        return packet;
    }

    public static WrappedDataWatcher createWatcher(Map<Integer, Object> data) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        for (Map.Entry<Integer, Object> e : data.entrySet()) {
            watcher.setObject(e.getKey(), WrappedDataWatcher.Registry.get(e.getValue().getClass()), e.getValue());
        }
        return watcher;
    }

    public static boolean trySend(PacketContainer packet, Player destination, Level importance, boolean filters) {
        if (destination == null) return false;
        try {
            manager.sendServerPacket(destination, packet, filters);
            return true;
        } catch (InvocationTargetException e) {
            errorLogger.log(importance, "Failed to send packet of type " + packet.getType().name() + " to player " +
                    destination.getName() + "!");
            e.printStackTrace();
        }
        return false;
    }


    public static class FakeEntity {
        private static int lastId = -1;

        private final int entityId, typeId, objectData;
        private final UUID uniqueId;
        private final EntityType type;
        private final boolean object;

        private WrappedDataWatcher watcher;

        public FakeEntity(EntityType type) {
            this(type, new WrappedDataWatcher());
        }

        public FakeEntity(EntityType type, WrappedDataWatcher metadata) {
            this(type, 0, metadata);
        }

        public FakeEntity(EntityType type, int objectData) {
            this(type, objectData, new WrappedDataWatcher());
        }

        public FakeEntity(EntityType type, int objectData, WrappedDataWatcher metadata) {
            lastId = lastId - 1;
            this.entityId = lastId;
            this.uniqueId = UUID.randomUUID();
            this.type = type;
            this.typeId = objects.containsKey(type) ? objects.get(type) : type.getTypeId();
            this.object = objects.containsKey(type);
            this.objectData = objectData;
            this.watcher = metadata;
        }

        public void setWatcher(WrappedDataWatcher watcher) {
            this.watcher = watcher;
        }

        public int getEntityId() {
            return entityId;
        }

        public UUID getUniqueId() {
            return uniqueId;
        }

        public EntityType getType() {
            return type;
        }
    }

}
