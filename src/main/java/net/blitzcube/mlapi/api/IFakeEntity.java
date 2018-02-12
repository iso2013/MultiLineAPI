package net.blitzcube.mlapi.api;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Represents an invisible fake entity that sits on a player's head. These are the entities
 * that hold the custom tags for all players and are merely packet-based entities. They do not
 * interact with the world at all nor do they exist for any purpose beyond holding a nameplate
 * 
 * @author Parker Hawke - 2008Choco
 */
public interface IFakeEntity {

    /**
     * Get the unique integer ID for this entity
     * 
     * @return the entity's ID
     */
    public int getEntityId();

    /**
     * Get the unique UUID for this entity
     * 
     * @return the entity's UUID
     */
    public UUID getUniqueId();

    /**
     * Get the type of entity represented by this entity
     * 
     * @return the entity's type
     */
    public EntityType getType();

    /**
     * Spawn this fake entity in the world at the specified location for the provided players.
     * This method will not have much effect for API usage as entities will already have been
     * spawned by the MultiLineAPI implementation
     * 
     * @param location the location at which the entity should be spawned
     * @param forWho the set of players capable of "seeing" the spawned entity
     */
    public void spawn(Location location, Player... forWho);

    /**
     * Spawn this fake entity in the world at the specified coordinates for the provided players.
     * This method will not have much effect for API usage as entities will already have been
     * spawned by the MultiLineAPI implementation
     * 
     * @param x the x spawn coordinate
     * @param y the y spawn coordinate
     * @param z the z spawn coordinate
     * @param pitch the spawn pitch
     * @param yaw the spawn yaw
     * @param forWho the set of players capable of "seeing" the spawned entity
     */
    public void spawn(double x, double y, double z, float pitch, float yaw, Player... forWho);

    /**
     * Update this entity's metadata for the specified set of players
     * 
     * @param forWho the set of players who will recognize the metadata updates
     */
    public void updateMetadata(Player... forWho);

}