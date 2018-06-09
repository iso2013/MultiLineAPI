package net.blitzcube.mlapi.api.tag;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Created by iso2013 on 5/24/2018.
 */
public interface ITagController {
    List<TagLine> getFor(Entity target);

    String getName(Entity target, Player viewer);

    EntityType[] getAutoApplyFor();

    JavaPlugin getPlugin();

    int getPriority();

    int getNamePriority();

    interface TagLine {
        String getText(Entity target, Player viewer);

        boolean keepSpaceWhenNull(Entity target);
    }
}
