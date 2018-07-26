package net.blitzcube.mlapi;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.IMultiLineAPI;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.listener.ServerListener;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Created by iso2013 on 5/23/2018.
 */
@SuppressWarnings("unused")
public final class MultiLineAPI extends JavaPlugin implements IMultiLineAPI {
    private TagRenderer renderer;
    private Map<Integer, Tag> tags = new HashMap<>();
    private Multimap<EntityType, ITagController> controllersMap = HashMultimap.create();

    @Override
    public void onEnable() {
        IPacketEntityAPI packetAPI;
        if ((packetAPI = (IPacketEntityAPI) Bukkit.getPluginManager().getPlugin("PacketEntityAPI")) == null) {
            throw new IllegalStateException("Failed to start MultiLineAPI! PacketEntityAPI could not be found!");
        }
        this.renderer = new TagRenderer(packetAPI, this);
        packetAPI.addListener(new PacketListener(tags, renderer, packetAPI));
        this.getServer().getPluginManager().registerEvents(new ServerListener(this, renderer, packetAPI), this);

        this.addDefaultTagController(DemoController.getInst(this));
    }

    @Override
    public void onDisable() {

    }

    @Override
    public ITag getTag(Entity entity) {
        return tags.get(entity.getEntityId());
    }

    @Override
    public ITag createTagIfMissing(Entity entity) {
        return tags.computeIfAbsent(entity.getEntityId(), id ->
                new Tag(entity, renderer, controllersMap.get(entity.getType())));
    }

    @Override
    public void deleteTag(Entity entity) {
        tags.remove(entity.getEntityId());
    }

    @Override
    public boolean hasTag(Entity entity) {
        return tags.containsKey(entity.getEntityId());
    }

    @Override
    public void addDefaultTagController(ITagController val) {
        EntityType[] autoApply = val.getAutoApplyFor();
        autoApply = autoApply != null ? autoApply : EntityType.values();
        for (EntityType t : autoApply) {
            controllersMap.put(t, val);
        }
    }

    @Override
    public void removeDefaultTagController(ITagController val) {
        Collection<ITagController> values = controllersMap.values();
        while (values.contains(val)) values.remove(val);
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
        Tag t;
        Preconditions.checkArgument((t = tags.get(entity.getEntityId())) != null, "This entity "
                + "does not have a tag associated with it!");
        t.update(target);
    }

    @Override
    public void update(Entity entity) {
        Tag t;
        Preconditions.checkArgument((t = tags.get(entity.getEntityId())) != null, "This entity "
                + "does not have a tag associated with it!");
        t.update();
    }

    public boolean hasDefaultTagControllers(EntityType type) {
        return controllersMap.containsKey(type);
    }

    public boolean hasTag(int entityID) {
        return tags.containsKey(entityID);
    }

    public Collection<Tag> getTags() {
        return tags.values();
    }

    public static class DemoController implements ITagController {
        private static DemoController inst;
        private final MultiLineAPI parent;
        public int refreshes = 15;
        private final TagLine line = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                if (refreshes % 2 == 0) return null;
                return "Three";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final TagLine line2 = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                if (refreshes % 3 == 0) return null;
                return "Two";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final TagLine line3 = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                if (refreshes % 4 == 0) return null;
                return "One";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private Set<Entity> enabledFor;

        public DemoController(MultiLineAPI parent) {
            this.parent = parent;
            this.enabledFor = new HashSet<>();
        }

        public static DemoController getInst(MultiLineAPI parent) {
            if (inst == null) inst = new DemoController(parent);
            return inst;
        }

        @Override
        public List<TagLine> getFor(Entity target) {
            enabledFor.add(target);
            return new ArrayList<TagLine>() {{
                add(line);
                add(line2);
                add(line3);
            }};
        }

        public void refreshAll() {
            for (Entity e : enabledFor) {
                if (parent.getTag(e) == null) continue;
                parent.getTag(e).update(this);
            }
        }

        @Override
        public String getName(Entity target, Player viewer) {
            return "- %PREV% -";
        }

        @Override
        public EntityType[] getAutoApplyFor() {
            return EntityType.values();
        }

        @Override
        public JavaPlugin getPlugin() {
            return parent;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getNamePriority() {
            return 0;
        }
    }
}
