package net.blitzcube.mlapi.api.tag;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Represents a dynamic tag over an entity's head.
 *
 * @author iso2013
 * @since May 23rd, 2018
 */
public interface ITag {

	/**
	 * Get an immutable List of all tag controllers applied on this tag.
	 *
     * @param sortByLines whether or not to sort by lines. If this is false, it sorts by the name priority instead.
	 *
	 * @return all active tag controllers
	 */
    ImmutableList<ITagController> getTagControllers(boolean sortByLines);

    /**
     * Add a tag controller to this tag.
     *
     * @param controller the controller to add
     */
    void addTagController(ITagController controller);

    /**
     * Remove a tag controller from this tag.
     *
     * @param controller the controller to remove
     */
    void removeTagController(ITagController controller);

    /**
     * Set whether or not this tag should be visible to the specified player.
     *
     * @param target the target for whom to update visibility
     * @param visible the new visibility state
     */
    void setVisible(Player target, boolean visible);

    /**
     * Reset the visibility state of this tag to default.
     *
     * @param target the target for whom to reset visibility
     */
    void clearVisible(Player target);

    /**
     * Check whether this tag is visible to the specified player.
     *
     * @param target the target to check
     *
     * @return true if visible, false otherwise. null if not explicitly set
     */
    Boolean isVisible(Player target);

    /**
     * Get the default value for this tag's visibility.
     *
     * @return the default visibility state
     */
    boolean getDefaultVisible();

    /**
     * Set the default value for this tag's visibility.
     *
     * @param visible the new default visibility state
     */
    void setDefaultVisible(boolean visible);

    /**
     * Update this tag for the specified player.
     *
     * @param target the player who the update should be sent to
     *
     * @see #update()
     */
    void update(Player target);

    /**
     * Update this tag for all players.
     *
     * @see #update(Player)
     */
    void update();

    /**
     * Update this tag for the specified player with respect to the lines of the specified tag controller.
     *
     * @param controller the controller whose lines should be updated
     * @param target the player who the update should be sent to
     *
     * @see #update(ITagController)
     */
    void update(ITagController controller, Player target);

    /**
     * Update this tag for all players with respect to the lines of the specified tag controller.
     *
     * @param controller the controller context with which to update
     *
     * @see #update(ITagController, Player)
     */
    void update(ITagController controller);

    /**
     * Update the specific line in this tag for the specified player.
     *
     * @param line the line to update
     * @param target the target for whom to receive the update
     *
     * @see #update(ITagController.TagLine)
     */
    void update(ITagController.TagLine line, Player target);

    /**
     * Update the specific line in this tag for all players.
     *
     * @param line the line to update
     *
     * @see #update(ITagController.TagLine, Player)
     */
    void update(ITagController.TagLine line);

    /**
     * Update this tag's name for the specified player.
     *
     * @param target the player who the update should be sent to
     *
     * @see #updateName()
     */
    void updateName(Player target);

    /**
     * Update this tag's name for all players.
     *
     * @see #updateName(Player)
     */
    void updateName();

    /**
     * Get the entity associated with this tag.
     *
     * @return the owning entity
     */
    Entity getTarget();
}
