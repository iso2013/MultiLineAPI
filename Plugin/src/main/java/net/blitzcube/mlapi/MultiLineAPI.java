package net.blitzcube.mlapi;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.IMultiLineAPI;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.demo.DemoController;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.listener.ServerListener;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.iso2013.peapi.api.PacketEntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Created by iso2013 on 5/23/2018.
 */
public final class MultiLineAPI extends JavaPlugin implements IMultiLineAPI {

    private final VisibilityStates states = new VisibilityStates();
    private final Map<Integer, Tag> tags = new HashMap<>();
    private final Multimap<EntityType, ITagController> controllersMap = HashMultimap.create();

    private LineEntityFactory lineFactory;

    @Override
    public void onEnable() {
        PacketEntityAPI packetAPI = (PacketEntityAPI) Bukkit.getPluginManager().getPlugin("PacketEntityAPI");
        if (packetAPI == null) {
            throw new IllegalStateException("Failed to start MultiLineAPI! PacketEntityAPI could not be found!");
        }

        this.lineFactory = new LineEntityFactory(packetAPI.getModifierRegistry(), packetAPI.getEntityFactory());

        packetAPI.addListener(new PacketListener(this, tags, states, packetAPI));
        ServerListener listener = new ServerListener(this, states, packetAPI);
        Bukkit.getPluginManager().registerEvents(listener, this);
        Bukkit.getScheduler().runTask(this, listener::loadAllWorldEntities);

        this.saveDefaultConfig();

        if (this.getConfig().getBoolean("demoMode", false)) {
            this.addDefaultTagController(new DemoController(this));
        }

        TagRenderer.init(packetAPI, lineFactory, states, this, this.getConfig());
    }

    @Override
    public Tag getTag(Entity entity) {
        if (entity == null) return null;
        return tags.get(entity.getEntityId());
    }

    @Override
    public Tag getOrCreateTag(Entity entity, boolean notifyPlayers) {
        int id = entity.getEntityId();

        if (!tags.containsKey(id)) {
            TagRenderer renderer = TagRenderer.createInstance(entity.getType());
            Tag tag = new Tag(entity, renderer, controllersMap.get(entity.getType()), lineFactory, states);
            tags.put(id, tag);

            if (notifyPlayers)
                renderer.getNearby(tag, 1.0).filter(input -> input != entity).forEach(player -> renderer.spawnTag(tag
                        , player, null));
        }

        return tags.get(id);
    }

    @Override
    public void deleteTag(Entity entity) {
        Tag tag = tags.remove(entity.getEntityId());
        if (tag == null) return;

        TagRenderer renderer = tag.getRenderer();
        renderer.getNearby(tag, 1.1).forEach(player -> renderer.destroyTag(tag, player, null));
        renderer.purge(tag);
    }

    @Override
    public boolean hasTag(Entity entity) {
        return tags.containsKey(entity.getEntityId());
    }

    @Override
    public void addDefaultTagController(ITagController controller) {
        EntityType[] autoApply = controller.getAutoApplyFor();
        autoApply = (autoApply != null) ? autoApply : EntityType.values();

        for (EntityType type : autoApply) {
            this.controllersMap.put(type, controller);
        }
    }

    @Override
    public void removeDefaultTagController(ITagController controller) {
        Collection<ITagController> values = controllersMap.values();
        while (values.contains(controller)) {
            values.remove(controller);
        }
    }

    @Override
    public Set<ITagController> getDefaultTagControllers() {
        return new HashSet<>(controllersMap.values());
    }

    @Override
    public Collection<ITagController> getDefaultTagControllers(EntityType type) {
        return controllersMap.get(type);
    }

    @Override
    public void update(Entity entity, Player target) {
        Tag tag = tags.get(entity.getEntityId());
        Preconditions.checkState(tag != null, "This entity does not have a tag associated with it!");

        tag.update(target);
    }

    @Override
    public void update(Entity entity) {
        Tag tag = tags.get(entity.getEntityId());
        Preconditions.checkState(tag != null, "This entity does not have a tag associated with it!");

        tag.update();
    }


    @Override
    public void update(ITagController controller, Player target) {
        this.states.getVisible(target).filter(input -> input.getTagControllers(false).contains(controller))
                .forEach(tag -> tag.update(controller));
    }

    @Override
    public void update(ITagController controller) {
        Bukkit.getOnlinePlayers().forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        this.states.getVisible(target).forEach(tag -> tag.update(line));
    }

    @Override
    public void update(ITagController.TagLine line) {
        Bukkit.getOnlinePlayers().forEach(player -> update(line, player));
    }

    @Override
    public void updateNames(Player target) {
        this.states.getVisible(target).forEach(tag -> tag.updateName(target));
    }

    @Override
    public void updateNames() {
        this.tags.values().forEach(Tag::updateName);
    }

    public boolean hasDefaultTagControllers(EntityType type) {
        return controllersMap.containsKey(type);
    }
}
