package net.blitzcube.score.secondlineapi.manager;

import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Created by iso2013 on 12/19/2016.
 */
public class Stack {
    private Plugin parent;
    private ArrayList<Entity> entities;
    private ArrayList<Integer> entityIds;

    private ArmorStand firstLine;
    private ArmorStand secondLine;

    private String line;
    private String name;

    private Player p;

    public Stack(Plugin parent, Player p, String secondLine){
        this.parent = parent;
        entities = new ArrayList<>();
        entityIds = new ArrayList<>();
        this.p = p;
        this.line = secondLine;
        this.name = p.getName();

        createStack();
        mountStack();
        updateLines();
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line){
        this.line = line;
        updateLines();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLines();
    }

    public void dispose(){
        entities.forEach(Entity::remove);
    }

    public boolean hasEntity(int id){
        return entityIds.contains(id);
    }

    private void updateLines(){
        if(line == null){
            firstLine.setCustomName("");
            secondLine.setCustomName(name);

            firstLine.setCustomNameVisible(false);
            secondLine.setCustomNameVisible(true);
        } else {
            firstLine.setCustomName(name);
            secondLine.setCustomName(line);

            firstLine.setCustomNameVisible(true);
            secondLine.setCustomNameVisible(true);
        }
    }

    private void createStack(){
        firstLine = createArmorStand();
        secondLine = createArmorStand();

        entities.add(createGapSilverfish());
        entities.add(secondLine);
        entities.add(createGapSlime());
        entities.add(createGapSilverfish());
        entities.add(createGapSilverfish());
        entities.add(firstLine);

        entityIds.addAll(entities.stream().map(Entity::getEntityId).collect(Collectors.toList()));
    }

    private void mountStack(){
        for(int i = 0; i < entities.size(); i++){
            if(i + 1 < entities.size()){
                entities.get(i).setPassenger(entities.get(i + 1));
            }
        }
        updateLoc();
    }

    private Silverfish createGapSilverfish(){
        Silverfish sf = (Silverfish) p.getWorld().spawnEntity(p.getLocation(), EntityType.SILVERFISH);
        sf.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false));
        sf.setAI(false);
        sf.setSilent(true);
        sf.setGravity(false);
        sf.setInvulnerable(true);
        sf.setCanPickupItems(false);
        sf.setCollidable(false);
        sf.setMetadata("STACK_ENTITY", new FixedMetadataValue(parent, null));
        return sf;
    }

    private Slime createGapSlime(){
        Slime s = (Slime) p.getWorld().spawnEntity(p.getLocation(), EntityType.SLIME);
        s.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false));
        s.setAI(false);
        s.setSilent(true);
        s.setGravity(false);
        s.setInvulnerable(true);
        s.setCanPickupItems(false);
        s.setCollidable(false);
        s.setMetadata("STACK_ENTITY", new FixedMetadataValue(parent, null));
        s.setSize(-1);
        return s;
    }

    private ArmorStand createArmorStand(){
        ArmorStand s = (ArmorStand) p.getWorld().spawnEntity(p.getLocation(), EntityType.ARMOR_STAND);
        s.setVisible(false);
        s.setMarker(true);
        s.setCollidable(false);
        s.setInvulnerable(true);
        s.setMetadata("STACK_ENTITY", new FixedMetadataValue(parent, null));
        return s;
    }

    void updateLoc() {
        entities.get(0).teleport(p.getLocation().subtract(0, p.getLocation().getY() + 10, 0));
    }
}
