package net.blitzcube.mlapi.example;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
}
