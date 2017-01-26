package net.blitzcube.test.mlapitest;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    public void handleChat(String message, Player p) {
        if (MultiLineAPI.getLineCount(this, p) < 1) {
            MultiLineAPI.addLine(this, p);
        }
        TagLine line = MultiLineAPI.getLine(this, p, 0);
        line.setText(ChatColor.YELLOW + ChatColor.translateAlternateColorCodes('&', message));
        if (p.hasMetadata("PLAYER_CHAT")) {
            int i = (int) p.getMetadata("PLAYER_CHAT").get(0).value();
            p.removeMetadata("PLAYER_CHAT", this);
            Bukkit.getScheduler().cancelTask(i);
        }

        MLAPIExample ex = this;
        p.setMetadata("PLAYER_CHAT", new FixedMetadataValue(this, Bukkit.getScheduler().runTaskLater(this, () -> {
            line.setText(null);
            MultiLineAPI.refresh(p);
            p.removeMetadata("PLAYER_CHAT", ex);
        }, 200L).getTaskId()));
        MultiLineAPI.refresh(p);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public int getPriority() {
        return 0;
    }
}
