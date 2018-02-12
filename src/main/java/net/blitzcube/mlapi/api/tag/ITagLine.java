package net.blitzcube.mlapi.api.tag;

import org.bukkit.entity.Player;

/**
 * Represents a single line in an {@link ITag}
 * 
 * @author Parker Hawke - 2008Choco
 */
public interface ITagLine {

    /**
     * Set the cached value of this tag line
     * 
     * @param cached the new text to be cached for this line
     */
    public void setCached(String cached);

    /**
     * Get the cached value of this tag line
     * 
     * @return the cached text for this line
     */
    public String getCached();

    /**
     * Get the on this tag line according to the specified player
     * 
     * @param who the player viewing this line
     * @return the text for the specified player
     */
    public String getText(Player who);

    /**
     * Check whether the space should be maintained even if this line has no content
     * 
     * @return true if kept when null, false otherwise
     */
    public boolean keepSpaceWhenNull();

}