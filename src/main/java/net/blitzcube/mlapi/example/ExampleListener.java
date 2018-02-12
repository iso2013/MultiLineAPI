package net.blitzcube.mlapi.example;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import net.blitzcube.mlapi.api.MLAPI;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class ExampleListener implements Listener {

    private ExampleTagController tg = new ExampleTagController();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        MLAPI.enable(e.getPlayer());
        MLAPI.addTagControllers(e.getPlayer(), new ExampleSecondTagController(), tg);
    }

    @EventHandler
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent e) {
        tg.lastMessage = e.getMessage();
        MLAPI.refresh(e.getPlayer());
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MultiLineAPI"),
                () -> {
                    tg.lastMessage = null;
                    MLAPI.refresh(e.getPlayer());
                }, 200L
        );
    }
}
