package net.blitzcube.score.secondlineapi.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by iso2013 on 12/19/2016.
 */
public class Stack {
    private Plugin parent;
    private ArrayList<Entity> entities;
    private HashMap<Entity, Entity> pairings;

    private ArmorStand firstLine;
    private ArmorStand secondLine;

    private String line;
    private String name;

    private Player p;

    private ProtocolManager protocol;

    public Stack(Plugin parent, Player p, String secondLine, ProtocolManager protocol) {
        this.parent = parent;
        entities = new ArrayList<>();
        this.p = p;
        this.line = secondLine;
        this.name = p.getName();

        this.protocol = protocol;

        createStack();
        mountStack();
        updateLines();
        hideFromPlayer();
        updateLocs();
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
        for (Entity e : entities) {
            if (id == e.getEntityId()) return true;
        }
        return false;
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

    void hideFromPlayer() {
        int[] ints = new int[entities.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = entities.get(i).getEntityId();
        }
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntegerArrays().write(0, ints);
        try {
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void createPairings(Player dest) {
        for (Map.Entry<Entity, Entity> pair : pairings.entrySet()) {
            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.MOUNT);
            packet.getIntegers().write(0, pair.getKey().getEntityId());
            packet.getIntegerArrays().write(0, new int[]{pair.getValue().getEntityId()});
            try {
                protocol.sendServerPacket(dest, packet);
            } catch (InvocationTargetException e) {
                parent.getLogger().info("Failed to send mount packet to " + dest.getName() + "!");
            }
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
    }

    private void mountStack(){
        pairings = new HashMap<>();
        for(int i = 0; i < entities.size(); i++){
            if(i + 1 < entities.size()){
                pairings.put(entities.get(i), entities.get(i + 1));
            }
        }
        pairings.put(p, entities.get(0));
    }

    private Silverfish createGapSilverfish(){
        Silverfish sf = (Silverfish) p.getWorld().spawnEntity(p.getLocation().subtract(0, p.getLocation().getY() +
                10, 0), EntityType.SILVERFISH);
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
        Slime s = (Slime) p.getWorld().spawnEntity(p.getLocation().subtract(0, p.getLocation().getY() + 10, 0), EntityType.SLIME);
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
        ArmorStand s = (ArmorStand) p.getWorld().spawnEntity(p.getLocation().subtract(0, p.getLocation().getY() + 10, 0), EntityType.ARMOR_STAND);
        s.setVisible(false);
        s.setMarker(true);
        s.setCollidable(false);
        s.setInvulnerable(true);
        s.setGravity(false);
        s.setMetadata("STACK_ENTITY", new FixedMetadataValue(parent, null));
        return s;
    }

    void updateLocs() {
        for (Entity e : entities) {
            e.teleport(p.getLocation().subtract(0, p.getLocation().getY() + 10, 0));
        }
    }

    public UUID getOwner() {
        return p.getUniqueId();
    }
}
