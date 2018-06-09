package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Created by iso2013 on 6/7/2018.
 */
public class ServerListener implements Listener {
    private final MultiLineAPI parent;
    private final TagRenderer renderer;
    private final IPacketEntityAPI packet;

    public ServerListener(MultiLineAPI parent, TagRenderer renderer, IPacketEntityAPI packet) {
        this.parent = parent;
        this.renderer = renderer;
        this.packet = packet;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        renderer.purge(e.getPlayer());
        onDespawn(e.getPlayer());
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
    public void onChunkLoad(ChunkLoadEvent e) {
        for (Entity en : e.getChunk().getEntities()) {
            onSpawn(en);
        }
    }

    private void onSpawn(Entity e) {
        if (parent.hasDefaultTagControllers(e.getType()))
            parent.createTagIfMissing(e);
    }

    @EventHandler
    public void onDespawn(EntityDeathEvent e) {
        if (e instanceof PlayerDeathEvent) return;
        Bukkit.getScheduler().runTaskLater(parent, () -> onDespawn(e.getEntity()), 60);
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent e) {
        Bukkit.getScheduler().runTaskLater(parent, () -> {
            for (Entity en : e.getChunk().getEntities()) {
                onDespawn(en);
            }
        }, 60);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SPECTATOR && e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            //Player is changing into SPECTATOR, so we should despawn all tags.
            renderer.batchDestroyTags(packet.getVisible(e.getPlayer(), 1.03, false)
                    .map(identifier -> {
                        Entity e1 = identifier.getEntity().get();
                        return e1 != null ? (Tag) parent.getTag(e1) : null;
                    }).filter(tag -> {
                        Boolean v = renderer.isVisible(tag, e.getPlayer());
                        if (tag == null) return false;
                        if (v == null) v = tag.getDefaultVisible();
                        return v;
                    }), e.getPlayer());
        } else if (e.getNewGameMode() != GameMode.SPECTATOR && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            //Player is changing out of SPECTATOR, so we should spawn all tags.
            packet.getVisible(e.getPlayer(), 1, false)
                    .map(identifier -> {
                        Entity e1 = identifier.getEntity().get();
                        return e1 != null ? parent.getTag(e1) : null;
                    }).filter(tag -> {
                Boolean v = renderer.isVisible(tag, e.getPlayer());
                if (tag == null) return false;
                if (v == null) v = tag.getDefaultVisible();
                return v;
            }).forEach(tag -> renderer.spawnTag((Tag) tag, e.getPlayer(), null));
        }
    }

    private void onDespawn(Entity e) {
        parent.deleteTag(e);
    }
}
