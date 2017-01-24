package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class EventListener implements Listener {
    //The instance of the API for referencing hide methods
    private final MultiLineAPI inst;
    //Whether or not new players should be automatically enabled
    private boolean autoEnable;

    //Constructor just accepts the API to store in the inst variable
    public EventListener(MultiLineAPI parent) {
        this.inst = parent;
        //By default, auto enabling is on.
        autoEnable = true;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!autoEnable) return;

        //If auto joining is enabled, then schedule a task
        Bukkit.getScheduler().runTaskLater(inst, () -> {
            //Enable the player with MultiLineAPI. This has to be done one tick later so all players receive the
            // entities.
            MultiLineAPI.enable(e.getPlayer());
            //Refresh the player to send the mount packets to all players.
            MultiLineAPI.refresh(e.getPlayer());
            //Hide the entities one tick later
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                //This has to be done one tick later to ensure the e.getPlayer() has received all the entities -
                // otherwise some will hide and some won't.
                inst.hide(e.getPlayer());
            }, 2L);
        }, 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (MultiLineAPI.isEnabled(e.getEntity())) {
            Bukkit.getScheduler().runTaskLater(inst, () -> inst.tags.get(e.getEntity().getUniqueId()).tempDisable(),
                    20L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                //For some reason this must be done twice... Maybe it has to do with the entity spawning times?
                inst.tags.get(e.getPlayer().getUniqueId()).reEnable();
                inst.tags.get(e.getPlayer().getUniqueId()).tempDisable();
                inst.tags.get(e.getPlayer().getUniqueId()).reEnable();
                //This number (2L) CANNOT be higher in my testing. Setting it to 10L resulted in no reset of the API.
            }, 2L);
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        //If the player is enabled, then disable the player.
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.disable(e.getPlayer());
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        //If the entity is a member of a player's stack, stop all damage to it.
        if (e.getEntity().hasMetadata("STACK_ENTITY")) e.setCancelled(true);
    }

    @EventHandler
    public void death(EntityDeathEvent e) {
        if (e.getEntity().hasMetadata("STACK_ENTITY")) {
            e.setDroppedExp(0);
            UUID u = (UUID) e.getEntity().getMetadata("STACK_ENTITY").get(0).value();
            Tag t = inst.tags.get(u);
            if (t != null) {
                t.tempDisable();
                Bukkit.getScheduler().runTaskLater(inst, () -> {
                    t.reEnable();
                    MultiLineAPI.refresh(Bukkit.getPlayer(u));
                }, 1L);
            }
        }
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        //Update the player's entities locations so they follow the player around
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());
        }
    }

    @EventHandler
    public void gamemode(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SPECTATOR) {
            Bukkit.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    MultiLineAPI.refreshOthers(e.getPlayer());
                }
            }, 1L);
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        Bukkit.getScheduler().runTaskLater(inst, () -> handle(e), 2L);
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        handle(e);
    }

    public void handle(PlayerEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());

            inst.tags.get(e.getPlayer().getUniqueId()).tempDisable();
            inst.tags.get(e.getPlayer().getUniqueId()).reEnable();

            inst.hide(e.getPlayer());

            MultiLineAPI.refresh(e.getPlayer());
        }
        //Refresh other players for that player. This re-sends the mount packets, which break when they change worlds.
        MultiLineAPI.refreshOthers(e.getPlayer());
    }

    //Get whether or not players should automatically be enabled.
    public boolean isAutoEnable() {
        return autoEnable;
    }

    //Set whether or not players should automatically be enabled.
    public void setAutoEnable(boolean autoEnable) {
        this.autoEnable = autoEnable;
    }
}
