package net.blitzcube.mlapi.demo;

import net.blitzcube.mlapi.api.tag.ITagController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Created by iso2013 on 8/18/2018.
 */
public class DemoLine implements ITagController.TagLine {

    @Override
    public String getText(Entity target, Player viewer) {
        return "Hello, " + viewer.getName() + "!";
    }

    @Override
    public boolean keepSpaceWhenNull(Entity target) {
        return false;
    }
}