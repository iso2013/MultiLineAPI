package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class EventListener implements Listener {
    private final MultiLineAPI inst;
    private boolean autoEnablePlayers;
    private boolean autoEnableEntities;

    public EventListener(MultiLineAPI parent) {
        this.inst = parent;
        autoEnablePlayers = true;
        autoEnableEntities = false;
    }

    @EventHandler
    public void create(PlayerJoinEvent e) {
        if (autoEnablePlayers) {
            Bukkit.getScheduler().runTaskLater(inst, () -> MultiLineAPI.enable(e.getPlayer()), 1L);
        }
        Bukkit.getScheduler().runTaskLater(inst, () -> MultiLineAPI.refreshOthers(e.getPlayer()), 2L);
    }

    @EventHandler
    public void create(EntitySpawnEvent e) {
        if (autoEnableEntities) {
            Bukkit.getScheduler().runTaskLater(inst, () -> MultiLineAPI.enable(e.getEntity()), 1L);
        }
    }

    @EventHandler
    public void death(EntityDeathEvent e) {
        if (inst.tags.containsKey(e.getEntity().getUniqueId())) {
            if (e instanceof PlayerDeathEvent) {
                Bukkit.getScheduler().runTaskLater(inst, () -> inst.tags.get(e.getEntity().getUniqueId()).despawn(),
                        20L);
            } else {
                Bukkit.getScheduler().runTaskLater(inst, () -> MultiLineAPI.disable(e.getEntity()), 20L);
            }
        } else if (e.getEntity().hasMetadata("STACK_ENTITY")) {
            e.setDroppedExp(0);
            UUID u = (UUID) e.getEntity().getMetadata("STACK_ENTITY").get(0).value();
            Tag t = inst.tags.get(u);
            if (t != null) {
                Bukkit.getScheduler().runTaskLater(inst, () -> t.getEvent().partKilled(inst.pckt), 2L);
            }
        }
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        if (inst.tags.containsKey(e.getPlayer().getUniqueId())) {
            inst.tags.get(e.getPlayer().getUniqueId()).getEvent().respawn(inst.pckt);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spawn(EntitySpawnEvent e) {
        if (e.isCancelled()) {
            if (e.getEntity().hasMetadata("STACK_ENTITY")) {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (inst.tags.containsKey(e.getPlayer().getUniqueId())) {
            MultiLineAPI.disable(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void damage(EntityDamageEvent e) {
        if (e.getEntity().hasMetadata("STACK_ENTITY")) {
            UUID u = (UUID) e.getEntity().getMetadata("STACK_ENTITY").get(0).value();
            Tag t = inst.tags.get(u);
            if (t != null) {
                Bukkit.getPluginManager().callEvent(new EntityDamageEvent(t.getOwner(), e.getCause(), e.));
                //TODO: Fix this
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void gameMode(PlayerGameModeChangeEvent e) {
        e.getPlayer().sendMessage(e.getPlayer().getGameMode().name());
        if (e.getNewGameMode() == GameMode.SPECTATOR || e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            Bukkit.getScheduler().runTaskLater(inst, () -> MultiLineAPI.refreshOthers(e.getPlayer()), 2L);
        }
    }

    @EventHandler
    public void teleport(EntityTeleportEvent e) {
        if (inst.tags.containsKey(e.getEntity().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(inst, () -> inst.tags.get(e.getEntity().getUniqueId()).getEvent()
                    .teleportOrWorldChange(inst.pckt), 2L);
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (inst.tags.containsKey(e.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                inst.tags.get(e.getPlayer().getUniqueId()).getEvent().teleportOrWorldChange(inst.pckt);
                MultiLineAPI.refreshOthers(e.getPlayer());
            }, 2L);
        }
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (inst.tags.containsKey(e.getPlayer().getUniqueId())) {
            inst.tags.get(e.getPlayer().getUniqueId()).getEvent().teleportOrWorldChange(inst.pckt);
            MultiLineAPI.refreshOthers(e.getPlayer());
        }
    }

    @EventHandler
    public void nameTag(PlayerInteractAtEntityEvent e) {
        if (e.isCancelled()) return;
        if (e.getRightClicked() == null) return;
        if (e.getRightClicked().hasMetadata("STACK_ENTITY")) {
            e.setCancelled(true);
            return;
        }
        ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
        if (stack.getType().equals(Material.NAME_TAG)) {
            if (inst.tags.containsKey(e.getRightClicked().getUniqueId())) {
                if (stack.hasItemMeta()) {
                    ItemMeta meta = stack.getItemMeta();
                    inst.tags.get(e.getRightClicked().getUniqueId()).getName().setText(meta.getDisplayName());
                    stack.setAmount(1);
                    e.getPlayer().getInventory().remove(stack);
                }
            }
        }
    }

    public boolean isAutoEnablePlayers() {
        return autoEnablePlayers;
    }

    public void setAutoEnablePlayers(boolean autoEnablePlayers) {
        this.autoEnablePlayers = autoEnablePlayers;
    }

    public boolean isAutoEnableEntities() {
        return autoEnableEntities;
    }

    public void setAutoEnableEntities(boolean autoEnableEntities) {
        this.autoEnableEntities = autoEnableEntities;
    }
}
