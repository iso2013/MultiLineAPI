package net.blitzcube.mlapi.packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;

import org.bukkit.plugin.Plugin;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.handler.CreateHandler;
import net.blitzcube.mlapi.packet.handler.DeleteHandler;
import net.blitzcube.mlapi.packet.handler.ReAimHandler;
import net.blitzcube.mlapi.packet.handler.RemountHandler;
import net.blitzcube.mlapi.packet.handler.VisibilityHandler;

/**
 * Created by iso2013 on 7/28/2017.
 */
public abstract class PacketHandler {

    public static final PacketHandler CREATE_HANDLER = new CreateHandler();
    public static final PacketHandler DELETE_HANDLER = new DeleteHandler();
    public static final PacketHandler REMOUNT_HANDLER = new RemountHandler();
    public static final PacketHandler VISIBILITY_HANDLER = new VisibilityHandler();
    public static final PacketHandler RE_AIM_HANDLER = new ReAimHandler();

    private static PacketHandler[] values = new PacketHandler[] {
            CREATE_HANDLER, DELETE_HANDLER, REMOUNT_HANDLER, VISIBILITY_HANDLER, RE_AIM_HANDLER
    };

    public List<PacketType> getPackets() {
        return new ArrayList<>();
    }

    public abstract void handle(MultiLineAPI plugin, PacketEvent event, TagSender tags);

    public static class ProtocolHandler implements PacketListener {

        private Map<PacketType, PacketHandler> handlers = new HashMap<>();
        private ListeningWhitelist packetList;
        private MultiLineAPI plugin;
        private TagSender tags;

        public ProtocolHandler(ProtocolManager manager, MultiLineAPI plugin) {
            ListeningWhitelist.Builder packets = ListeningWhitelist.newBuilder();

            for (net.blitzcube.mlapi.packet.PacketHandler v : values) {
                packets = packets.types(v.getPackets());

                for (PacketType t : v.getPackets()) {
                    this.handlers.put(t, v);
                }
            }

            this.packetList = packets.gamePhase(GamePhase.PLAYING).high().build();
            this.plugin = plugin;
            this.tags = new TagSender(manager);
            manager.addPacketListener(this);
        }

        @Override
        public void onPacketSending(PacketEvent e) {
            if (!handlers.containsKey(e.getPacketType())) return;
            this.handlers.get(e.getPacketType()).handle(plugin, e, tags);
        }

        @Override
        public void onPacketReceiving(PacketEvent e) {
            if (!handlers.containsKey(e.getPacketType())) return;
            this.handlers.get(e.getPacketType()).handle(plugin, e, tags);
        }

        @Override
        public ListeningWhitelist getSendingWhitelist() {
            return packetList;
        }

        @Override
        public ListeningWhitelist getReceivingWhitelist() {
            return packetList;
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }

    }

}
