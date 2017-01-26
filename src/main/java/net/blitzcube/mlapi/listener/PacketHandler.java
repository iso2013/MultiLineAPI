package net.blitzcube.mlapi.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.collect.Lists;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class PacketHandler implements com.comphenix.protocol.events.PacketListener {
    //The instance of the API to access players' Tag objects
    private final MultiLineAPI inst;
    //The protocol manager for ProtocolLib
    private final ProtocolManager protocol;

    //Constructor accepts the API instance and retrieves the protocol manager from ProtocolLib.
    public PacketHandler(MultiLineAPI secondLineAPI) {
        this.inst = secondLineAPI;
        protocol = ProtocolLibrary.getProtocolManager();
        protocol.addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        //Pull the PacketContainer object out of the event for easier
        PacketContainer packet = packetEvent.getPacket();
        if (packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            spawnPlayer(packet.getUUIDs().read(0), packetEvent.getPlayer());
        } else if (packet.getType().equals(PacketType.Play.Server.MOUNT)) {
            resendMount(packetEvent);
        } else if (packet.getType().equals(PacketType.Play.Server.ENTITY_EFFECT)) {
            handleAddEffect(packetEvent);
        } else if (packet.getType().equals(PacketType.Play.Server.REMOVE_ENTITY_EFFECT)) {
            handleEffect(packetEvent);
        }
    }

    private void handleAddEffect(PacketEvent event) {
        PacketContainer p = event.getPacket();
        int entityId = p.getIntegers().read(0);
        int effectId = p.getBytes().read(0);
        if (effectId != PotionEffectType.INVISIBILITY.getId()) return;
        Optional<? extends Player> oP = Bukkit.getOnlinePlayers().stream()
                .filter(ps -> ps.getEntityId() == entityId)
                .filter(ps -> ps.getWorld().getUID().equals(event.getPlayer().getWorld().getUID())).findAny();
        if (!oP.isPresent()) return;
        Bukkit.getScheduler().runTask(inst, () -> inst.tags.get(oP.get().getUniqueId()).tempDisable());
    }

    private void handleEffect(PacketEvent event) {
        PacketContainer p = event.getPacket();
        PotionEffectType effectId = p.getEffectTypes().read(0);
        if (!effectId.equals(PotionEffectType.INVISIBILITY)) return;
        inst.tags.get(event.getPlayer().getUniqueId()).reEnable();
        MultiLineAPI.refresh(event.getPlayer());
    }

    private void spawnPlayer(UUID who, Player forWho) {
        //Check if the player can see the player that's supposed to spawn. (This if statement may not be necessary,
        // because this method is only fired in NAMED_ENTITY_SPAWN which doesn't get sent if the player is invisible,
        // but oh well.)
        if (forWho.canSee(Bukkit.getPlayer(who))) {
            //And if they can, create the entity pairings 2 ticks later to ensure the entities have spawned.
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                Tag t;
                if ((t = inst.tags.get(who)) != null) {
                    inst.createPairs(t, forWho);
                }
            }, 2L);
        }
    }

    private void despawnPlayer(UUID who, Player forWho) {
        //TODO: Implement player de-spawning
    }

    private void resendMount(PacketEvent packetEvent) {
        //This code is designed to remount the entities if another plugin sends an empty mount packet. Due to a bug
        // in #setPassenger(), it has not been tested extensively.
        PacketContainer packet = packetEvent.getPacket();
        int[] passengers = packet.getIntegerArrays().read(0);
        int entity = packet.getIntegers().read(0);
        if (passengers.length == 0) {
            if (packetEvent.getPlayer().getEntityId() == entity) {
                return;
            }
            Optional<? extends Player> p = Bukkit.getOnlinePlayers().stream()
                    .filter(ps -> ps.getEntityId() == entity)
                    .filter(ps -> ps.getWorld().getUID().equals(packetEvent.getPlayer().getWorld().getUID()))
                    .findAny();
            if (p.isPresent()) {
                Tag t;
                if ((t = inst.tags.get(p.get().getUniqueId())) != null) {
                    packetEvent.setCancelled(true);
                    Bukkit.getScheduler().runTask(inst, () -> {
                        t.reEnable();
                        inst.createPairs(t, packetEvent.getPlayer());
                    });
                }
            }
        } else {
            Optional<? extends Player> p = Bukkit.getOnlinePlayers().stream()
                    .filter(ps -> ps.getEntityId() == entity)
                    .filter(ps -> ps.getWorld().getUID().equals(packetEvent.getPlayer().getWorld().getUID()))
                    .findAny();
            if (p.isPresent()) {
                Tag t;
                if ((t = inst.tags.get(p.get().getUniqueId())) != null) {
                    List<Integer> entityList = Lists.newArrayList();
                    for (int i : t.getEntities()) entityList.add(i);
                    boolean containsOther = false;
                    for (int i : passengers) {
                        if (!entityList.contains(i)) {
                            containsOther = true;
                            break;
                        }
                    }
                    if (containsOther) {
                        t.tempDisable();
                    }
                }
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        //404: Method onPacketReceiving(PacketEvent packetEvent) not found.
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        //We only need to listen for the sending of player spawn packets and mount packets, so filter those using the
        // ListeningWhitelist.
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.MOUNT,
                PacketType.Play.Server.ENTITY_EFFECT,
                PacketType.Play.Server.REMOVE_ENTITY_EFFECT
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        //We don't need to listen for anything from the client.
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        //Return the API instance - the plugin this listener belongs to
        return inst;
    }

    //This method is used to send entity mount packets in the createPairings method of MultiLineAPI.java.
    public void sendMountPacket(Player p, int key, int value) {
        //Create a new packet container with the MOUNT packet type.
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.MOUNT);
        //Write the vehicle entity ID to the first integer field.
        packet.getIntegers().write(0, key);
        //Write the passenger entity ID in a new empty array list to the first integer list field.
        packet.getIntegerArrays().write(0, new int[]{value});
        try {
            //Send the packet to the player.
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            //Record in the logs if a packet fails to send.
            inst.getLogger().info("Failed to send mount packet to " + p.getName() + "!");
        }
    }

    //This method is used to hide a list of entities from a player by destroying them. This is used instead of
    // cancelling the entity spawn packets due to the dynamic nature of the list of entities in the Tag class.
    public void hide(Player p, int[] is) {
        //Create a new ENTITY_DESTROY packet.
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        //Write all the entity IDs that need to be hidden to the first integer list field.
        packet.getIntegerArrays().write(0, is);
        try {
            //Send the packet to the player.
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            //Record in the logs if a packet fails to send.
            inst.getLogger().info("Failed to send hide packet to " + p.getName() + "!");
        }
    }
}
