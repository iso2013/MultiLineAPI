package net.iso2013.mlapi.demo;

import net.iso2013.mlapi.api.tag.TagController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Created by iso2013 on 8/18/2018.
 */
public class DemoLine implements TagController.TagLine {

    @Override
    public String getText(Entity target, Player viewer) {
        return "Hello, " + viewer.getName() + "!";
    }

    @Override
    public boolean keepSpaceWhenNull(Entity target) {
        return false;
    }
}