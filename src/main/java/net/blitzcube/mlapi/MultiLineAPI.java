package net.blitzcube.mlapi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.blitzcube.mlapi.example.ExampleListener;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public final class MultiLineAPI extends JavaPlugin {
    private static MultiLineAPI inst;
    public final Map<UUID, Tag> tags = Maps.newHashMap();
    private EventListener event;
    private PacketListener packet;

    public static boolean isAutoEnablePlayer() {
        return inst.event.autoEnablePlayer;
    }

    public static void setAutoEnablePlayer(boolean val) {
        inst.event.autoEnablePlayer = val;
    }

    public static boolean isAutoDisablePlayer() {
        return inst.event.autoDisablePlayer;
    }

    public static void setAutoDisablePlayer(boolean val) {
        inst.event.autoDisablePlayer = val;
    }

    public static void enable(Entity e) {
        inst.tags.put(e.getUniqueId(), new Tag());
    }

    public static boolean isEnabled(Entity e) {
        return inst.tags.containsKey(e);
    }

    public static void disable(Entity e) {
        EntityUtil.getEntities(e, 1).filter(en -> en instanceof Player)
                .forEach(p -> inst.packet.despawnStack((Player) p, e.getUniqueId()));
        inst.tags.remove(e.getUniqueId());
    }

    public static void disable() {
        for (UUID u : inst.tags.keySet()) {
            Bukkit.getOnlinePlayers().forEach((Consumer<Player>) player -> inst.packet.despawnStack(player, u));
        }
    }

    public static void show(Entity e) {
        EntityUtil.getEntities(e, 1).filter(en -> en instanceof Player)
                .forEach(p -> inst.packet.spawnStackAndSend((Player) p, e));
        inst.tags.get(e.getUniqueId()).setHiddenForAll(false);
    }

    public static void hide(Entity e) {
        EntityUtil.getEntities(e, 1).filter(en -> en instanceof Player)
                .forEach(p -> inst.packet.despawnStack((Player) p, e.getUniqueId()));
        inst.tags.get(e.getUniqueId()).setHiddenForAll(true);
    }

    public static boolean isHidden(Entity e) {
        return inst.tags.get(e.getUniqueId()).isHiddenForAll();
    }

    public static void addTagControllers(Entity e, TagController... tg) {
        Collections.addAll(inst.tags.get(e.getUniqueId()).tagControllers, tg);
    }

    public static boolean removeTagController(Entity e, TagController tg) {
        return inst.tags.get(e.getUniqueId()).tagControllers.remove(tg);
    }

    public static List<TagController> getTagControllers(Entity e) {
        return inst.tags.get(e.getUniqueId()).tagControllers;
    }

    public static void refresh(Entity e, Player... forWho) {
        for (Player p : forWho) {
            inst.packet.spawnStack(p, e);
        }
    }

    public static void refreshLines(Entity e, Player... forWho) {
        for (Player p : forWho) {
            inst.packet.refreshLines(p, e);
        }
    }

    public static void refreshName(Entity e, Player... forWho) {
        for (Player p : forWho) {
            inst.packet.refreshName(p, e);
        }
    }

    public static void refreshForAll(Entity e) {
        EntityUtil.getEntities(e, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> inst.packet.spawnStackAndSend((Player) en, e));
    }

    public static void refreshLinesForAll(Entity e) {
        EntityUtil.getEntities(e, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> inst.packet.refreshLines((Player) en, e));
    }

    public static void refreshNameForAll(Entity e) {
        EntityUtil.getEntities(e, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> inst.packet.refreshName((Player) en, e));
    }

    public static void showFor(Entity e, Player... forWho) {
        for (Player p : forWho) {
            if (EntityUtil.isInRange(e, p, 1.0)) inst.packet.spawnStackAndSend(p, e);
            inst.tags.get(e.getUniqueId()).setHidden(p.getUniqueId(), false);
        }
    }

    public static void hideFor(Entity e, Player... forWho) {
        for (Player p : forWho) {
            inst.packet.despawnStack(p, e.getUniqueId());
            inst.tags.get(e.getUniqueId()).setHidden(p.getUniqueId(), true);
        }
    }

    public static void resetFor(Entity e, Player... forWho) {
        for (Player p : forWho) {
            if (inst.tags.get(e.getUniqueId()).isHiddenForAll()) {
                inst.packet.despawnStack(p, e.getUniqueId());
            } else {
                if (EntityUtil.isInRange(e, p, 1.0)) inst.packet.spawnStackAndSend(p, e);
            }
            inst.tags.get(e.getUniqueId()).setHidden(p.getUniqueId(), null);
        }
    }

    public static boolean isHiddenFor(Entity e, Player forWho) {
        return inst.tags.get(e.getUniqueId()).isHiddenFor(forWho.getUniqueId());
    }

    public static List<Entity> hideAllFor(Player... forWho) {
        List<Entity> hidden = Lists.newArrayList();
        for (Player p : forWho) {
            EntityUtil.getEntities(p, 1.1).forEach(e -> {
                UUID u = e.getUniqueId();
                if (inst.tags.containsKey(u)) {
                    hidden.add(e);
                    inst.packet.despawnStack(p, u);
                    inst.tags.get(u).setHidden(u, true);
                }
            });
        }
        return hidden;
    }

    public static List<Entity> showAllFor(Player... forWho) {
        List<Entity> shown = Lists.newArrayList();
        for (Player p : forWho) {
            EntityUtil.getEntities(p, 1.0).forEach(e -> {
                UUID u = e.getUniqueId();
                if (inst.tags.containsKey(u)) {
                    shown.add(e);
                    inst.packet.spawnStack(p, e);
                    inst.tags.get(u).setHidden(u, true);
                }
            });
        }
        return shown;
    }

    @Override
    public void onEnable() {
        inst = this;
        EntityUtil.init();
        VisibilityUtil.init(this);
        this.packet = new PacketListener(this);
        this.event = new EventListener(packet);
        this.getServer().getPluginManager().registerEvents(event, this);
        this.getServer().getPluginManager().registerEvents(new ExampleListener(), this);
    }
}
