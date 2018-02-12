package net.blitzcube.mlapi.api.tag;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.comphenix.protocol.events.PacketContainer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.api.IFakeEntity;

/**
 * Represents an entity's custom tag
 * 
 * @author Parker Hawke - 2008Choco
 */
public interface ITag {

    /**
     * Get the base ID for this tag
     * 
     * @return the tag's base ID
     */
    public int getBaseId();

    /**
     * Refresh this tag's lines on the specified entity for the specified player
     * 
     * @param what the entity on which this tag should be refreshed
     * @param who the player to which this tag should be refreshed
     * 
     * @return the packet containers involved in this refresh action
     */
    public Set<PacketContainer> refreshLines(Entity what, Player who);

    /**
     * Refresh the name tag on the specified entity for the specified player
     * 
     * @param what the entity on which this tag's name should be refreshed
     * @param who the player to which this tag's name should be refreshed
     * 
     * @return the packet container involved in this refresh action
     */
    public PacketContainer refreshName(Entity what, Player who);
    
    /**
     * Get a list of all tag controllers on this tag
     * 
     * @return this tag's controllers
     */
    public List<ITagController> getControllers();
    
    /**
     * Get a list of all lines in this tag
     * 
     * @return this tag's lines
     */
    public List<ITagLine> getLines();

    /**
     * Get a list of the last entities that were rendered on this tag
     * 
     * @return the last rendered entities
     */
    public List<IFakeEntity> getLast();

    /**
     * Set whether this tag should be hidden to the specified UUID
     * 
     * @param uuid the UUID from which to hide this tag
     * @param value the new hidden state
     */
    public void setHidden(UUID uuid, boolean value);

    /**
     * Check whether this tag is hidden from the specified UUID
     * 
     * @param uuid the UUID to check
     * @return true if hidden, false otherwise
     */
    public boolean isHidden(UUID uuid);

    /**
     * Set whether this tag should be hidden from all players
     * 
     * @param value the new hidden state
     */
    public void setHiddenForAll(boolean value);

    /**
     * Check whether this tag is hidden from all players or not
     * 
     * @return true if hidden, false otherwise
     */
    public boolean isHiddenForAll();

    /**
     * Check whether the specified UUID is capable of viewing this tag or not
     * 
     * @param uuid the UUID to check
     * @return true if visible, false otherwise
     */
    public boolean isVisible(UUID uuid);

}