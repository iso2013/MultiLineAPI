package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

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

        if (Bukkit.getPlayer("iso2013") != null) {
            Bukkit.getPlayer("iso2013").hidePlayer(e.getPlayer());
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
    public void move(PlayerMoveEvent e) {
        //Update the player's entities locations so they follow the player around
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld().getUID().equals(e.getTo().getWorld().getUID())) {
            if (MultiLineAPI.isEnabled(e.getPlayer())) {
                //Update the player's entities locations so they follow the player around
                MultiLineAPI.updateLocs(e.getPlayer());
                //Hide all the entities from the player who owns them. This needs to be done on teleport or world
                // change,

                // in my testing. Teleporting the entities across worlds causes Spigot to resend them to the client,
                // so they need to be removed again.
                inst.hide(e.getPlayer());

                MultiLineAPI.refresh(e.getPlayer());
            }
            //Refresh other players for that player. This re-sends the mount packets, which break when they teleport.
            MultiLineAPI.refreshOthers(e.getPlayer());
        }
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            inst.tags.get(e.getPlayer().getUniqueId()).refresh();

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
