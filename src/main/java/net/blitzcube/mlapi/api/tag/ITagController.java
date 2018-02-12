package net.blitzcube.mlapi.api.tag;

import java.util.Collection;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Represents a tag controller. Used to have more fine control over tags and their lines
 * 
 * @author Parker Hawke - 2008Choco
 */
public interface ITagController {

    /**
     * Get a collection of all tag lines to be displayed for the specified entity
     * 
     * @param what the entity
     * @return a list of tag lines to be displayed
     */
    public Collection<ITagLine> getLines(Entity what);

    /**
     * Get the name of the entity according to the specified player
     * 
     * @param what the entity whose name to check
     * @param who the player viewing the entity
     * 
     * @return the name of the entity
     */
    public String getName(Entity what, Player who);

    /**
     * Get the priority for this tag controller
     * 
     * @return the tag controller priority
     */
    public default int getPriority() {
        return 0;
    }

    /**
     * Get the priority of the name tag
     * 
     * @return the name tag priority
     */
    public default int getNamePriority() {
        return 0;
    }

}