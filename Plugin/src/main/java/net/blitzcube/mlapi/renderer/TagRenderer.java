package net.blitzcube.mlapi.renderer;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.mount.MountTagRenderer;
import net.blitzcube.mlapi.renderer.teleport.TeleportTagRenderer;
import net.blitzcube.mlapi.structure.transactions.StructureTransaction;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.packet.IEntityDestroyPacket;
import net.blitzcube.peapi.api.packet.IEntityMountPacket;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 8/7/2018.
 */
public abstract class TagRenderer {
    protected static final double LINE_HEIGHT = 0.275;
    protected static final double BOTTOM_LINE_HEIGHT = 0.12;
    private static Map<EntityType, TagRenderer> renderers = new HashMap<>();
    protected final IPacketEntityAPI packetAPI;
    protected final VisibilityStates state;
    protected final LineEntityFactory lineFactory;
    protected final JavaPlugin parent;

    public TagRenderer(IPacketEntityAPI packet, LineEntityFactory lineFactory, VisibilityStates state, JavaPlugin
            parent) {
        this.packetAPI = packet;
        this.state = state;
        this.lineFactory = lineFactory;
        this.parent = parent;
    }

    public static void init(IPacketEntityAPI packetAPI, LineEntityFactory lineFactory, VisibilityStates states,
                            MultiLineAPI parent, FileConfiguration config) {
        TagRenderer mtr = null, tptr = null;

        String defVal = config.getString("defaultRenderer");
        TagRenderer def = null;
        if (defVal.equalsIgnoreCase("mount")) {
            def = new MountTagRenderer(packetAPI, lineFactory, states, parent);
            mtr = def;
        } else if (defVal.equalsIgnoreCase("teleport")) {
            def = new TeleportTagRenderer(packetAPI, lineFactory, states, parent);
            tptr = def;
        } else {
            parent.getLogger().severe("Could not find renderer for name `" + defVal + "`!");
        }

        for (EntityType t : EntityType.values()) {
            String val = config.getString("typeRenderers." + t.name());
            if (val != null) {
                if (val.equalsIgnoreCase("mount")) {
                    if (mtr == null) mtr = new MountTagRenderer(packetAPI, lineFactory, states, parent);
                    renderers.put(t, mtr);
                } else if (val.equalsIgnoreCase("teleport")) {
                    if (tptr == null) tptr = new TeleportTagRenderer(packetAPI, lineFactory, states, parent);
                    renderers.put(t, tptr);
                } else {
                    parent.getLogger().severe("Could not find renderer for name `" + val + "`!");
                }
            } else {
                renderers.put(t, def);
            }
        }
    }

    public static TagRenderer createInstance(EntityType type) {
        return renderers.get(type);
    }

    public static void batchDestroyTags(IPacketEntityAPI packetAPI, Stream<Tag> tags, Player player) {
        IEntityDestroyPacket destroyPacket = packetAPI.getPacketFactory().createDestroyPacket();
        tags.forEach(tag -> tag.getRenderer().destroyTag(tag, player, destroyPacket));
        packetAPI.dispatchPacket(destroyPacket, player);
    }

    public Stream<Player> getNearby(Tag tag, double err) {
        return packetAPI.getViewers(packetAPI.wrap(tag.getTarget()), err);
    }

    public abstract void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target);

    public abstract void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket);

    public void destroyTag(Tag t, Player p, IEntityDestroyPacket destroyPacket) {
        boolean event = destroyPacket != null;
        if (destroyPacket == null)
            destroyPacket = packetAPI.getPacketFactory().createDestroyPacket();
        IEntityDestroyPacket finalDestroyPacket = destroyPacket;
        t.getLines().forEach(l -> l.getStack().forEach(e -> finalDestroyPacket.addToGroup(e.getIdentifier())));
        state.getSpawnedLines(p).removeAll(t.getLines());
        finalDestroyPacket.addToGroup(t.getBottom().getIdentifier());
        finalDestroyPacket.addToGroup(t.getTop().getIdentifier());
        if (!event) {
            packetAPI.dispatchPacket(finalDestroyPacket, p, 0);
        }
        state.removeSpawnedTag(p, t);
    }

    public void updateName(Tag tag, Player viewer) {
        String name;
        name = tag.getTarget() instanceof Player ? ((Player) tag.getTarget()).getDisplayName() : tag.getTarget()
                .getCustomName();
        for (ITagController tc : tag.getTagControllers(false)) {
            name = tc.getName(tag.getTarget(), viewer, name);
            if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) name = name + ChatColor.RESET;
        }
        lineFactory.updateName(tag.getTop(), name);
        packetAPI.dispatchPacket(packetAPI.getPacketFactory().createDataPacket(tag.getTop().getIdentifier()), viewer);
    }

    public abstract IFakeEntity createBottom(Tag target);

    public abstract IFakeEntity createTop(Tag target);

    public abstract void purge(Tag t);

    public abstract LinkedList<IFakeEntity> createStack(Tag tag, int addIndex);

    public abstract void purge(IFakeEntity entity);
}

