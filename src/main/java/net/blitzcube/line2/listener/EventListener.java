package net.blitzcube.line2.listener;

import net.blitzcube.line2.SecondLineAPI;
import net.blitzcube.line2.tag.TagLine;
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
    private SecondLineAPI inst;

    public EventListener(SecondLineAPI parent) {
        this.inst = parent;
        autoEnable = false;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (autoEnable) {
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                SecondLineAPI.enable(e.getPlayer());
                SecondLineAPI.refresh(e.getPlayer());
                Bukkit.getScheduler().runTaskLater(inst, () -> {
                    inst.hide(e.getPlayer());
                }, 1L);
            }, 1L);
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (SecondLineAPI.isEnabled(e.getPlayer())) {
            SecondLineAPI.disable(e.getPlayer());
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (e.getEntity().hasMetadata("STACK_ENTITY")) e.setCancelled(true);
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (SecondLineAPI.isEnabled(e.getPlayer())) {
            SecondLineAPI.updateLocs(e.getPlayer());
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (SecondLineAPI.isEnabled(e.getPlayer())) {
            SecondLineAPI.updateLocs(e.getPlayer());
            inst.hide(e.getPlayer());
        }
        SecondLineAPI.refreshOthers(e.getPlayer());
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (SecondLineAPI.isEnabled(e.getPlayer())) {
            SecondLineAPI.updateLocs(e.getPlayer());
            inst.hide(e.getPlayer());
        }
        SecondLineAPI.refreshOthers(e.getPlayer());
    }

    @EventHandler
    public void sneak(PlayerToggleSneakEvent e) {
        if (SecondLineAPI.getLineCount(e.getPlayer()) < 1) {
            SecondLineAPI.addLine(e.getPlayer());
        }
        TagLine line = SecondLineAPI.getLine(e.getPlayer(), 0);
        line.setKeepSpaceWhenNull(false);
        if (e.isSneaking()) {
            line.setText("Sneaking");
        } else {
            line.setText(null);
        }
        SecondLineAPI.refresh(e.getPlayer());
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
        while (SecondLineAPI.getLineCount(p) < 2) {
            SecondLineAPI.addLine(p);
        }
        TagLine line = SecondLineAPI.getLine(p, 1);
        line.setKeepSpaceWhenNull(false);
        line.setText(message);
        p.setMetadata("CHAT_SCHEDULER", new FixedMetadataValue(inst, Bukkit.getScheduler().runTaskLater(inst, () -> {
            line.setText(null);
            SecondLineAPI.refresh(p);
        }, 10 * 20)));
        SecondLineAPI.refresh(p);
    }
}
