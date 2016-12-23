package net.blitzcube.line2.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import net.blitzcube.line2.SecondLineAPI;
import net.blitzcube.line2.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by iso2013 on 12/22/2016.
 */
public class PacketListener implements com.comphenix.protocol.events.PacketListener {
    private SecondLineAPI inst;
    private ProtocolManager protocol;

    public PacketListener(SecondLineAPI secondLineAPI) {
        this.inst = secondLineAPI;
        protocol = ProtocolLibrary.getProtocolManager();
        protocol.addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        if (packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            UUID sending = packet.getUUIDs().read(0);
            Bukkit.getScheduler().runTaskLater(inst, () -> {
                Tag t;
                if ((t = inst.tags.get(sending)) != null) {
                    inst.createPairs(t, packetEvent.getPlayer());
                }
            }, 2L);
        } else {
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
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.MOUNT
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return inst;
    }

    public void sendMountPacket(Player p, int key, int value) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.MOUNT);
        packet.getIntegers().write(0, key);
        packet.getIntegerArrays().write(0, new int[]{value});
        try {
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            inst.getLogger().info("Failed to send mount packet to " + p.getName() + "!");
        }
    }

    public void hide(Player p, int[] is) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntegerArrays().write(0, is);
        try {
            protocol.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            inst.getLogger().info("Failed to send hide packet to " + p.getName() + "!");
        }
    }
}
