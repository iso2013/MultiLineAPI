package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Created by iso2013 on 6/7/2018.
 */
public class ServerListener implements Listener {
    private final MultiLineAPI parent;
    private final VisibilityStates states;
    private final IPacketEntityAPI packet;

    public ServerListener(MultiLineAPI parent, VisibilityStates states, IPacketEntityAPI packet) {
        this.parent = parent;
        this.states = states;
        this.packet = packet;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        onSpawn(e.getPlayer());
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) { onSpawn(e.getEntity()); }

    @EventHandler
    public void onSpawn(ItemSpawnEvent e) { onSpawn(e.getEntity()); }

    @EventHandler
    public void onSpawn(SpawnerSpawnEvent e) { onSpawn(e.getEntity()); }

    @EventHandler
    public void onSpawn(ProjectileLaunchEvent e) { onSpawn(e.getEntity()); }

    @EventHandler
    public void onSpawn(VehicleCreateEvent e) {
        onSpawn(e.getVehicle());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (Entity en : e.getChunk().getEntities()) {
            if (!en.isValid()) continue;
            onSpawn(en);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.purge(e.getPlayer());
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent e) {
        for (Entity en : e.getChunk().getEntities())
            onDespawn(en);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SPECTATOR && e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            //Player is changing into SPECTATOR, so we should despawn all tags.
            TagRenderer.batchDestroyTags(packet, states.getVisible(e.getPlayer()), e.getPlayer());
        } else if (e.getNewGameMode() != GameMode.SPECTATOR && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            //Player is changing out of SPECTATOR, so we should spawn all tags.
            packet.getVisible(e.getPlayer(), 1, false)
                    .map(identifier -> {
                        Entity e1 = identifier.getEntity().get();
                        return e1 != null ? parent.getTag(e1) : null;
                    }).filter(tag -> {
                Boolean v = states.isVisible(tag, e.getPlayer());
                if (tag == null) return false;
                if (v == null) v = tag.getDefaultVisible();
                return v;
            }).forEach(tag -> tag.getRenderer().spawnTag(tag, e.getPlayer(), null));
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) {
            MultiLineAPI.DemoController c = MultiLineAPI.DemoController.getInst(null);
            c.refreshes--;
            c.refreshAll();
        }
    }

    @EventHandler
    public void onNameChange(PlayerInteractAtEntityEvent e) {
        if (parent.hasTag(e.getRightClicked())) {
            Bukkit.getScheduler().runTaskLater(parent, () -> parent.getTag(e.getRightClicked()).updateName(), 1L);
        }
    }

    private void onSpawn(Entity e) {
        if (parent.hasDefaultTagControllers(e.getType()))
            parent.createTagIfMissing(e);
    }

    private void onDespawn(Entity e) {
        Bukkit.getScheduler().runTaskLater(parent, () -> parent.deleteTag(e), 1);
    }
}
