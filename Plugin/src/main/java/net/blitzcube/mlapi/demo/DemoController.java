package net.blitzcube.mlapi.demo;

import net.blitzcube.mlapi.api.tag.ITagController;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Created by iso2013 on 8/18/2018.
 */
public class DemoController implements ITagController {
    private final JavaPlugin parent;
    private final ITagController.TagLine line;

    public DemoController(JavaPlugin parent) {
        this.parent = parent;
        this.line = new DemoLine();
    }

    @Override
    public List<TagLine> getFor(Entity target) {
        return Collections.singletonList(this.line);
    }

    @Override
    public String getName(Entity target, Player viewer, String previous) {
        if (previous == null) return null;
        return ChatColor.BLUE + previous;
    }

    @Override
    public EntityType[] getAutoApplyFor() {
        return new EntityType[]{
                EntityType.PLAYER
        };
    }

    @Override
    public JavaPlugin getPlugin() {
        return parent;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getNamePriority() {
        return 0;
    }
}