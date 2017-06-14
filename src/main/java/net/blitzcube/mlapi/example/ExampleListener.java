package net.blitzcube.mlapi.example;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class ExampleListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Tag t = new Tag();
        MultiLineAPI.tags.put(e.getPlayer().getUniqueId(), t);
        t.tagControllers.add(new ExampleTagController());
        t.tagControllers.add(new ExampleSecondTagController());
    }
}
