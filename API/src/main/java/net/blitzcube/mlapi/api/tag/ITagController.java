package net.blitzcube.mlapi.api.tag;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Represents a controller for a set of {@link ITag} instances. Controllers may be applied
 * by default to a select array of entity types (see {@link #getAutoApplyFor()}), or may be
 * used for one tag in specific (see {@link ITag#addTagController(ITagController)}). This
 * interface is meant to be implemented in order to further control how tags are processed
 * and viewed by players in the world.
 *
 * @author iso2013
 * @since May 23rd, 2018
 */
public interface ITagController {

	/**
	 * Get a List of tag lines to be shown for the specified entity. The value returned by
	 * this method should never be modified during the lifetime of a single instance of the
	 * server. The value should be constant.
	 *
	 * @param target the entity for which to retrieve tag lines
	 *
	 * @return the lines to display
	 */
    List<TagLine> getFor(Entity target);

    /**
     * Get the name to be displayed for the {@code target} with respect to the {@code viewer}.
     * This method will be called when an entity's tag has been update for any reason.
     *
     * @param target the entity for which to retrieve the name
     * @param viewer the player for whom to retrieve the name
     * @param previous the name prior to this update invocation
     *
     * @return the new name to be displayed
     */
    default String getName(Entity target, Player viewer, String previous) {
        return previous;
    }

    /**
     * Get an array of entity types to which this tag controller will be automatically applied.
     *
     * @return the entity types
     */
    EntityType[] getAutoApplyFor();

    /**
     * Get an instance of the JavaPlugin implementing this controller. Must not be null.
     *
     * @return the controlling plugin
     */
    JavaPlugin getPlugin();

    /**
     * Get this tag controller's priority. Higher priority controllers are rendered above
     * lower priority controllers. The higher the integer, the higher the priority. Negative
     * priorities are supported such that -1 is of a lower priority than 0.
     *
     * @return this controller's priority
     */
    int getPriority();

    /**
     * Get this tag controller's priority with regards to name updates. Higher priority
     * controllers are run after lower priority controllers. The higher the integer, the
     * higher the priority. Negative priorities are supported such that -1 is of a lower
     * priority than 0.
     *
     * @return this controller's name priority
     */
    int getNamePriority();

    /**
     * Represents a specific line in a {@link ITag}. A unique implementation of TagLine should
     * be created for each different line returned in {@link ITagController#getFor(Entity)}.
     *
     * @author iso2013
     * @since May 23rd, 2018
     */
    interface TagLine {

    	/**
    	 * Get the text to be displayed over the {@code target}'s head with respect to the
    	 * {@code viewer}.
    	 *
         * @param target the entity for which to retrieve the name
         * @param viewer the player for whom to retrieve the name
    	 *
    	 * @return the text to display
    	 */
        String getText(Entity target, Player viewer);

        /**
         * Check whether or not the space should be kept if {@code null} is returned by
         * {@link #getText(Entity, Player)}.
         *
         * @param target the entity for which to check
         *
         * @return true if the space should be kept, false if it should be removed
         */
        boolean keepSpaceWhenNull(Entity target);
    }
}
