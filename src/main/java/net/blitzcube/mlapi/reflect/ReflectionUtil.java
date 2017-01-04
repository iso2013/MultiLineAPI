package net.blitzcube.mlapi.reflect;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Created by iso2013 on 1/3/2017.
 */
public class ReflectionUtil {
    private static Class craftWorld;
    private static Method getHandle;
    private static Field entitiesByUUID;
    private static Field bukkitEntity;

    private static void init() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName();
        nmsVersion = nmsVersion.substring(nmsVersion.lastIndexOf('.'));
        craftWorld = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".CraftServer");
        getHandle = craftWorld.getDeclaredMethod("getHandle");
        entitiesByUUID = Class.forName("net.minecraft.server." + nmsVersion + ".WorldServer").getField
                ("entitiesByUUID");
        bukkitEntity = Class.forName("net.minecraft.server." + nmsVersion + ".Entity").getField("bukkitEntity");
        if (!entitiesByUUID.isAccessible()) {
            entitiesByUUID.setAccessible(true);
        }
    }

    public static Entity getEntityByUUID(World fromWhere, UUID id) {
        boolean paper = false;
        for (Method m : Bukkit.class.getDeclaredMethods()) {
            if (m.getName().equals("getEntity") && m.getParameterCount() == 1) {
                paper = true;
                break;
            }
        }
        if (paper) {
            Bukkit.getLogger().info("Using paper...");
            return Bukkit.getEntity(id);
        }
        try {
            if (craftWorld == null) {
                init();
            }
            Object o = craftWorld.cast(fromWhere);
            o = getHandle.invoke(o);
            o = entitiesByUUID.get(o);
            Map m = (Map) o;
            o = m.get(id);
            return (Entity) bukkitEntity.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bukkit.getLogger().info("derpederpudurp");
        return null;
    }
}
