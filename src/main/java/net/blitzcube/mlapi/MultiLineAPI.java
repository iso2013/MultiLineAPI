package net.blitzcube.mlapi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketHandler;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.vanish.VanishPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MultiLineAPI extends JavaPlugin {

    //The static instance of the API.
    private static MultiLineAPI inst;
    //All player's Tag objects that correspond to their players.
    public final Map<UUID, Tag> tags;
    //The list of currently registered controllers.
    private final List<TagController> registeredControllers;
    //The vanish
    public VanishManager vnsh;
    //The packet handler for ProtocolLib. Used for controlling mount packets and despawn packets.
    private PacketHandler pckt;
    //The event handler. Used for automatic enabling, entity relocation, and repairing on teleportation.
    private EventListener evnt;
    private Map<String, Integer> trackingRanges;

    /*
    I use a constructor for initializing non-Bukkit variables to new objects, and onEnable for setting their values
    or calling actual updates.
     */
    public MultiLineAPI() {
        MultiLineAPI.inst = this;
        tags = Maps.newHashMap();
        registeredControllers = Lists.newArrayList();
        trackingRanges = Maps.newHashMap();
    }

    /**
     * Checks if the event listener will automatically enable new players.
     *
     * @return whether or not new players will be automatically enabled
     */
    public static boolean isAutoEnable() {
        return inst.evnt.isAutoEnable();
    }

    /**
     * Sets whether or not new players should be automatically enabled.
     *
     * @param val whether or not new players will be automatically enabled
     */
    public static void setAutoEnable(boolean val) {
        inst.evnt.setAutoEnable(val);
    }

    /**
     * Enables the API for usage on the specified player.
     *
     * @param p the player to enable
     */
    public static void enable(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId())) {
            inst.tags.put(p.getUniqueId(), new Tag(p, inst.registeredControllers));
        }
    }

    /**
     * Disables the API for usage on the specified player.
     *
     * @param p the player to disable
     */
    public static void disable(Player p) {
        if (inst.tags.containsKey(p.getUniqueId())) {
            inst.tags.remove(p.getUniqueId()).remove();
        }
    }

    /**
     * Disables everyone on the API. Used in onDisable for server stopping.
     */
    public static void disable() {
        inst.tags.values().forEach(Tag::remove);
        inst.tags.clear();
    }

    /**
     * Checks whether a player is enabled.
     *
     * @param p the player to check the status of
     * @return whether or not the player is enabled
     */
    public static boolean isEnabled(Player p) {
        return inst.tags.containsKey(p.getUniqueId());
    }

    /**
     * Gets the name which the main tag for a player is currently set to.
     *
     * @param p the player to get the name of
     * @return the player's name
     */
    public static TagLine getName(Player p) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getName();
    }

    /**
     * Gets a line by the specified index. Line numbers go from top to bottom, starting at zero and not including the
     * player's nametag. The line must exist in order to be retrieved.
     *
     * @param controller The controller to get a line for
     * @param p          The player to get a line of
     * @param lineIndex  The index of the line to get, starting at zero at the top and goes to the bottom
     * @return The line object that allows editing of the line
     */
    public static TagLine getLine(TagController controller, Player p, int lineIndex) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getLine(controller, lineIndex);
    }

    /**
     * Add a line to the specified player.
     *
     * @param controller The controller to add a line for
     * @param p          The player to add a line to
     * @return The line object that allows editing of the new line
     */
    public static TagLine addLine(TagController controller, Player p) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        TagLine t = inst.tags.get(p.getUniqueId()).addLine(controller);
        inst.hide(p);
        return t;
    }

    /**
     * Remove a specified line of a player. Be sure the line you remove belongs to your plugin.
     *
     * @param controller The controller to remove a line from
     * @param p          The player to remove a line of
     * @param lineIndex  The index of the line to remove
     */
    public static void removeLine(TagController controller, Player p, int lineIndex) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        inst.tags.get(p.getUniqueId()).removeLine(controller, lineIndex);
    }

    /**
     * Remove a specified line of a player. Be sure the line you remove belongs to your plugin.
     *
     * @param controller The controller to remove a line from
     * @param p          The player to remove a line of
     * @param line       The line to remove
     */
    public static void removeLine(TagController controller, Player p, TagLine line) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        inst.tags.get(p.getUniqueId()).removeLine(controller, line);
    }

    /**
     * Get the number of lines a player's tag has. For appearance purposes, this is recommended to never be higher
     * than 3 or 4.
     *
     * @param p The player to get the line count of
     * @return The number of lines the player's tag has
     */
    public static int getLineCount(Player p) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getNumLines();
    }

    /**
     * Get the number of lines a player's tag has. For appearance purposes, this is recommended to never be higher
     * than 3 or 4.
     *
     * @param controller The controller to get the line count of
     * @param p          The player to get the line count of
     * @return The number of lines the player's tag has
     */
    public static int getLineCount(TagController controller, Player p) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getNumLines(controller);
    }

    /**
     * Refresh a player's tag. Call after any tag addition or removal to update the tag for all players.
     *
     * @param p The player to refresh
     */
    public static void refresh(Player p) {
        Preconditions.checkArgument(inst.tags.containsKey(p.getUniqueId()), "Player does not have API enabled!");
        inst.refreshForEveryone(p);
    }

    /**
     * Refresh all players in a player's view for the specified player. Used in onWorldChange and onTeleport to
     * repair broken tags.
     *
     * @param p The player whose view should be refreshed
     */
    public static void refreshOthers(Player p) {
        inst.refreshView(p);
    }

    /**
     * Clear all lines of a player. Be sure you are not removing other plugin's lines. Recommended to use removeLine
     * instead.
     *
     * @param p The player whose lines should be cleared
     */
    public static void clearLines(Player p) {
        inst.tags.get(p.getUniqueId()).clear();
    }

    /**
     * Clear all lines of a player registered to a TagController.
     *
     * @param controller The controller to clear the lines of
     * @param p          The player whose lines should be cleared
     */
    public static void clearLines(TagController controller, Player p) {
        inst.tags.get(p.getUniqueId()).clear(controller);
    }

    /**
     * Update the locations of the entities that correspond to a player. They are always at y=-10 below the player.
     * This is used in onMove, onTeleport, and onWorldChange to ensure the entities are still loaded by all clients
     * who can see the player they correspond to.
     *
     * @param p The player whose locations should be updated
     */
    public static void updateLocs(Player p) {
        inst.tags.get(p.getUniqueId()).updateEntityLoc();
    }


    /**
     * Register a TagController class for use with MultiLineAPI.
     *
     * @param t The TagController to register
     */
    public static void register(TagController t) {
        if (!inst.registeredControllers.contains(t)) {
            inst.registeredControllers.add(t);
        }
    }

    /**
     * Check if a TagController is currently registered.
     *
     * @param t The TagController to check
     * @return Whether or not the TagController is registered.
     */
    public static boolean isRegistered(TagController t) {
        return inst.registeredControllers.contains(t);
    }

    /*
    onEnable method for loading the API as a plugin. Used to register the EventListener and the PacketHandler.
     */
    @Override
    public void onEnable() {
        evnt = new EventListener(this);
        pckt = new PacketHandler(this);
        vnsh = new VanishManager(this);

        this.getServer().getPluginManager().registerEvents(evnt, this);

        ConfigurationSection section = Bukkit.spigot().getSpigotConfig().getConfigurationSection("world-settings");
        for (String s : section.getKeys(false)) {
            trackingRanges.put(s, section.getInt(s + ".entity-tracking-range.players"));
        }
    }

    /*
    onDisable method for clearing all entities so they are not saved to disk.
     */
    @Override
    public void onDisable() {
        tags.values().forEach(Tag::remove);
        tags.clear();
    }

    /*
    Method for refreshing the view of a specified player. Used internally by #refreshOthers(Player).
     */
    private void refreshView(Player p) {
        tags.values().stream()
                .filter(s -> s.getOwner().getWorld() == p.getWorld())
                .forEach(s -> createPairs(s, p)
                );
    }

    /*
    Creates mount packets that pair the entities together to stack them client-side.
     */
    public void createPairs(Tag t, Player p) {
        t.refreshPairings();
        int[][] pairings = t.getEntityPairings();
        int[] keys = pairings[0], values = pairings[1];
        for (int i = 0; i < keys.length; i++) {
            pckt.sendMountPacket(p, keys[i], values[i]);
        }
    }

    /*
    Refreshes a specified player for all viewers. Used internally by #refresh(Player).
     */
    private void refreshForEveryone(Player p) {
        Bukkit.getOnlinePlayers().stream()
                .filter(o -> o.getWorld() == p.getWorld())
                .filter(o -> o != p)
                .forEach(o -> createPairs(tags.get(p.getUniqueId()), o)
                );
    }

    /*
    Used to hide a player's tag from himself, preventing the player's view and interactions from being obstructed by
    the hitboxes.
     */
    public void hide(Player p) {
        int[] entities = inst.tags.get(p.getUniqueId()).getEntities();
        Integer dist = trackingRanges.get(p.getWorld().getName());
        if (dist == null) dist = trackingRanges.get("default");
        for (Entity e : p.getNearbyEntities(dist, dist, 250)) {
            if (e instanceof Player) {
                if (!vnsh.canSee(p, (Player) e)) {
                    pckt.hide((Player) e, entities);
                }
            }
        }
        pckt.hide(p, entities);
    }

    public class VanishManager {
        boolean vanishNoPacket;

        org.kitteh.vanish.VanishManager manager;

        VanishManager(JavaPlugin parent) {
            vanishNoPacket = parent.getServer().getPluginManager().isPluginEnabled("VanishNoPacket");
            if (vanishNoPacket) {
                manager = ((VanishPlugin) parent.getServer().getPluginManager().getPlugin("VanishNoPacket"))
                        .getManager();
            }
        }

        public boolean canSee(Player who, Player forWho) {
            if (manager != null) {
                return forWho.canSee(who) && !manager.isVanished(who);
            } else {
                return forWho.canSee(who);
            }
        }
    }
}
