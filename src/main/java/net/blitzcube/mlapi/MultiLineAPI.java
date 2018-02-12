package net.blitzcube.mlapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.blitzcube.mlapi.api.IMultiLineAPI;
import net.blitzcube.mlapi.api.MLAPI;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.api.tag.ITagLine;
import net.blitzcube.mlapi.example.ExampleListener;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public final class MultiLineAPI extends JavaPlugin implements IMultiLineAPI {

    private final Map<UUID, Tag> tags = new HashMap<>();
    private final Set<Integer> enabled = new HashSet<>();

    private EventListener event;
    private PacketListener packet;
    private boolean autoEnable, autoDisable;

    @Override
    public boolean shouldAutoEnable() {
        return autoEnable;
    }

    @Override
    public void setAutoEnable(boolean enable) {
        this.autoEnable = enable;
    }

    @Override
    public boolean shouldAutoDisable() {
        return autoDisable;
    }

    @Override
    public void setAutoDisable(boolean disable) {
        this.autoDisable = disable;
    }

    @Override
    public void enable(Entity entity) {
        this.tags.put(entity.getUniqueId(), new Tag());
    }

    @Override
    public boolean isEnabled(Entity entity) {
        return tags.containsKey(entity.getUniqueId());
    }

    // TODO make this an API method too?
    public boolean isEnabled(UUID entity) {
        return tags.containsKey(entity);
    }

    @Override
    public void disable(Entity entity) {
        EntityUtil.getEntities(entity, 1).filter(en -> en instanceof Player)
                .forEach(p -> packet.despawnStack((Player) p, entity));
        this.tags.remove(entity.getUniqueId());
    }

    @Override
    public void disable() {
        for (UUID u : tags.keySet()) {
            Bukkit.getOnlinePlayers().forEach(player -> packet.despawnStack(player, u));
        }
    }

    @Override
    public void show(Entity entity) {
        EntityUtil.getEntities(entity, 1).filter(en -> en instanceof Player)
                .forEach(p -> packet.spawnStackAndSend((Player) p, entity));
        this.tags.get(entity.getUniqueId()).setHiddenForAll(false);
    }

    @Override
    public void hide(Entity entity) {
        EntityUtil.getEntities(entity, 1).filter(en -> en instanceof Player)
                .forEach(p -> packet.despawnStack((Player) p, entity));
        this.tags.get(entity.getUniqueId()).setHiddenForAll(true);
    }

    @Override
    public void show(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            if (EntityUtil.isInRange(entity, p, 1.0)) {
                this.packet.spawnStackAndSend(p, entity);
            }

            this.tags.get(entity.getUniqueId()).setHidden(p.getUniqueId(), false);
        }
    }

    @Override
    public void hide(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            this.packet.despawnStack(p, entity);
            this.tags.get(entity.getUniqueId()).setHidden(p.getUniqueId(), true);
        }
    }

    @Override
    public void reset(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            if (tags.get(entity.getUniqueId()).isHiddenForAll()) {
                this.packet.despawnStack(p, entity);
            } else if (EntityUtil.isInRange(entity, p, 1.0)) {
                this.packet.spawnStackAndSend(p, entity);
            }

            this.tags.get(entity.getUniqueId()).setHidden(p.getUniqueId(), false);
        }
    }

    @Override
    public boolean isHidden(Entity entity) {
        return tags.get(entity.getUniqueId()).isHiddenForAll();
    }

    @Override
    public boolean isHiddenFor(Entity entity, Player forWho) {
        return this.tags.get(entity.getUniqueId()).isHidden(forWho.getUniqueId());
    }

    @Override
    public void addTagControllers(Entity entity, ITagController... controllers) {
        Collections.addAll(tags.get(entity.getUniqueId()).getControllers(), controllers);
    }

    @Override
    public boolean removeTagController(Entity entity, ITagController controller) {
        return tags.get(entity.getUniqueId()).getControllers().remove(controller);
    }

    @Override
    public List<ITagController> getTagControllers(Entity entity) {
        return tags.get(entity.getUniqueId()).getControllers();
    }

    @Override
    public ITagLine createTagLine(String text, boolean keepSpaceWhenNull, Function<Player, String> dynamicText) {
        return new TagLine(text, keepSpaceWhenNull, dynamicText);
    }

    @Override
    public ITagLine createTagLine(String text, boolean keepSpaceWhenNull) {
        return new TagLine(text, keepSpaceWhenNull);
    }

    @Override
    public ITagLine createTagLine(String text, Function<Player, String> dynamicText) {
        return new TagLine(text, dynamicText);
    }

    @Override
    public ITagLine createTagLine(String text) {
        return new TagLine(text);
    }

    @Override
    public ITag getTag(Entity entity) {
        return tags.get(entity.getUniqueId());
    }

    // TODO make this an API method too?
    public ITag getTag(UUID entity) {
        return tags.get(entity);
    }

    @Override
    public void refresh(Entity entity) {
        EntityUtil.getEntities(entity, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> packet.spawnStackAndSend((Player) en, entity));
    }

    @Override
    public void refreshLines(Entity entity) {
        EntityUtil.getEntities(entity, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> packet.refreshLines((Player) en, entity));
    }

    @Override
    public void refreshName(Entity entity) {
        EntityUtil.getEntities(entity, 1.0).filter(p -> p instanceof Player)
                .forEach(en -> packet.refreshName((Player) en, entity));
    }

    @Override
    public void refresh(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            this.packet.spawnStack(p, entity);
        }
    }

    @Override
    public void refreshLines(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            this.packet.refreshLines(p, entity);
        }
    }

    @Override
    public void refreshName(Entity entity, Player... forWho) {
        for (Player p : forWho) {
            this.packet.refreshName(p, entity);
        }
    }

    @Override
    public List<Entity> hideAllFor(Player... forWho) {
        List<Entity> hidden = Lists.newArrayList();

        for (Player p : forWho) {
            EntityUtil.getEntities(p, 1.1)
                .filter(e -> tags.containsKey(e.getUniqueId()))
                .forEach(e -> {
                    UUID u = e.getUniqueId();
                    hidden.add(e);

                    this.packet.despawnStack(p, u);
                    this.tags.get(u).setHidden(u, true);
                });
        }

        return hidden;
    }

    @Override
    public List<Entity> showAllFor(Player... forWho) {
        List<Entity> shown = Lists.newArrayList();

        for (Player p : forWho) {
            EntityUtil.getEntities(p, 1.0)
                .filter(e -> tags.containsKey(e.getUniqueId()))
                .forEach(e -> {
                    UUID u = e.getUniqueId();
                    shown.add(e);

                    this.packet.spawnStack(p, e);
                    this.tags.get(u).setHidden(u, true);
                });
        }

        return shown;
    }

    public boolean isTagEnabled(int id) {
        return enabled.contains(id);
    }

    @Override
    public void onEnable() {
        EntityUtil.init();
        VisibilityUtil.init(this);

        this.packet = new PacketListener(this);
        this.event = new EventListener(this, packet);
        Bukkit.getPluginManager().registerEvents(event, this);
        Bukkit.getPluginManager().registerEvents(new ExampleListener(), this);

        MLAPI.setImplementation(this);
    }

}
