package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class EventListener implements Listener {
    //Whether or not new players should be automatically enabled
    public boolean autoEnable;
    //The instance of the API for referencing hide methods
    private MultiLineAPI inst;

    //Constructor just accepts the API to store in the inst variable
    public EventListener(MultiLineAPI parent) {
        this.inst = parent;
        //By default, auto enabling is off.
        autoEnable = false;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        //If auto joining is enabled, then schedule a task
        if (autoEnable) {
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
                }, 1L);
            }, 1L);
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

    @EventHandler
    public void sneak(PlayerToggleSneakEvent e) {
        //Just some code for testing on sneak. Nothing to see here.
        if (MultiLineAPI.getLineCount(e.getPlayer()) < 1) {
            MultiLineAPI.addLine(e.getPlayer());
        }
        TagLine line = MultiLineAPI.getLine(e.getPlayer(), 0);
        line.setKeepSpaceWhenNull(false);
        if (e.isSneaking()) {
            line.setText("Sneaking ☺");
        } else {
            line.setText(null);
        }
        MultiLineAPI.refresh(e.getPlayer());
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String message = e.getMessage();
        //Run code synchronously.
        Bukkit.getScheduler().runTask(inst, () -> handleChat(p, message));
    }

    private void handleChat(Player p, String message) {
        //Chat messages below name.
        if (p.hasMetadata("CHAT_SCHEDULER")) {
            ((BukkitTask) p.getMetadata("CHAT_SCHEDULER").get(0).value()).cancel();
            p.removeMetadata("CHAT_SCHEDULER", inst);
        }
        while (MultiLineAPI.getLineCount(p) < 2) {
            MultiLineAPI.addLine(p);
        }
        TagLine line = MultiLineAPI.getLine(p, 1);
        line.setKeepSpaceWhenNull(false);
        line.setText(ChatColor.YELLOW + message);
        p.setMetadata("CHAT_SCHEDULER", new FixedMetadataValue(inst, Bukkit.getScheduler().runTaskLater(inst, () -> {
            line.setText(null);
            MultiLineAPI.refresh(p);
        }, 10 * 20)));
        MultiLineAPI.refresh(p);
    }
}
