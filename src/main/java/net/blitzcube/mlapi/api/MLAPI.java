package net.blitzcube.mlapi.api;

import java.util.List;
import java.util.function.Function;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.api.tag.ITagLine;

/**
 * A utility class to bridge API and implementation for MultiLineAPI. The entry point
 * for all API calls should be done through this class!
 * 
 * @author Parker Hawke - 2008Choco
 */
public class MLAPI {

    private static IMultiLineAPI implementation;

    /**
     * Set the MultiLineAPI implementation. This should be done internally by the implementation
     * for this API. If invoked after implementation has been set, an exception will be thrown
     * 
     * @param implementation the MultiLineAPI implementation
     */
    public static void setImplementation(IMultiLineAPI implementation) {
        if (MLAPI.implementation != null) {
            throw new ExceptionInInitializerError("Two MLAPI implementations cannot be set at the same time");
        }

        MLAPI.implementation = implementation;
    }

    /**
     * Get the implementation for MultiLineAPI. MLAPI has static utility methods identical to that
     * of the implementation for the sake of ease of access, therefore retrieval of the implementation
     * is not necessarily required, but still available to those that prefer to hold an instance of
     * it instead
     * 
     * @return the MultiLineAPI implementation
     */
    public static IMultiLineAPI getImplementation() {
        return implementation;
    }

    /**
     * Set whether tags should auto enable for players as they join the server
     * 
     * @param enable true if should auto enable, false otherwise
     */
    public static void setAutoEnable(boolean enable) {
        implementation.setAutoEnable(enable);
    }

    /**
     * Check whether tags will automatically enable for players when joining the
     * server or not
     * 
     * @return true if auto enable, false otherwise
     */
    public boolean shouldAutoEnable() {
        return implementation.shouldAutoEnable();
    }

    /**
     * Set whether tags should auto disable for players as they leave the server
     * 
     * @param disable true if should auto disable, false otherwise
     */
    public static void setAutoDisable(boolean disable) {
        implementation.setAutoDisable(disable);
    }

    /**
     * Check whether tags will automatically disable for players when leaving the
     * server or not
     * 
     * @return true if auto disable, false otherwise
     */
    public boolean shouldAutoDisable() {
        return implementation.shouldAutoDisable();
    }

    /**
     * Enable an entity's custom tags
     * 
     * @param entity the entity whose tags to enable
     */
    public static void enable(Entity entity) {
        implementation.enable(entity);
    }

    /**
     * Check whether an entity's custom tags are enabled or not
     * 
     * @param entity the entity to check
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled(Entity entity) {
        return implementation.isEnabled(entity);
    }

    /**
     * Disable an entity's custom tags
     * 
     * @param entity the entity whose tags to disable
     */
    public static void disable(Entity entity) {
        implementation.disable(entity);
    }

    /**
     * Disable the custom tags for all entities
     */
    public void disable() {
        implementation.disable();
    }

    /**
     * Show an entity's custom tags. This does NOT enable them. Tags must be
     * enabled before this method is invoked
     * 
     * @param entity the entity whose tags to show
     */
    public static void show(Entity entity) {
        implementation.show(entity);
    }

    /**
     * Hide an entity's custom tags. This does NOT disable them. Tags must be
     * disabled through the {@link #disable(Entity)} method
     * 
     * @param entity the entity whose tags to hide
     */
    public static void hide(Entity entity) {
        implementation.hide(entity);
    }

    /**
     * Show an entity's custom tags to a specific set of players. This does NOT
     * enable them. Tags must be enabled before this method is invoked
     * 
     * @param entity the entity whose tags to show
     * @param forWho the players to which the tags should be shown
     */
    public static void show(Entity entity, Player... forWho) {
        implementation.show(entity, forWho);
    }

    /**
     * Hide an entity's custom tags from a specific set of players. This does NOT
     * disable them. Tags must be disabled through the {@link #disable(Entity)} method
     * 
     * @param entity the entity whose tags to hide
     * @param forWho the players from which the tags should be hidden
     */
    public static void hide(Entity entity, Player... forWho) {
        implementation.hide(entity, forWho);
    }

    /**
     * Reset an entity's tags to default (i.e. just the name tag). This does NOT
     * disable them. Tags must be disabled through the {@link #disable(Entity)} method
     * 
     * @param entity the entity whose tags to reset
     * @param forWho the players who will see the reset
     */
    public static void reset(Entity entity, Player... forWho) {
        implementation.reset(entity, forWho);
    }

    /**
     * Check whether an entity's tags are hidden from all players or not
     * 
     * @param entity the entity to check
     * @return true if hidden, false otherwise
     */
    public boolean isHidden(Entity entity) {
        return implementation.isHidden(entity);
    }

    /**
     * Check whether an entity's tags are hidden from a set of specific players
     * 
     * @param entity the entity to check
     * @param player the set of players to check
     * 
     * @return true if hidden from the specified players, false otherwise
     */
    public boolean isHiddenFor(Entity entity, Player player) {
        return implementation.isHiddenFor(entity, player);
    }

    /**
     * Add a custom tag controller implementation to a specific entity
     * 
     * @param entity the entity to which the controller should be added
     * @param controllers the controller implementation to add
     * 
     * @see ITagController
     */
    public static void addTagControllers(Entity entity, ITagController... controllers) {
        implementation.addTagControllers(entity, controllers);
    }

    /**
     * Remove a custom tag controller implementation from the specified entity
     * 
     * @param entity the entity from which the controller should be removed
     * @param controller the controller implementation to remove
     * 
     * @return true if successful, false otherwise (i.e. didn't exist or unsuccessful)
     */
    public boolean removeTagController(Entity entity, ITagController controller) {
        return implementation.removeTagController(entity, controller);
    }

    /**
     * Get a list of all tag controllers on the specified entity
     * 
     * @param entity the entity to check
     * @return a list of all tag controllers
     */
    public List<ITagController> getTagControllers(Entity entity) {
        return implementation.getTagControllers(entity);
    }

    /**
     * Create a new instance of ITagLine with the provided values and a dynamic text displayed to
     * specific players
     * 
     * @param text the text to be held on the tag line
     * @param keepSpaceWhenNull if true, the line will display an empty line if the text is null
     * @param dynamicText the text to be displayed to specific players
     * 
     * @return the created tag
     */
    public static ITagLine createTagLine(String text, boolean keepSpaceWhenNull, Function<Player, String> dynamicText) {
        return implementation.createTagLine(text, keepSpaceWhenNull, dynamicText);
    }

    /**
     * Create a new instance of ITagLine with the provided values
     * 
     * @param text the text to be held on the tag line
     * @param keepSpaceWhenNull if true, the line will display an empty line if the text is null
     * 
     * @return the created tag
     */
    public static ITagLine createTagLine(String text, boolean keepSpaceWhenNull) {
        return implementation.createTagLine(text, keepSpaceWhenNull);
    }

    /**
     * Create a new instance of ITagLine with the provided text, no space if the text is null and
     * a dynamic text displayed to specific players
     * 
     * @param text the text to be held on the tag line
     * @param dynamicText the text to be displayed to specific players
     * 
     * @return the created tag
     */
    public static ITagLine createTagLine(String text, Function<Player, String> dynamicText) {
        return implementation.createTagLine(text, dynamicText);
    }

    /**
     * Create a new instance of ITagLine with the provided text and no space if the text is null
     * 
     * @param text the text to be held on the tag line
     * @return the created tag
     */
    public static ITagLine createTagLine(String text) {
        return implementation.createTagLine(text);
    }

    /**
     * Get the specified entity's tag custom tag (if enabled)
     * 
     * @param entity the entity whose tag to retrieve
     * @return the entity's tag. null if not enabled
     */
    public static ITag getTag(Entity entity) {
        return implementation.getTag(entity);
    }

    /**
     * Refresh an entity's custom tags (including its name). Not to be confused with 
     * {@link #reset(Entity, Player...)}, this will update an entity's tags and re-display
     * them to all players who are capable of seeing them
     * 
     * @param entity the entity whose tags to refresh
     */
    public static void refresh(Entity entity) {
        implementation.refresh(entity);
    }

    /**
     * Refresh an entity's custom (non-name) tags. Not to be confused with
     * {@link #reset(Entity, Player...)}, this will update an entity's tags and re-display
     * them to all players who are capable of seeing them
     * 
     * @param entity the entity whose tags to refresh
     */
    public static void refreshLines(Entity entity) {
        implementation.refreshLines(entity);
    }

    /**
     * Refresh an entity's name tag. Not to be confused with {@link #refreshLines(Entity)},
     * this will update an entity's nametag and re-display it to all players who are capable
     * of seeing it
     * 
     * @param entity the entity whose name to refresh
     */
    public static void refreshName(Entity entity) {
        implementation.refreshName(entity);
    }

    /**
     * Refresh an entity's custom tags (including its name). Not to be confused with 
     * {@link #reset(Entity, Player...)}, this will update an entity's tags and re-display
     * them to the specified set of players
     * 
     * @param entity the entity whose tags to refresh
     * @param forWho the entities who will see the refreshed tags
     */
    public static void refresh(Entity entity, Player... forWho) {
        implementation.refresh(entity, forWho);
    }

    /**
     * Refresh an entity's custom (non-name) tags. Not to be confused with
     * {@link #reset(Entity, Player...)}, this will update an entity's tags and re-display
     * them to the specified set of players
     * 
     * @param entity the entity whose tags to refresh
     * @param forWho the entities who will see the refreshed tags
     */
    public static void refreshLines(Entity entity, Player... forWho) {
        implementation.refreshLines(entity, forWho);
    }

    /**
     * Refresh an entity's name tag. Not to be confused with {@link #refreshLines(Entity)},
     * this will update an entity's nametag and re-display it to the specified set of players
     * 
     * @param entity the entity whose name to refresh
     * @param forWho the entities who will see the refreshed tags
     */
    public static void refreshName(Entity entity, Player... forWho) {
        implementation.refreshName(entity, forWho);
    }

    /**
     * Hide all custom tags from the specified players
     * 
     * @param forWho the players from which all tags should be hidden
     * @return the list of entities whose tags were hidden
     */
    public static List<Entity> hideAllFor(Player... forWho) {
        return implementation.hideAllFor(forWho);
    }

    /**
     * Show all custom tags to the specified players
     * 
     * @param forWho the players to which all tags will be shown
     * @return the list of entities whose tags were shown
     */
    public static List<Entity> showAllFor(Player... forWho) {
        return implementation.showAllFor(forWho);
    }

}