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

    protected static double LINE_HEIGHT = 0.275D, BOTTOM_LINE_HEIGHT = 0.12D;
    private static final Map<EntityType, TagRenderer> RENDERERS = new HashMap<>();

    protected final IPacketEntityAPI packetAPI;
    protected final VisibilityStates state;
    protected final LineEntityFactory lineFactory;
    protected final JavaPlugin parent;

    protected TagRenderer(IPacketEntityAPI packet, LineEntityFactory lineFactory, VisibilityStates state, JavaPlugin parent) {
        this.packetAPI = packet;
        this.state = state;
        this.lineFactory = lineFactory;
        this.parent = parent;
    }

    public static void init(IPacketEntityAPI packetAPI, LineEntityFactory lineFactory, VisibilityStates states, MultiLineAPI parent, FileConfiguration config) {
        LINE_HEIGHT = config.getDouble("options.lineHeight", LINE_HEIGHT);
        BOTTOM_LINE_HEIGHT = config.getDouble("options.bottomLineHeight", BOTTOM_LINE_HEIGHT);

        TagRenderer mountRenderer = null, teleportRenderer = null;

        String defaultValue = config.getString("defaultRenderer", "mount");
        TagRenderer defaultRenderer = null;

        if (defaultValue.equalsIgnoreCase("mount")) {
            defaultRenderer = new MountTagRenderer(packetAPI, lineFactory, states, parent);
            mountRenderer = defaultRenderer;
        } else if (defaultValue.equalsIgnoreCase("teleport")) {
            defaultRenderer = new TeleportTagRenderer(packetAPI, lineFactory, states, parent, config.getBoolean("options.teleport.animated", true));
            teleportRenderer = defaultRenderer;
        } else {
            parent.getLogger().severe("Could not find renderer for name `" + defaultValue + "`!");
        }

        for (EntityType type : EntityType.values()) {
            String value = config.getString("typeRenderers." + type.name());

            if (value != null) {
                if (value.equalsIgnoreCase("mount")) {
                    if (mountRenderer == null) {
                        mountRenderer = new MountTagRenderer(packetAPI, lineFactory, states, parent);
                    }

                    RENDERERS.put(type, mountRenderer);
                } else if (value.equalsIgnoreCase("teleport")) {
                    if (teleportRenderer == null) {
                        teleportRenderer = new TeleportTagRenderer(packetAPI, lineFactory, states, parent, config.getBoolean("options.teleport.animated"));
                    }

                    RENDERERS.put(type, teleportRenderer);
                } else {
                    parent.getLogger().severe("Could not find renderer for name `" + value + "`!");
                }
            } else {
                RENDERERS.put(type, defaultRenderer);
            }
        }
    }

    public static TagRenderer createInstance(EntityType type) {
        return RENDERERS.get(type);
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

    public abstract void spawnTag(Tag tag, Player player, IEntityMountPacket mountPacket);

    public void destroyTag(Tag tag, Player player, IEntityDestroyPacket destroyPacket) {
        boolean event = (destroyPacket != null);
        IEntityDestroyPacket finalDestroyPacket = (!event) ? packetAPI.getPacketFactory().createDestroyPacket() :
                destroyPacket;
        tag.getLines().forEach(l -> l.getStack().forEach(e -> finalDestroyPacket.addToGroup(e.getIdentifier())));

        this.state.getSpawnedLines(player).removeAll(tag.getLines());
        if (tag.getBottom() != null)
            finalDestroyPacket.addToGroup(tag.getBottom().getIdentifier());
        finalDestroyPacket.addToGroup(tag.getTop().getIdentifier());

        if (!event) {
            this.packetAPI.dispatchPacket(finalDestroyPacket, player, 0);
        }

        this.state.removeSpawnedTag(player, tag);
    }

    public void updateName(Tag tag, Player viewer) {
        String name = (tag.getTarget() instanceof Player) ? ((Player) tag.getTarget()).getDisplayName() : tag.getTarget().getCustomName();

        for (ITagController controller : tag.getTagControllers(false)) {
            name = controller.getName(tag.getTarget(), viewer, name);
            if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) {
                name += ChatColor.RESET;
            }
        }

        this.lineFactory.updateName(tag.getTop(), name);
        this.packetAPI.dispatchPacket(packetAPI.getPacketFactory().createDataPacket(tag.getTop().getIdentifier()), viewer);
    }

    public abstract IFakeEntity createBottom(Tag target);

    public abstract IFakeEntity createTop(Tag target);

    public abstract void purge(Tag tag);

    public abstract LinkedList<IFakeEntity> createStack(Tag tag, int addIndex);

    public abstract void purge(IFakeEntity entity);
}

