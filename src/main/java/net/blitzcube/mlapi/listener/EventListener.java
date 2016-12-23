package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.Bukkit;
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
    public boolean autoEnable;
    private MultiLineAPI inst;

    public EventListener(MultiLineAPI parent) {
        this.inst = parent;
        autoEnable = false;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (autoEnable) {
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                MultiLineAPI.enable(e.getPlayer());
                MultiLineAPI.refresh(e.getPlayer());
                Bukkit.getScheduler().runTaskLater(inst, () -> {
                    inst.hide(e.getPlayer());
                }, 1L);
            }, 1L);
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.disable(e.getPlayer());
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (e.getEntity().hasMetadata("STACK_ENTITY")) e.setCancelled(true);
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());
            inst.hide(e.getPlayer());
        }
        MultiLineAPI.refreshOthers(e.getPlayer());
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (MultiLineAPI.isEnabled(e.getPlayer())) {
            MultiLineAPI.updateLocs(e.getPlayer());
            inst.hide(e.getPlayer());
        }
        MultiLineAPI.refreshOthers(e.getPlayer());
    }

    @EventHandler
    public void sneak(PlayerToggleSneakEvent e) {
        if (MultiLineAPI.getLineCount(e.getPlayer()) < 1) {
            MultiLineAPI.addLine(e.getPlayer());
        }
        TagLine line = MultiLineAPI.getLine(e.getPlayer(), 0);
        line.setKeepSpaceWhenNull(false);
        if (e.isSneaking()) {
            line.setText("Sneaking");
        } else {
            line.setText(null);
        }
        MultiLineAPI.refresh(e.getPlayer());
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String message = e.getMessage();
        Bukkit.getScheduler().runTask(inst, new Runnable() {
            @Override
            public void run() {
                handleChat(p, message);
            }
        });
    }

    private void handleChat(Player p, String message) {
        if (p.hasMetadata("CHAT_SCHEDULER")) {
            ((BukkitTask) p.getMetadata("CHAT_SCHEDULER").get(0).value()).cancel();
            p.removeMetadata("CHAT_SCHEDULER", inst);
        }
        while (MultiLineAPI.getLineCount(p) < 2) {
            MultiLineAPI.addLine(p);
        }
        TagLine line = MultiLineAPI.getLine(p, 1);
        line.setKeepSpaceWhenNull(false);
        line.setText(message);
        p.setMetadata("CHAT_SCHEDULER", new FixedMetadataValue(inst, Bukkit.getScheduler().runTaskLater(inst, () -> {
            line.setText(null);
            MultiLineAPI.refresh(p);
        }, 10 * 20)));
        MultiLineAPI.refresh(p);
    }
}
