package net.blitzcube.mlapi.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class Tag {
    //The list of entities that compose the base of the tag. Currently it's just the SILVERFISH entity to separate
    // the player's tag from the player's head.
    private List<Entity> baseEntities;
    //The list of entities that make up the actual tag. This changes whenever the tag is refreshed, and is pulled
    // from the TagLine objects. See #refreshPairings()
    private List<Entity> stack;
    //The HashMap that represents pairings of entities that should be mounted on each other. Key is the vehicle,
    // Value is the passenger. This is also updated in #refreshPairings()
    private Map<Entity, Entity> pairings;

    //The TagLine object for the uppermost tag, - the player's name. It cannot be removed.
    private TagLine name;
    //A list of lines to show underneath the player's name. This should only be changed through addLine(), getLine(),
    // clear() and removeLine() methods.
    private List<TagLine> lines;

    //The player whom this tag belongs to.
    private Player whoOwns;
    //The location entities should be spawned at when creating this tag.
    private Location entityLoc;

    /**
     * The default constructor for the tag object. This class should not be used in most scenarios, use the main
     * class: MultiLineAPI.
     *
     * @param owner The player who owns the tag.
     */
    //Constructor just accepts the player who owns the tag, automatically updates the location, and generates the
    // base and pairings.
    public Tag(Player owner) {
        //Initialize lists and maps to empty values.
        baseEntities = Lists.newArrayList();
        stack = Lists.newArrayList();
        pairings = Maps.newHashMap();
        lines = Lists.newArrayList();
        //Set whoOwns to the player provided.
        whoOwns = owner;

        //Update the location entities should spawn at so spawning can be done.
        updateEntityLoc();
        //Create the new TagLine object that represents the player's first line of the name
        name = new TagLine(this);
        //Set it to the player's name
        name.setText(owner.getName());
        //Generate the base of the tag. Just a silverfish right now.
        genBase();
        //Refresh the pairings map so that pairings can be sent.
        refreshPairings();
    }

    /**
     * Retrieves the TagLine object that represents the first line of the nametag.
     *
     * @return The TagLine name object
     */
    public TagLine getName() {
        return name;
    }

    /**
     * Add a new line to the player's tag.
     *
     * @return The new line that has been added
     */
    public TagLine addLine() {
        TagLine newLine = new TagLine(this);
        lines.add(newLine);
        refreshPairings();
        return newLine;
    }
    
    /**
     * Add a new line to the player's tag.
     *
     * @param newLine The new line to add
     * @return The new line that has been added
     */
    public TagLine addLine(TagLine newLine) {
    	Preconditions.checkArgument(lines.contains(newLine), "Cannot add an instance of TagLine to a Tag more than once");
    	lines.add(newLine);
    	refreshPairings();
    	return newLine;
    }

    /**
     * Get a line of the player's tag by a specified index.
     *
     * @param index The index of the tag to retrieve
     * @return The TagLine that has been retrieved
     */
    public TagLine getLine(int index) {
    	Preconditions.checkArgument(index >= 0 && index < lines.size(), "Index " + index + " was not found in list of size " + lines.size());
        return lines.get(index);
    }

    /**
     * Clear the player's lines. Removes all lines except the player's default name.
     */
    public void clear() {
        lines.clear();
        refreshPairings();
    }

    /**
     * Get the number of lines a player has.
     *
     * @return The number of lines a player has
     */
    public int getNumLines() {
        return lines.size();
    }

    /**
     * Remove a line from this tag object.
     *
     * @param line The TagLine to remove from the tag
     */
    public void removeLine(TagLine line) {
        removeLine(lines.indexOf(line));
        refreshPairings();
    }

    /**
     * Remove a line from this tag object based on its index.
     *
     * @param index The index of the TagLine that should be removed
     */
    public void removeLine(int index) {
    	Preconditions.checkArgument(index >= 0 && index < lines.size(), "Index " + index + " was not found in list of size " + lines.size());
        lines.remove(index).remove();
    }

    /**
     * Get an array of entity IDs that the stack is comprised of.
     * 
     * @return An array of entity IDs
     */
    public int[] getEntityIds() {
        List<Entity> stack = new ArrayList<>();
        stack.add(whoOwns);
        stack.addAll(baseEntities);
        for (TagLine line : lines) {
            stack.add(line.getLineEntity());
            stack.addAll(line.getSpaceEntities());
        }
        int[] ints = new int[stack.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = stack.get(i).getEntityId();
        }
        return ints;
    }

    /** 
     * Get a 2D integer array that represents the pairings map. Only contains entity IDs
     * <br> Index 0 is the list of vehicles
     * <br> Index 1 is the list of passengers
     * 
     * @return the entity ID pairing maps
     */
    public int[][] getEntityPairings() {
        int[] keys = new int[pairings.size()];
        int[] vals = new int[pairings.size()];
        List<Map.Entry<Entity, Entity>> entries = new ArrayList<>();
        entries.addAll(pairings.entrySet());
        for (int i = 0; i < keys.length; i++) {
            keys[i] = entries.get(i).getKey().getEntityId();
            vals[i] = entries.get(i).getValue().getEntityId();
        }
        return new int[][]{keys, vals};
    }

    //Generate the base of the tag. Currently is just a silverfish for spacing.
    private void genBase() {
        baseEntities.add(createGenericEntity(EntityType.SILVERFISH));
    }

    //Refresh the entity pairings so they can be resent
    public void refreshPairings() {
        //Clear the current pairings map
        pairings.clear();
        //Clear the current stack so it can be regenerated.
        stack.clear();
        //Add the player who owns the tag at the bottom of the stack.
        stack.add(whoOwns);
        //Add all of the baseEntities to the stack, after the Player.
        stack.addAll(baseEntities);
        //Reverse the order of the lines, so they are added in the correct order.
        List<TagLine> lines = Lists.newArrayList(this.lines);
        Collections.reverse(lines);
        //For each line the tag contains,
        for (TagLine line : lines) {
            if (line.getText() != null) {
                //Add the text if the text message is not null.
                stack.add(line.getLineEntity());
            }
            if (line.keepSpaceWhenNull() || line.getText() != null) {
                //If the line is not null, or the line is set to always keep spacing, add the space entities.
                stack.addAll(line.getSpaceEntities());
            }
        }
        //Add the line entity for the name.
        stack.add(name.getLineEntity());
        //For each entity in the stack; add it and the one following it to the pairings map.
        for (int i = 0; i < stack.size(); i++) {
            if (i + 1 < stack.size()) {
                pairings.put(stack.get(i), stack.get(i + 1));
            }
        }
    }

    //Method to create a generic LivingEntity with the given entity type.
    LivingEntity createGenericEntity(EntityType type) {
        //Create the new entity by spawning it at the entity location.
        LivingEntity e = (LivingEntity) entityLoc.getWorld().spawnEntity(entityLoc, type);
        //Add an invisibility potion effect
        e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false));
        //Set AI to false so if it escapes (somehow), it will not be able to do anything.
        e.setAI(false);
        //Prevent the entity from picking up items. Not sure if this is really necessary, but better safe than sorry.
        e.setCanPickupItems(false);
        //Disable collisions for the entity.
        e.setCollidable(false);
        //Disable gravity for the entity, so they do not fall into the void when held at y=-10.
        e.setGravity(false);
        //Make the entity invulnerable
        e.setInvulnerable(true);
        //Make the entity silent to prevent silverfish sounds
        e.setSilent(true);
        //Add a STACK_ENTITY metadata value so the entity will have damage or death cancelled by the EventListener.
        e.setMetadata("STACK_ENTITY", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("MultiLineAPI"),
                null));
        return e;
    }

    //Create a slime to go down in the entity stack. Used for generating spaces.
    LivingEntity createSlime() {
        //Create a new slime through createGenericEntity and cast it to slime
        Slime s = (Slime) createGenericEntity(EntityType.SLIME);
        //Set slime size to -1
        s.setSize(-1);
        return s;
    }

    //Create an armor stand to show the text on.
    LivingEntity createArmorStand() {
        //Create a new armor stand through createGenericEntity and cast it to ArmorStand. Yes, for some reason armor
        // stands are LivingEntities and can hold potion effects. Why? I have no idea...
        ArmorStand as = (ArmorStand) createGenericEntity(EntityType.ARMOR_STAND);
        //Set the armor stand so it is a marker and does not have a hitbox.
        as.setMarker(true);
        //Make the armor stand invisible, since the invisibility potion effect doesn't apply to it.
        as.setVisible(false);
        //Make the custom name visible so it can be used to display text.
        as.setCustomNameVisible(true);
        return as;
    }

    //Create a space and return it. Used by TagLine objects to generate a space.
    List<Entity> createSpace() {
        //Create a new array list to store the entities in.
        List<Entity> space = new ArrayList<>();
        //Add a slime
        space.add(createSlime());
        //Add two silverfishes. This is the proper amount of spacing to create a decent-sized gap.
        space.add(createGenericEntity(EntityType.SILVERFISH));
        space.add(createGenericEntity(EntityType.SILVERFISH));
        return space;
    }

    //Update the location the entities should be at, so players within the view distance will still have them loaded.
    public void updateEntityLoc() {
        //Get the owning player's location
        Location l = whoOwns.getLocation();
        //Set y-level to -10.
        l.setY(-10.0D);
        //Update the variable of this class
        this.entityLoc = l;

        //For each base entity, teleport it to the new location.
        for (Entity e : baseEntities) {
            e.teleport(entityLoc);
        }
        //For each tag line, teleport it's entities to the new location.
        for (TagLine t : lines) {
            t.teleport(entityLoc);
        }
    }

    /**
     * Get the owner of the Tag
     * 
     * @return The owner of the Tag
     */
    public Player getOwner() {
        return whoOwns;
    }

    /**
     * Remove the tag from the player. This includes removal of the name, lines and
     * the base entities. Clears all localized data
     */
    public void remove() {
        name.remove();
        lines.forEach(TagLine::remove);
        baseEntities.forEach(Entity::remove);
    }

    /**
     * Refresh all entity data and entity pairings on this tag
     */
    public void refresh() {
        name.tempDisable();
        lines.forEach(TagLine::tempDisable);
        baseEntities.forEach(Entity::remove);

        baseEntities.clear();
        stack.clear();
        pairings.clear();
        lines.clear();

        updateEntityLoc();

        name.reEnable();
        lines.forEach(TagLine::reEnable);
        genBase();

        refreshPairings();
    }
}