package net.blitzcube.mlapi.util;

import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.kitteh.vanish.VanishPlugin;

import java.util.List;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class VisibilityUtil {
    static boolean vanishNoPacket;
    static org.kitteh.vanish.VanishManager manager;

    public static void init(JavaPlugin parent) {
        vanishNoPacket = parent.getServer().getPluginManager().isPluginEnabled("VanishNoPacket");
        if (vanishNoPacket) {
            manager = ((VanishPlugin) parent.getServer().getPluginManager().getPlugin("VanishNoPacket")).getManager();
        }
    }

    public static boolean isMetadataInvisible(List<WrappedWatchableObject> metadata) {
        WrappedWatchableObject ob = metadata.stream().filter(wrappedWatchableObject -> wrappedWatchableObject
                .getIndex() == 0).findAny().orElse(null);
        if (ob == null) return false;
        return (((Byte) ob.getRawValue()) & 0x20) > 0;
    }

    public static boolean isViewable(Player viewer, Entity target, boolean bypassGamemode) {
        if (target instanceof Player) {
            if (vanishNoPacket) {
                if (manager.isVanished((Player) target)) return false;
            }
            if (!viewer.canSee((Player) target)) return false;
            if (((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
        }
        if (target instanceof LivingEntity) {
            if (((LivingEntity) target).hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
        }
        if (viewer.getGameMode() == GameMode.SPECTATOR && !bypassGamemode) return false;
        return true;
    }
}
