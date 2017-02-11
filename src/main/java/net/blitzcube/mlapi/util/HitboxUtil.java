package net.blitzcube.mlapi.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by iso2013 on 2/3/2017.
 */
public class HitboxUtil {
    private static final Class<?> craftEntity;
    private static final Method getHandle;
    private static final Method getBB;

    static {
        Class<?> tempCraftEntity;
        Method tempGetHandle;
        Method tempGetBB;
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);
            tempGetBB = Class.forName("net.minecraft.server." + version + ".Entity").getDeclaredMethod
                    ("getBoundingBox");
            tempCraftEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
            tempGetHandle = tempCraftEntity.getDeclaredMethod("getHandle");
            if (!tempGetBB.isAccessible()) tempGetBB.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            tempCraftEntity = null;
            tempGetHandle = null;
            tempGetBB = null;
            e.printStackTrace();
        }
        craftEntity = tempCraftEntity;
        getHandle = tempGetHandle;
        getBB = tempGetBB;
    }

    //Algorithm adapted from http://gamedev.stackexchange.com/a/18459
    public static boolean isLookingAt(Player player, Entity entity) {
        try {
            BoundingBoxWrapper wrapped = new BoundingBoxWrapper(getBB.invoke(getHandle.invoke(craftEntity.cast
                    (entity))).toString(), entity);
            wrapped.scale();
            Vector facing = player.getEyeLocation().getDirection().normalize();
            Vector origin = player.getEyeLocation().toVector();
            Vector divided = new Vector(1, 1, 1).divide(facing);
            double t1 = (wrapped.min.getX() - origin.getX()) * divided.getX();
            double t2 = (wrapped.max.getX() - origin.getX()) * divided.getX();
            double t3 = (wrapped.min.getY() - origin.getY()) * divided.getY();
            double t4 = (wrapped.max.getY() - origin.getY()) * divided.getY();
            double t5 = (wrapped.min.getZ() - origin.getZ()) * divided.getZ();
            double t6 = (wrapped.max.getZ() - origin.getZ()) * divided.getZ();
            double tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
            double tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
            if (tMax > 0 && tMin < tMax) {
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e1) {
            e1.printStackTrace();
            return false;
        }
        return false;
    }

    private static class BoundingBoxWrapper {
        Vector min;
        Vector max;

        public BoundingBoxWrapper(String aaBB, Entity forWhat) {
            String[] data = aaBB.replace("box[", "").replace("]", "").replace(" -> ", ",").replace(", ", ",").split
                    (",");
            this.min = new Vector(Float.valueOf(data[0]), Float.valueOf(data[1]), Float.valueOf(data[2]));
            this.max = new Vector(Float.valueOf(data[3]), Float.valueOf(data[4]), Float.valueOf(data[5]));
            if (forWhat instanceof LivingEntity) {
                min.setY((float) forWhat.getLocation().getY());
                max.setY((float) (min.getY() + ((LivingEntity) forWhat).getEyeHeight()));
            }
        }

        public void scale() {
            Vector center = min.clone().add(max).divide(new Vector(2, 2, 2));
            Vector diff = max.clone().subtract(min).multiply(1.25).multiply(0.5);
            min = center.clone().subtract(diff);
            max = center.add(diff);
        }
    }
}
