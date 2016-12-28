package net.blitzcube.mlapi.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class PacketHandler implements com.comphenix.protocol.events.PacketListener {
    //The instance of the API to access players' Tag objects
    private MultiLineAPI inst;
    //The protocol manager for ProtocolLib
    private ProtocolManager protocol;

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
        //If the packet is a NAMED_ENTITY_SPAWN packet (the packet used for sending a player to another player's
        // client, then mount the entities for that player.
        if (packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            //Get the UUID of the player that is being sent, so we can use it to retrieve their Tag object.
            UUID sending = packet.getUUIDs().read(0);
            //Delay the code 2 ticks to prevent the packets from sending before the player has received the entities
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                //Get the player's Tag object, and check if it is not equal to null.
                Tag t;
                if ((t = inst.tags.get(sending)) != null) {
                    //If they do have a tag, send the mount packets to the player.
                    inst.createPairs(t, packetEvent.getPlayer());
                }
            }, 2L);
        } else {
            //Note: This code is used to remount the player's tag if the player has a different entity on their head
            // and it is unmounted. It has barely been tested due to a bug in #setPassenger().
            //Retrieve the new list of passengers from the packet
            int[] passengers = packet.getIntegerArrays().read(0);
            //Retrieve the EID of the vehicle from the packet
            int entity = packet.getIntegers().read(0);
            //If the player it's being sent to is the vehicle, ignore the event.
            if (packetEvent.getPlayer().getEntityId() == entity) {
                return;
            }
            //Only resend pairs if the player is having it's passengers removed
            if (passengers.length == 0) {
                //Get a player who has the same entity ID as the vehicle in the packet, and has the same world as the
                // world the player who receives the packet is in.
                Optional<? extends Player> p = Bukkit.getOnlinePlayers().stream()
                        .filter(ps -> ps.getEntityId() == entity)
                        .filter(ps -> ps.getWorld().getUID().equals(packetEvent.getPlayer().getWorld().getUID()))
                        .findAny();
                //If a player matches the criteria, then attempt to resend the mount packets.
                if (p.isPresent()) {
                    //Check if the player has a tag.
                    Tag t;
                    if ((t = inst.tags.get(p.get().getUniqueId())) != null) {
                        //If they do have a tag, cancel the packet and resend the proper mount packets.
                        packetEvent.setCancelled(true);
                        inst.createPairs(t, packetEvent.getPlayer());
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
                PacketType.Play.Server.MOUNT
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
            //Send the packet to the palyer.
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            //Record in the logs if a packet fails to send.
            inst.getLogger().info("Failed to send hide packet to " + p.getName() + "!");
        }
    }
}
