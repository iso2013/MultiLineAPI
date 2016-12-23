package net.blitzcube.mlapi.tag;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class Tag {
    private ArrayList<Entity> baseEntities;
    private ArrayList<Entity> stack;
    private HashMap<Entity, Entity> pairings;

    private TagLine name;
    private ArrayList<TagLine> lines;

    private Player whoOwns;
    private Location entityLoc;

    public Tag(Player owner) {
        baseEntities = new ArrayList<>();
        stack = new ArrayList<>();
        pairings = new HashMap<>();
        lines = new ArrayList<>();
        whoOwns = owner;

        updateEntityLoc();
        name = new TagLine(this);
        name.setText(owner.getName());
        genBase();
        refreshPairings();
    }

    public TagLine getName() {
        return name;
    }

    public TagLine addLine() {
        TagLine newLine = new TagLine(this);
        lines.add(newLine);
        refreshPairings();
        return newLine;
    }

    public TagLine getLine(int index) {
        return lines.get(index);
    }

    public void clear() {
        lines.clear();
        refreshPairings();
    }

    public int getNumLines() {
        return lines.size();
    }

    public void removeLine(TagLine line) {
        removeLine(lines.indexOf(line));
        refreshPairings();
    }

    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) {
            throw new IllegalArgumentException("Index " + index + " was not found in list of size " + lines.size());
        }
        lines.remove(index).remove();
    }

    public int[] getEntityIds() {
        int[] ints = new int[stack.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = stack.get(i).getEntityId();
        }
        return ints;
    }

    public int[][] getEntityPairings() {
        int[] keys = new int[pairings.size()];
        int[] vals = new int[pairings.size()];
        ArrayList<Map.Entry<Entity, Entity>> entries = new ArrayList<>();
        entries.addAll(pairings.entrySet());
        for (int i = 0; i < keys.length; i++) {
            keys[i] = entries.get(i).getKey().getEntityId();
            vals[i] = entries.get(i).getValue().getEntityId();
        }
        return new int[][]{keys, vals};
    }

    private void genBase() {
        baseEntities.add(createGenericEntity(EntityType.SILVERFISH));
    }

    public void refreshPairings() {
        pairings.clear();
        stack.clear();
        stack.add(whoOwns);
        stack.addAll(baseEntities);
        for (TagLine line : lines) {
            if (line.getText() != null) {
                stack.add(line.getLineEntity());
            }
            if (line.keepSpaceWhenNull() || line.getText() != null) {
                stack.addAll(line.getSpaceEntities());
            }
        }
        stack.add(name.getLineEntity());
        for (int i = 0; i < stack.size(); i++) {
            if (i + 1 < stack.size()) {
                pairings.put(stack.get(i), stack.get(i + 1));
            }
        }
    }

    LivingEntity createGenericEntity(EntityType type) {
        LivingEntity e = (LivingEntity) entityLoc.getWorld().spawnEntity(entityLoc, type);
        e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false));
        e.setAI(false);
        e.setCanPickupItems(false);
        e.setCollidable(false);
        e.setGravity(false);
        e.setInvulnerable(true);
        e.setSilent(true);
        e.setMetadata("STACK_ENTITY", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("SecondLineAPI"),
                null));
        return e;
    }

    LivingEntity createSlime() {
        Slime s = (Slime) createGenericEntity(EntityType.SLIME);
        s.setSize(-1);
        return s;
    }

    LivingEntity createArmorStand() {
        ArmorStand as = (ArmorStand) createGenericEntity(EntityType.ARMOR_STAND);
        as.setMarker(true);
        as.setVisible(false);
        as.setCustomNameVisible(true);
        return as;
    }

    ArrayList<Entity> createSpace() {
        ArrayList<Entity> space = new ArrayList<>();
        space.add(createSlime());
        space.add(createGenericEntity(EntityType.SILVERFISH));
        space.add(createGenericEntity(EntityType.SILVERFISH));
        return space;
    }

    public void updateEntityLoc() {
        Location l = whoOwns.getLocation();
        l.setY(-10.0D);
        this.entityLoc = l;

        for (Entity e : baseEntities) {
            e.teleport(entityLoc);
        }
        for (TagLine t : lines) {
            t.teleport(entityLoc);
        }
    }

    public UUID getOwner() {
        return whoOwns.getUniqueId();
    }

    public void remove() {
        lines.forEach(TagLine::remove);
        baseEntities.forEach(Entity::remove);
    }
}