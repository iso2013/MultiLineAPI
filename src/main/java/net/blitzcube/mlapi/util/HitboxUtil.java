package net.blitzcube.mlapi.util;

import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class HitboxUtil {
    private static Map<EntityType, BoundingBoxWrapper> boxes = Maps.newHashMap();

    //Algorithm adapted from http://gamedev.stackexchange.com/a/18459
    public static boolean isLookingAt(Player player, Entity entity) {
        if (!boxes.containsKey(entity.getType()) || boxes.get(entity.getType()) == null)
            boxes.put(entity.getType(), BoundingBoxWrapper.getBB(entity));
        BoundingBoxWrapper box = boxes.get(entity.getType());
        if (box == null) return false;
        box = box.shift(entity.getLocation());
        Vector facing = player.getEyeLocation().getDirection().normalize();
        Vector origin = player.getEyeLocation().toVector();
        Vector divided = new Vector(1, 1, 1).divide(facing);
        double t1 = (box.min.getX() - origin.getX()) * divided.getX();
        double t2 = (box.max.getX() - origin.getX()) * divided.getX();
        double t3 = (box.min.getY() - origin.getY()) * divided.getY();
        double t4 = (box.max.getY() - origin.getY()) * divided.getY();
        double t5 = (box.min.getZ() - origin.getZ()) * divided.getZ();
        double t6 = (box.max.getZ() - origin.getZ()) * divided.getZ();
        double tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        return tMax > 0 && tMin < tMax;
    }

    private static class BoundingBoxWrapper {
        private static final Class<?> craftEntity;
        private static final Method getHandle;
        private static final Method getBB;
        private static final Field[] coordinates;

        static {
            Class<?> tempCraftEntity;
            Method tempGetHandle;
            Method tempGetBB;
            Field[] tempCoordinates;
            try {
                String version = Bukkit.getServer().getClass().getPackage().getName();
                version = version.substring(version.lastIndexOf('.') + 1);
                tempCraftEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
                tempGetHandle = tempCraftEntity.getDeclaredMethod("getHandle");
                tempGetBB = Class.forName("net.minecraft.server." + version + ".Entity").getDeclaredMethod
                        ("getBoundingBox");
                Class<?> clazz = Class.forName("net.minecraft.server." + version + ".AxisAlignedBB");
                tempCoordinates = new Field[]{
                        clazz.getDeclaredField("a"),
                        clazz.getDeclaredField("b"),
                        clazz.getDeclaredField("c"),
                        clazz.getDeclaredField("d"),
                        clazz.getDeclaredField("e"),
                        clazz.getDeclaredField("f"),
                };
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
                tempCraftEntity = null;
                tempGetHandle = null;
                tempGetBB = null;
                tempCoordinates = null;
            }
            craftEntity = tempCraftEntity;
            getHandle = tempGetHandle;
            getBB = tempGetBB;
            coordinates = tempCoordinates;
        }

        Vector min;
        Vector max;

        public BoundingBoxWrapper(Vector min, Vector max) {
            this.min = min;
            this.max = max;
        }

        public static BoundingBoxWrapper getBB(Entity forWhat) {
            try {
                Object aaBb = getBB.invoke(getHandle.invoke(craftEntity.cast(forWhat)));
                double[] c = new double[]{
                        (double) coordinates[0].get(aaBb),
                        (double) coordinates[1].get(aaBb),
                        (double) coordinates[2].get(aaBb),
                        (double) coordinates[3].get(aaBb),
                        (double) coordinates[4].get(aaBb),
                        (double) coordinates[5].get(aaBb)
                };
                BoundingBoxWrapper box = new BoundingBoxWrapper(new Vector(c[0], c[1], c[2]), new Vector(c[3], c[4],
                        c[5]));
                box.unshift(forWhat.getLocation());
                return box;
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void unshift(Location location) {
            this.min.subtract(location.toVector());
            this.max.subtract(location.toVector());
        }

        private BoundingBoxWrapper shift(Location location) {
            return new BoundingBoxWrapper(this.min.clone().add(location.toVector()), this.max.clone().add(location
                    .toVector()));
        }
    }
}
