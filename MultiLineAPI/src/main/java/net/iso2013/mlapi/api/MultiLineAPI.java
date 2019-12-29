package net.iso2013.mlapi.api;

import net.iso2013.mlapi.api.tag.Tag;
import net.iso2013.mlapi.api.tag.TagController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;

/**
 * Represents the core of MultiLineAPI providing various utility methods and functionality
 * with regards to the API's primary goal.
 *
 * @author iso2013
 * @since May 23rd, 2018
 */
public interface MultiLineAPI {

	/**
	 * Get the tag associated with the specified entity.
	 *
	 * @param entity the entity whose tag to retrieve
	 *
     * @return the tag or null if none
	 */
    Tag getTag(Entity entity);

    /**
     * Get the tag associated with the specified entity. If no tag has yet been associated,
     * create one instead. Optionally sends the new tag to players.
     *
     * @param entity the entity whose tag to retrieve or create
     * @param notifyPlayers whether or not to send the new tag to players (should almost always be true).
     * @return the created tag
     */
    Tag getOrCreateTag(Entity entity, boolean notifyPlayers);

    /**
     * Delete the specified entity's tag (if present). If the entity has no tag, this method
     * will fail silently.
     *
     * @param entity the entity whose tag to delete
     */
    void deleteTag(Entity entity);

    /**
     * Check whether the specified entity has a tag or not.
     *
     * @param entity the entity to check
     *
     * @return true if the entity has a tag, false otherwise
     */
    boolean hasTag(Entity entity);

    /**
     * Add a default tag controller. Default tag controllers are applied to entity types specified
     * by {@link TagController#getAutoApplyFor()}.
     *
     * @param controller the controller to add
     */
    void addDefaultTagController(TagController controller);

    /**
     * Remove a default tag controller.
     *
     * @param controller the controller to remove
     */
    void removeDefaultTagController(TagController controller);

    /**
     * Get an immutable Set of all default tag controllers.
     *
     * @return all default tag controllers
     */
    Set<TagController> getDefaultTagControllers();

    /**
     * Get an immutable Collection of all default tag controllers for the specified entity type
     *
     * @param type the type of entity
     *
     * @return all default tag controllers
     */
    Collection<TagController> getDefaultTagControllers(EntityType type);

    /**
     * Update an entity's tag for the specified player.
     *
     * @param entity the entity whose tag should be updated
     * @param target the player who the update should be sent to
     *
     * @see #update(Entity)
     */
    void update(Entity entity, Player target);

    /**
     * Update an entity's tag for all players
     *
     * @param entity the entity whose tag should be updated
     *
     * @see #update(Entity, Player)
     */
    void update(Entity entity);

    /**
     * Update a tag controller for the specified player.
     *
     * @param controller the controller to update
     * @param target the player who the update should be sent to
     *
     * @see #update(TagController)
     */
    void update(TagController controller, Player target);

    /**
     * Update a tag controller for all players.
     *
     * @param controller the controller to update
     *
     * @see #update(TagController, Player)
     */
    void update(TagController controller);

    /**
     * Update a specific tag line for the specified player.
     *
     * @param line the line to update
     * @param target the player who the update should be sent to
     *
     * @see #update(TagController.TagLine)
     */
    void update(TagController.TagLine line, Player target);

    /**
     * Update a specific tag line for all players.
     *
     * @param line the line to update
     *
     * @see #update(TagController.TagLine, Player)
     */
    void update(TagController.TagLine line);

    /**
     * Update all tag names for the specified player.
     *
     * @param target the player who the update should be sent to
     */
    void updateNames(Player target);

    /**
     * Update all tag names for all players.
     */
    void updateNames();
}
