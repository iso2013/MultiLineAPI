package net.blitzcube.mlapi.listener;

import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.api.MLAPI;
import net.blitzcube.mlapi.util.EntityUtil;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class EventListener implements Listener {

    private final MultiLineAPI plugin;
    private final PacketListener packet;

    public EventListener(MultiLineAPI plugin, PacketListener packet) {
        this.plugin = plugin;
        this.packet = packet;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (plugin.shouldAutoEnable()) {
            MLAPI.enable(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        if (plugin.shouldAutoDisable()) {
            MLAPI.disable(e.getPlayer());
        }
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SPECTATOR && e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            this.packet.despawnAllStacks(e.getPlayer());
        }
        else if (e.getNewGameMode() != GameMode.SPECTATOR && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            this.packet.spawnAllStacks(e.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Stream<Player> players = EntityUtil.getEntities(e.getEntity(), 1)
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity);
        Bukkit.getScheduler().runTaskLater(packet.getPlugin(),
                () -> players.forEach(player -> packet.despawnStack(player, e.getEntity().getUniqueId())), 20L);
    }

}
