package net.blitzcube.mlapi.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.blitzcube.mlapi.util.packet.entity.FakeEntity;
import net.blitzcube.mlapi.util.packet.entity.object.FakeObject;

/**
 * Class by iso2013 © 2017.
 * <p>
 * Licensed under LGPLv3. See https://opensource.org/licenses/lgpl-3.0.html for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivative works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class PacketUtil {

    private static final Map<EntityType, Integer> OBJECTS = new HashMap<>();

    static {
        OBJECTS.put(EntityType.BOAT, 1);
        OBJECTS.put(EntityType.DROPPED_ITEM, 2);
        OBJECTS.put(EntityType.AREA_EFFECT_CLOUD, 3);
        OBJECTS.put(EntityType.MINECART, 10);
        OBJECTS.put(EntityType.PRIMED_TNT, 50);
        OBJECTS.put(EntityType.ENDER_CRYSTAL, 51);
        OBJECTS.put(EntityType.TIPPED_ARROW, 60);
        OBJECTS.put(EntityType.SNOWBALL, 61);
        OBJECTS.put(EntityType.EGG, 62);
        OBJECTS.put(EntityType.FIREBALL, 63);
        OBJECTS.put(EntityType.SMALL_FIREBALL, 64);
        OBJECTS.put(EntityType.ENDER_PEARL, 65);
        OBJECTS.put(EntityType.WITHER_SKULL, 66);
        OBJECTS.put(EntityType.SHULKER_BULLET, 67);
        OBJECTS.put(EntityType.FALLING_BLOCK, 70);
        OBJECTS.put(EntityType.ITEM_FRAME, 71);
        OBJECTS.put(EntityType.ENDER_SIGNAL, 72);
        OBJECTS.put(EntityType.SPLASH_POTION, 73);
        OBJECTS.put(EntityType.THROWN_EXP_BOTTLE, 75);
        OBJECTS.put(EntityType.FIREWORK, 76);
        OBJECTS.put(EntityType.LEASH_HITCH, 77);
        OBJECTS.put(EntityType.ARMOR_STAND, 78);
        OBJECTS.put(EntityType.FISHING_HOOK, 90);
        OBJECTS.put(EntityType.SPECTRAL_ARROW, 91);
        OBJECTS.put(EntityType.DRAGON_FIREBALL, 93);  
    }

    private static ProtocolManager manager;
    private static Logger errorLogger;
    private static Field entityID;
    private static int fallbackId = -1;
    private static Method getISNMSCopy;

    public static void init(ProtocolManager manager, Logger errorLogger, boolean itemSupport) {
        PacketUtil.manager = manager;
        PacketUtil.errorLogger = errorLogger;
        String version = Bukkit.getServer().getClass().getPackage().getName();
        version = version.substring(version.lastIndexOf('.') + 1);

        try {
            entityID = Class.forName("net.minecraft.server." + version + ".Entity").getDeclaredField("entityCount");
            if (!entityID.isAccessible()) entityID.setAccessible(true);
            if (!itemSupport) return;

            getISNMSCopy = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack").getDeclaredMethod("asNMSCopy", ItemStack.class);
        } catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
            entityID = null;
            getISNMSCopy = null;
        }
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, Location l) {
        return getSpawnPacket(entity, l, entity.getMetadata() != null ? entity.getMetadata() : new WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, Location l, WrappedDataWatcher metadata) {
        return getSpawnPacket(entity, l.getX(), l.getY(), l.getZ(), l.getPitch(), l.getYaw(), metadata);
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z) {
        return getSpawnPacket(entity, x, y, z, 0.0, 0.0, entity.getMetadata() != null ? entity.getMetadata() : new WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z, WrappedDataWatcher metadata) {
        return getSpawnPacket(entity, x, y, z, 0.0, 0.0, metadata);
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z, double pitch, double yaw) {
        return getSpawnPacket(entity, x, y, z, pitch, yaw, entity.getMetadata() != null ? entity.getMetadata() : new WrappedDataWatcher());
    }

    public static PacketContainer[] getSpawnPacket(FakeEntity entity, double x, double y, double z, double pitch, double yaw, WrappedDataWatcher metadata) {
        PacketContainer packet = new PacketContainer(entity instanceof FakeObject
                ? PacketType.Play.Server.SPAWN_ENTITY
                : PacketType.Play.Server.SPAWN_ENTITY_LIVING);

        packet.getIntegers().write(0, entity.getEntityId());
        packet.getModifier().writeDefaults();
        packet.getUUIDs().write(0, entity.getUniqueId());
        packet.getDoubles().write(0, x);
        packet.getDoubles().write(1, y);
        packet.getDoubles().write(2, z);

        if (entity instanceof FakeObject) {
            packet.getIntegers().write(6, (int) entity.getType().getTypeId());
            packet.getIntegers().write(4, (int) (pitch * 256.0F / 360.0F));
            packet.getIntegers().write(5, (int) (yaw * 256.0F / 360.0F));
            packet.getIntegers().write(7, ((FakeObject) entity).getObjectData());
            return new PacketContainer[]{packet, getMetadataPacket(entity)};
        } else {
            packet.getIntegers().write(1, (int) entity.getType().getTypeId());
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
        packet.getModifier().write(0, entity.getEntityId());
        packet.getWatchableCollectionModifier().write(0, entity.getMetadata().getWatchableObjects());
        return packet;
    }

    public static PacketContainer getPassengerPacket(int entity, FakeEntity passenger) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.MOUNT);
        packet.getIntegers().write(0, entity);
        packet.getIntegerArrays().write(0, new int[]{passenger.getEntityId()});
        return packet;
    }

    public static PacketContainer getDespawnPacket(FakeEntity... entities) {
        int[] ids = Arrays.stream(entities).mapToInt(FakeEntity::getEntityId).toArray();
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getModifier().writeDefaults();
        packet.getIntegerArrays().write(0, ids);
        return packet;
    }

    public static WrappedDataWatcher createWatcher(Map<Integer, Object> data) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();

        for (Map.Entry<Integer, Object> e : data.entrySet()) {
            if (e.getValue().getClass() == org.bukkit.inventory.ItemStack.class) {
                if (getISNMSCopy == null) throw new IllegalArgumentException("Not enabled for item support.");

                try {
                    watcher.setObject(e.getKey(), WrappedDataWatcher.Registry.getItemStackSerializer(false),
                            getISNMSCopy.invoke(null, e.getValue()));
                } catch (IllegalAccessException | InvocationTargetException e1) {
                    e1.printStackTrace();
                }

                continue;
            }
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
            errorLogger.log(importance, "Failed to send packet of type " + packet.getType().name() + " to player " + destination.getName() + "!");
            e.printStackTrace();
        }

        return false;
    }

}