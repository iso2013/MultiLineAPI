package net.blitzcube.mlapi.tag;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class TagLine {

    private final Tag parent;
    private final TagController controller;
    private final List<Entity> spaceEntities;
    private Entity lineEntity;
    private boolean keepSpaceWhenNull;
    private String text;

    /**
     * The default constructor for a TagLine. This should only be called through MultiLineAPI.
     *
     * @param parent The Tag that this TagLine belongs to
     * @param controller The TagController responsible for this tag
     */
    public TagLine(Tag parent, TagController controller) {
        this.parent = parent;
        this.controller = controller;
        this.lineEntity = parent.createArmorStand();
        this.spaceEntities = parent.createSpace();
        this.keepSpaceWhenNull = false;
        this.text = lineEntity.getCustomName();
    }

    /**
     * The line entity is the ArmorStand with the text as it's name. Called by MultiLineAPI.
     *
     * @return The entity that is used to display text
     */
    public Entity getLineEntity() {
        return lineEntity;
    }

    /**
     * The spacing entities are used to separate the armor stands of each tag line. Without them, the text would all
     * show up in the same position and be impossible to read. Called by MultiLineAPI.
     *
     * @return The list of entities that represent a space
     */
    public List<Entity> getSpaceEntities() {
        return spaceEntities;
    }

    /**
     * Whether or not the line should keep the space between the lines before and after when it's text value is null.
     * This should almost always be false for appearance purposes.
     *
     * @param b Whether or not the space should be kept when null
     */
    public void setKeepSpaceWhenNull(boolean b) {
        this.keepSpaceWhenNull = b;
    }

    /**
     * Gets whether or not the line should keep the space when it's value is null.
     *
     * @return Whether or not the space will be kept
     */
    public boolean keepSpaceWhenNull() {
        return keepSpaceWhenNull;
    }

    /**
     * Get the text value of the line.
     *
     * @return The text currently being displayed on the line
     */
    public String getText() {
        return lineEntity.getCustomName();
    }

    /**
     * Set the text value of the line. Set to a String to display it, or null to hide the TagLine.
     *
     * @param s The string that should be displayed
     */
    public void setText(String s) {
        lineEntity.setCustomName(s);
        lineEntity.setCustomNameVisible(s != null);
        text = s;
    }

    /**
     * Teleport the entities for this line to the specified location. Should only be called by MultiLineAPI
     *
     * @param entityLoc The location to teleport the entities to.
     */
    public void teleport(Location entityLoc) {
        lineEntity.teleport(entityLoc);
        for (Entity e : spaceEntities) {
            e.teleport(entityLoc);
        }
    }

    /**
     * Remove the tag line. Used in onDisable and when disabling the API for a player.
     */
    public void remove() {
        lineEntity.remove();
        spaceEntities.forEach(Entity::remove);
    }

    /**
     * Gets the tag this line belongs to.
     *
     * @return The tag this line belongs to
     */
    public Tag getParent() {
        return parent;
    }

    /**
     * Get the TagController this line belongs to.
     *
     * @return The TagController this line belongs to
     */
    public TagController getController() {
        return controller;
    }


    public void despawn() {
        lineEntity.remove();
        spaceEntities.forEach(Entity::remove);
        spaceEntities.clear();
    }

    public void respawn() {
        lineEntity = parent.createArmorStand();
        lineEntity.setCustomName(text);
        lineEntity.setCustomNameVisible(text != null);
        spaceEntities.addAll(parent.createSpace());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagLine tagLine = (TagLine) o;

        if (keepSpaceWhenNull != tagLine.keepSpaceWhenNull) return false;
        if (!parent.equals(tagLine.parent)) return false;
        if (!controller.equals(tagLine.controller)) return false;
        return text != null ? text.equals(tagLine.text) : tagLine.text == null;

    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + controller.hashCode();
        result = 31 * result + (keepSpaceWhenNull ? 1 : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
