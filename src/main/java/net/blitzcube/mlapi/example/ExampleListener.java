package net.blitzcube.mlapi.example;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Created by iso2013 on 6/2/2017.
 */
public class ExampleListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Tag t = new Tag();
        MultiLineAPI.tags.put(e.getPlayer().getUniqueId(), t);
        t.tagControllers.add(new ExampleTagController());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Pig pig = (Pig) e.getPlayer().getWorld().spawnEntity(e.getPlayer().getLocation(), EntityType.PIG);
        pig.setAI(false);
        Tag t = new Tag();
        MultiLineAPI.tags.put(pig.getUniqueId(), t);
        t.tagControllers.add(new ExampleTagController());
    }
}
