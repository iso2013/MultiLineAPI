package net.blitzcube.mlapi.api;

import java.util.Collection;
import java.util.Set;

import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Represents the core of MultiLineAPI providing various utility methods and functionality
 * with regards to the API's primary goal.
 *
 * @author iso2013
 * @since May 23rd, 2018
 */
public interface IMultiLineAPI {

	/**
	 * Get the tag associated with the specified entity.
	 *
	 * @param entity the entity whose tag to retrieve
	 *
	 * @return the tag. null if none
	 */
    ITag getTag(Entity entity);

    /**
     * Get the tag associated with the specified entity. If no tag has yet been associated,
     * create one instead.
     *
     * @param entity the entity whose tag to retrieve
     *
     * @return the created tag
     */
    ITag getOrCreateTag(Entity entity);

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
     * by {@link ITagController#getAutoApplyFor()}.
     *
     * @param controller the controller to add
     */
    void addDefaultTagController(ITagController controller);

    /**
     * Remove a default tag controller.
     *
     * @param controller the controller to remove
     */
    void removeDefaultTagController(ITagController controller);

    /**
     * Get an immutable Set of all default tag controllers.
     *
     * @return all default tag controllers
     */
    Set<ITagController> getDefaultTagControllers();

    /**
     * Get an immutable Collection of all default tag controllers for the specified entity type
     *
     * @param type the type of entity
     *
     * @return all default tag controllers
     */
    Collection<ITagController> getDefaultTagControllers(EntityType type);

    /**
     * Update an entity's tag for the specified player.
     *
     * @param entity the entity whose tag to update
     * @param target the player for whom to receive the update
     *
     * @see #update(Entity)
     */
    void update(Entity entity, Player target);

    /**
     * Update an entity's tag for all players
     *
     * @param entity the entity whose tag to update
     *
     * @see #update(Entity, Player)
     */
    void update(Entity entity);

    /**
     * Update a tag controller for the specified player.
     *
     * @param controller the controller to update
     * @param target the player for whom to receive the update
     *
     * @see #update(ITagController)
     */
    void update(ITagController controller, Player target);

    /**
     * Update a tag controller for all players.
     *
     * @param controller the controller to update
     *
     * @see #update(ITagController, Player)
     */
    void update(ITagController controller);

    /**
     * Update a specific tag line for the specified player.
     *
     * @param line the line to update
     * @param target the player for whom to receive the update
     *
     * @see #update(ITagController.TagLine)
     */
    void update(ITagController.TagLine line, Player target);

    /**
     * Update a specific tag line for all players.
     *
     * @param line the line to update
     *
     * @see #update(ITagController.TagLine, Player)
     */
    void update(ITagController.TagLine line);

    /**
     * Update all names for the specified player.
     *
     * @param target the player for whom to update names
     */
    void updateNames(Player target);

    /**
     * Update all names for all players.
     */
    void updateNames();
}
