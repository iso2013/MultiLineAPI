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
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by iso2013 on 12/22/2016.
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
        }
    }

    private void spawnPlayer(UUID who, Player forWho) {
        //Check if the player can see the player that's supposed to spawn. (This if statement may not be necessary,
        // because this method is only fired in NAMED_ENTITY_SPAWN which doesn't get sent if the player is invisible,
        // but oh well.)
        if (!inst.vnsh.isVanished(Bukkit.getPlayer(who), forWho)) {
            //And if they can, create the entity pairings 2 ticks later to ensure the entities have spawned.
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                Tag t;
                if ((t = inst.tags.get(who)) != null) {
                    inst.createPairs(t, forWho);
                }
            }, 2L);
        }
    }

    private void resendMount(PacketEvent packetEvent) {
        //This code is designed to remount the entities if another plugin sends an empty mount packet. Due to a bug
        // in #setPassenger(), it has not been tested extensively.
        PacketContainer packet = packetEvent.getPacket();
        int[] passengers = packet.getIntegerArrays().read(0);
        int entity = packet.getIntegers().read(0);
        if (packetEvent.getPlayer().getEntityId() == entity) {
            return;
        }
        if (passengers.length == 0) {
            Optional<? extends Player> p = Bukkit.getOnlinePlayers().stream()
                    .filter(ps -> ps.getEntityId() == entity)
                    .filter(ps -> ps.getWorld().getUID().equals(packetEvent.getPlayer().getWorld().getUID()))
                    .findAny();
            if (p.isPresent()) {
                Tag t;
                if ((t = inst.tags.get(p.get().getUniqueId())) != null) {
                    packetEvent.setCancelled(true);
                    inst.createPairs(t, packetEvent.getPlayer());
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
        p.sendMessage(Arrays.toString(is));
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
