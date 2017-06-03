package net.blitzcube.mlapi.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 5/4/2017.
 */
public class EntityUtil {
    private static Map<String, Integer> entityDistances;

    public static void init() {
        if (entityDistances != null) return;
        entityDistances = Maps.newHashMap();
        ConfigurationSection section = Bukkit.spigot().getConfig().getConfigurationSection("world-settings");
        ConfigurationSection defaultSec = section.getConfigurationSection("default.entity-tracking-range");
        for (String s : section.getKeys(false)) {
            ConfigurationSection world = section.getConfigurationSection(s + ".entity-tracking-range");
            int[] ranges = new int[]{
                    world.contains("players") ? world.getInt("players") : defaultSec.getInt("players"),
                    world.contains("animals") ? world.getInt("animals") : defaultSec.getInt("animals"),
                    world.contains("monsters") ? world.getInt("monsters") : defaultSec.getInt("monsters"),
                    world.contains("misc") ? world.getInt("misc") : defaultSec.getInt("misc"),
                    world.contains("other") ? world.getInt("other") : defaultSec.getInt("other")
            };
            int max = 0;
            for (int range : ranges) {
                if (range > max) max = range;
            }
            entityDistances.putIfAbsent(s, max);
        }
    }

    public static Stream<Entity> getEntities(Entity forWho, double distanceMultiplier, int... entityIds) {
        double highest = entityDistances.containsKey(forWho.getWorld().getName()) ?
                entityDistances.get(forWho.getWorld().getName()) : entityDistances.get("default");
        highest *= distanceMultiplier;
        List<Integer> ids = Lists.newArrayList();
        for (int i : entityIds) ids.add(i);
        return forWho.getNearbyEntities(highest, highest, highest).stream()
                .filter(entity -> ids.size() <= 0 || ids.contains(entity.getEntityId()));
    }

    public static Stream<Entity> getEntities(Entity forWho, double distanceMultiplier, UUID uuid) {
        double highest = entityDistances.containsKey(forWho.getWorld().getName()) ?
                entityDistances.get(forWho.getWorld().getName()) : entityDistances.get("default");
        highest *= distanceMultiplier;
        return forWho.getNearbyEntities(highest, highest, highest).stream()
                .filter(entity -> entity.getUniqueId().equals(uuid));
    }
}
