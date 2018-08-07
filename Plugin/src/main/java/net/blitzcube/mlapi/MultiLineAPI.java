package net.blitzcube.mlapi;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.IMultiLineAPI;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.listener.ServerListener;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final VisibilityStates states = new VisibilityStates();
    private final Map<Integer, Tag> tags = new HashMap<>();
    private final Multimap<EntityType, ITagController> controllersMap = HashMultimap.create();

    private LineEntityFactory lineFactory;

    @Override
    public void onEnable() {
        IPacketEntityAPI packetAPI;
        if ((packetAPI = (IPacketEntityAPI) Bukkit.getPluginManager().getPlugin("PacketEntityAPI")) == null) {
            throw new IllegalStateException("Failed to start MultiLineAPI! PacketEntityAPI could not be found!");
        }

        this.lineFactory = new LineEntityFactory(packetAPI.getModifierRegistry(),
                packetAPI.getEntityFactory());

        packetAPI.addListener(new PacketListener(this, tags, states, packetAPI));
        this.getServer().getPluginManager().registerEvents(new ServerListener(this, states, packetAPI), this);

        this.addDefaultTagController(DemoController.getInst(this));
        this.addDefaultTagController(DemoController2.getInst(this));

        this.saveDefaultConfig();

        TagRenderer.init(packetAPI, lineFactory, states, this, this.getConfig());
    }

    @Override
    public ITag getTag(Entity entity) {
        return tags.get(entity.getEntityId());
    }

    @Override
    public ITag createTagIfMissing(Entity entity) {
        int id = entity.getEntityId();
        if (!tags.containsKey(id)) {
            TagRenderer renderer = TagRenderer.createInstance(entity.getType());
            Tag t = new Tag(entity, renderer, controllersMap.get(entity.getType()), lineFactory, states);
            tags.put(id, t);
            renderer.getNearby(t, 1.0).filter(input -> input != entity)
                    .forEach(player -> renderer.spawnTag(t, player, null));
        }
        return tags.get(id);
    }

    @Override
    public void deleteTag(Entity entity) {
        Tag t = tags.remove(entity.getEntityId());
        if (t == null) return;
        TagRenderer r = t.getRenderer();
        r.getNearby(t, 1.1).forEach(player -> r.destroyTag(t, player, null));
        r.purge(t);
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


    @Override
    public void update(ITagController controller, Player target) {
        states.getVisible(target).filter(input -> input.getTagControllers(false).contains(controller))
                .forEach(tag -> tag.update(controller));
    }

    @Override
    public void update(ITagController controller) {
        Bukkit.getOnlinePlayers().forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        states.getVisible(target).forEach(tag -> tag.update(line));
    }

    @Override
    public void update(ITagController.TagLine line) {
        Bukkit.getOnlinePlayers().forEach(player -> update(line, player));
    }

    @Override
    public void updateNames(Player target) {
        states.getVisible(target).forEach(tag -> tag.updateName(target));
    }

    @Override
    public void updateNames() {
        tags.values().forEach(Tag::updateName);
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
                return "One";
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
                return "Three";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final Set<Entity> enabledFor;

        DemoController(MultiLineAPI parent) {
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
        public String getName(Entity target, Player viewer, String previous) {
            return "- " + previous + " -";
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

    public static class DemoController2 implements ITagController {
        private static DemoController2 inst;
        private final MultiLineAPI parent;
        private final int refreshes = 15;
        private final TagLine line = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                return "One TWO";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final TagLine line2 = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                return null;
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final TagLine line3 = new TagLine() {
            @Override
            public String getText(Entity target, Player viewer) {
                return "Three TWO";
            }

            @Override
            public boolean keepSpaceWhenNull(Entity target) {
                return false;
            }
        };
        private final Set<Entity> enabledFor;

        DemoController2(MultiLineAPI parent) {
            this.parent = parent;
            this.enabledFor = new HashSet<>();
        }

        static DemoController2 getInst(MultiLineAPI parent) {
            if (inst == null) inst = new DemoController2(parent);
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
        public String getName(Entity target, Player viewer, String previous) {
            return ChatColor.AQUA + "! " + previous + " !";
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
            return 50;
        }

        @Override
        public int getNamePriority() {
            return -10;
        }
    }
}
