package net.blitzcube.mlapi.tag;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class TagLine {
    private Tag parent;
    private Entity lineEntity;
    private ArrayList<Entity> spaceEntities;
    private boolean keepSpaceWhenNull;

    public TagLine(Tag parent) {
        this.parent = parent;
        this.lineEntity = parent.createArmorStand();
        this.spaceEntities = parent.createSpace();
        this.keepSpaceWhenNull = false;
    }

    public Entity getLineEntity() {
        return lineEntity;
    }

    public ArrayList<Entity> getSpaceEntities() {
        return spaceEntities;
    }

    public void setKeepSpaceWhenNull(boolean b) {
        this.keepSpaceWhenNull = b;
    }

    public boolean keepSpaceWhenNull() {
        return keepSpaceWhenNull;
    }

    public String getText() {
        return lineEntity.getCustomName();
    }

    public void setText(String s) {
        lineEntity.setCustomName(s);
        lineEntity.setCustomNameVisible(s != null);
    }

    public void teleport(Location entityLoc) {
        lineEntity.teleport(entityLoc);
        for (Entity e : spaceEntities) {
            e.teleport(entityLoc);
        }
    }

    public void remove() {
        lineEntity.remove();
        spaceEntities.forEach(Entity::remove);
    }
}
