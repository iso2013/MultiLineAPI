package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import java.util.stream.Stream;

/**
 * Created by iso2013 on 5/4/2017.
 */
public class EventListener implements Listener {
    private PacketListener packet;

    public EventListener(PacketListener packet) {
        this.packet = packet;
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode().equals(GameMode.SPECTATOR) && !e.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
            packet.despawnAllStacks(e.getPlayer());
        } else if (!e.getNewGameMode().equals(GameMode.SPECTATOR) && e.getPlayer().getGameMode().equals(GameMode
                .SPECTATOR)) {
            packet.spawnAllStacks(e.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Stream<Player> players = EntityUtil.getEntities(e.getEntity(), 1).filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity);
        Bukkit.getScheduler().runTaskLater(packet.getPlugin(), () -> players.forEach(player -> packet.despawnStack
                (player, e.getEntity())), 20L);
    }
}
