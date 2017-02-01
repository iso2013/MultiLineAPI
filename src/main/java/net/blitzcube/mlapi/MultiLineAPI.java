package net.blitzcube.mlapi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MultiLineAPI extends JavaPlugin {

    //The static instance of the API.
    protected static MultiLineAPI inst;
    //All player's Tag objects that correspond to their players.
    public final Map<UUID, Tag> tags;
    //The list of currently registered controllers.
    private final List<TagController> registeredControllers;
    //The packet handler for ProtocolLib. Used for controlling mount packets and despawn packets.
    public PacketListener pckt;
    //The event handler. Used for automatic enabling, entity relocation, and repairing on teleportation.
    private EventListener evnt;

    /*
    I use a constructor for initializing non-Bukkit variables to new objects, and onEnable for setting their values
    or calling actual updates.
     */
    public MultiLineAPI() {
        MultiLineAPI.inst = this;
        tags = Maps.newHashMap();
        registeredControllers = Lists.newArrayList();
    }

    /**
     * Checks if the event listener will automatically enable new players.
     *
     * @return whether or not new players will be automatically enabled
     */
    public static boolean isAutoEnablePlayers() {
        return inst.evnt.isAutoEnablePlayers();
    }

    /**
     * Sets whether or not new players should be automatically enabled.
     *
     * @param val whether or not new players will be automatically enabled
     */
    public static void setAutoEnablePlayers(boolean val) {
        inst.evnt.setAutoEnablePlayers(val);
    }

    /**
     * Checks if the event listener will automatically enable new players.
     *
     * @return whether or not new players will be automatically enabled
     */
    public static boolean isAutoEnableEntities() {
        return inst.evnt.isAutoEnableEntities();
    }

    /**
     * Sets whether or not new players should be automatically enabled.
     *
     * @param val whether or not new players will be automatically enabled
     */
    public static void setAutoEnableEntities(boolean val) {
        inst.evnt.setAutoEnableEntities(val);
    }

    /**
     * Enables the API for usage on the specified player.
     *
     * @param e the player to enable
     */
    public static void enable(Entity e) {
        if (!inst.tags.containsKey(e.getUniqueId())) {
            Preconditions.checkArgument(!e.hasMetadata("STACK_ENTITY"), "Cannot create a Tag object for an entity " +
                    "that is part of a Tag!");
            inst.tags.put(e.getUniqueId(), new Tag(e, inst.registeredControllers));
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                inst.tags.get(e.getUniqueId()).getEvent().create(inst.pckt);
                if (e instanceof LivingEntity && ((LivingEntity) e).hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    inst.tags.get(e.getUniqueId()).despawn();
                }
            }, 1L);
        }
    }

    /**
     * Disables the API for usage on the specified player.
     *
     * @param e the player to disable
     */
    public static void disable(Entity e) {
        if (inst.tags.containsKey(e.getUniqueId())) {
            inst.tags.remove(e.getUniqueId()).remove();
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
     * @param e the player to check the status of
     * @return whether or not the player is enabled
     */
    public static boolean isEnabled(Entity e) {
        return inst.tags.containsKey(e.getUniqueId());
    }

    /**
     * Gets the name which the main tag for a player is currently set to.
     *
     * @param e the player to get the name of
     * @return the player's name
     */
    public static TagLine getName(Entity e) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Entity does not have API enabled!");
        return inst.tags.get(e.getUniqueId()).getName();
    }

    /**
     * Gets a line by the specified index. Line numbers go from top to bottom, starting at zero and not including the
     * player's nametag. The line must exist in order to be retrieved.
     *
     * @param controller The controller to get a line for
     * @param e          The player to get a line of
     * @param lineIndex  The index of the line to get, starting at zero at the top and goes to the bottom
     * @return The line object that allows editing of the line
     */
    public static TagLine getLine(TagController controller, Entity e, int lineIndex) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(e.getUniqueId()).getLine(controller, lineIndex);
    }

    /**
     * Add a line to the specified player.
     *
     * @param controller The controller to add a line for
     * @param e          The player to add a line to
     * @return The line object that allows editing of the new line
     */
    public static TagLine addLine(TagController controller, Entity e) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        TagLine t = inst.tags.get(e.getUniqueId()).addLine(controller);
        inst.tags.get(e.getUniqueId()).getProtocol().hide(inst.pckt);
        return t;
    }

    /**
     * Remove a specified line of a player. Be sure the line you remove belongs to your plugin.
     *
     * @param controller The controller to remove a line from
     * @param e          The player to remove a line of
     * @param lineIndex  The index of the line to remove
     */
    public static void removeLine(TagController controller, Entity e, int lineIndex) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        inst.tags.get(e.getUniqueId()).removeLine(controller, lineIndex);
    }

    /**
     * Remove a specified line of a player. Be sure the line you remove belongs to your plugin.
     *
     * @param controller The controller to remove a line from
     * @param e          The player to remove a line of
     * @param line       The line to remove
     */
    public static void removeLine(TagController controller, Entity e, TagLine line) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        inst.tags.get(e.getUniqueId()).removeLine(controller, line);
    }

    /**
     * Get the number of lines a player's tag has. For appearance purposes, this is recommended to never be higher
     * than 3 or 4.
     *
     * @param e The player to get the line count of
     * @return The number of lines the player's tag has
     */
    public static int getLineCount(Entity e) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(e.getUniqueId()).getNumLines();
    }

    /**
     * Get the number of lines a player's tag has. For appearance purposes, this is recommended to never be higher
     * than 3 or 4.
     *
     * @param controller The controller to get the line count of
     * @param e          The player to get the line count of
     * @return The number of lines the player's tag has
     */
    public static int getLineCount(TagController controller, Entity e) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        return inst.tags.get(e.getUniqueId()).getNumLines(controller);
    }

    /**
     * Refresh a player's tag. Call after any tag addition or removal to update the tag for all players.
     *
     * @param e The player to refresh
     */
    public static void refresh(Entity e) {
        Preconditions.checkArgument(inst.tags.containsKey(e.getUniqueId()), "Player does not have API enabled!");
        Bukkit.getOnlinePlayers().stream()
                .filter(o -> o.getWorld().getUID().equals(e.getWorld().getUID()))
                .forEach(o -> inst.tags.get(e.getUniqueId()).getProtocol().sendPairs(o, inst.pckt, true));
    }

    /**
     * Refresh all players in a player's view for the specified player. Used in onWorldChange and onTeleport to
     * repair broken tags.
     *
     * @param p The player whose view should be refreshed
     */
    public static void refreshOthers(Player p) {
        inst.tags.values().stream()
                .filter(s -> s.getOwner().getWorld() == p.getWorld())
                .forEach(s -> {
                    s.getProtocol().sendPairs(p, inst.pckt, false);
                    s.getProtocol().hide(inst.pckt);
                });
    }

    /**
     * Clear all lines of a player. Be sure you are not removing other plugin's lines. Recommended to use removeLine
     * instead.
     *
     * @param e The player whose lines should be cleared
     */
    public static void clearLines(Entity e) {
        inst.tags.get(e.getUniqueId()).clear();
    }

    /**
     * Clear all lines of a player registered to a TagController.
     *
     * @param controller The controller to clear the lines of
     * @param e          The player whose lines should be cleared
     */
    public static void clearLines(TagController controller, Entity e) {
        inst.tags.get(e.getUniqueId()).clear(controller);
    }

    /**
     * Update the locations of the entities that correspond to a player. They are always at y=-10 below the player.
     * This is used in onMove, onTeleport, and onWorldChange to ensure the entities are still loaded by all clients
     * who can see the player they correspond to.
     *
     * @param e The player whose locations should be updated
     */
    public static void updateLocs(Entity e) {
        inst.tags.get(e.getUniqueId()).updateEntityLoc();
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
        pckt = new PacketListener(this);

        this.getServer().getPluginManager().registerEvents(evnt, this);
    }

    /*
    onDisable method for clearing all entities so they are not saved to disk.
     */
    @Override
    public void onDisable() {
        tags.values().forEach(Tag::remove);
        tags.clear();
    }
}
