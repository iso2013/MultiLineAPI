package net.blitzcube.test.mlapitest;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class MLAPIExample extends JavaPlugin implements TagController, Listener {
    @Override
    public void onEnable() {
        MultiLineAPI.register(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Bukkit.getScheduler().runTask(this, () -> handleChat(e.getMessage(), e.getPlayer()));
    }

    @EventHandler
    public void damage(EntityRegainHealthEvent e) {
        updateLine((LivingEntity) e.getEntity());
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        updateLine((LivingEntity) e.getEntity());
    }

    private void updateLine(LivingEntity e) {
        if (e instanceof Player) return;
        if (!MultiLineAPI.isEnabled(e)) MultiLineAPI.enable(e);
        if (MultiLineAPI.getLineCount(this, e) < 1) MultiLineAPI.addLine(this, e);
        TagLine line = MultiLineAPI.getLine(this, e, 0);
        double percent = e.getHealth() / e.getMaxHealth();
        line.setText(getColor(percent) + "" + Math.round(e.getHealth()) + "/" + Math.round(e.getMaxHealth()));
        MultiLineAPI.refresh(e);
    }

    private ChatColor getColor(double percent) {
        if (percent > 0.75) {
            return ChatColor.DARK_GREEN;
        } else if (percent > 0.75) {
            return ChatColor.GREEN;
        } else if (percent > 0.25) {
            return ChatColor.YELLOW;
        } else if (percent > 0.05) {
            return ChatColor.GOLD;
        } else if (percent > 0) {
            return ChatColor.RED;
        }
        return ChatColor.BLACK;
    }

    private void handleChat(String message, Player p) {
        if (MultiLineAPI.getLineCount(this, p) < 1) {
            MultiLineAPI.addLine(this, p);
        }
        TagLine line = MultiLineAPI.getLine(this, p, 0);
        line.setText(ChatColor.translateAlternateColorCodes('&', message));
        if (p.hasMetadata("PLAYER_CHAT")) {
            int i = (int) p.getMetadata("PLAYER_CHAT").get(0).value();
            p.removeMetadata("PLAYER_CHAT", this);
            Bukkit.getScheduler().cancelTask(i);
        }
        MultiLineAPI.refresh(p);

        MLAPIExample ex = this;
        p.setMetadata("PLAYER_CHAT", new FixedMetadataValue(this, Bukkit.getScheduler().runTaskLater(this, () -> {
            line.setText(null);
            MultiLineAPI.refresh(p);
            p.removeMetadata("PLAYER_CHAT", ex);
        }, 200L)));
    }

    @Override
    public void onDisable() {

    }

    @Override
    public int getPriority() {
        return 0;
    }
}
