package net.blitzcube.score.secondlineapi;

import net.blitzcube.score.secondlineapi.manager.SecondLineManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SecondLineAPI extends JavaPlugin implements Listener {
    private SecondLineManager manager;

    @Override
    public void onEnable() {
        manager = SecondLineManager.getInstance(this);
        manager.setDefaultMessage(null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "name":
                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (args.length > 2) {
                            String name = "";
                            for (int i = 2; i < args.length; i++) {
                                name = name + args[i];
                                if (i + 1 < args.length) {
                                    name = name + " ";
                                }
                            }
                            manager.setName(p, name);
                        }
                    }
                    return true;
                case "line":
                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (args.length > 2) {
                            String name = "";
                            for (int i = 2; i < args.length; i++) {
                                name = name + args[i];
                                if (i + 1 < args.length) {
                                    name = name + " ";
                                }
                            }
                            if (Objects.equals(name, "null")) {
                                manager.setLine(p, null);
                            }
                            manager.setLine(p, name);
                        }
                    }
                    return true;
                case "disable":
                    if (args.length > 1) {
                        manager.clear(Bukkit.getPlayer(args[1]));
                    }
                    return true;
                case "enable":
                    if (args.length > 1) {
                        manager.add(Bukkit.getPlayer(args[1]));

                    }
                    return true;
                case "testmount":
                    Player p = Bukkit.getPlayer(args[1]);
                    p.setPassenger(p.getWorld().spawnEntity(p.getLocation(), EntityType.PIG));
                    return true;
                case "testunmount":
                    Player p2 = Bukkit.getPlayer(args[1]);
                    p2.sendMessage(p2.eject() + "");
                    p2.sendMessage("Passenger removed?");
                    return true;
            }
        }
        sender.sendMessage("Proper usage:");
        sender.sendMessage("/secondline name <player> <name>");
        sender.sendMessage("/secondline line <player> <line>");
        sender.sendMessage("/secondline disable <player>");
        sender.sendMessage("/secondline enable <player>");
        sender.sendMessage("/secondline testmount <player>");
        sender.sendMessage("/secondline testunmount <player>");
        return true;
    }

    @Override
    public void onDisable() {
        manager.dispose();
    }
}
