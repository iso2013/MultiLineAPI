package net.blitzcube.bridge.nametageditbridge;

import com.nametagedit.plugin.api.NametagAPI;
import net.blitzcube.mlapi.MultiLineAPI;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class NametagEditBridge extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getServer().getPluginManager().registerEvents(this, this);
        MultiLineAPI.register(new NametagAPI());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
