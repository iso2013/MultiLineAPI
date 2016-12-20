package net.blitzcube.score.secondlineapi.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by iso2013 on 12/19/2016.
 */
public class SecondLineManager implements Listener, PacketListener {
    private static SecondLineManager instance;
    private Plugin parent;
    private ProtocolManager protocol;
    private HashMap<UUID, Stack> stacks;
    private String defaultMessage;

    public SecondLineManager(Plugin parent) {
        if (instance != null) throw new IllegalArgumentException("API already initialized!");
        instance = this;

        this.parent = parent;
        protocol = ProtocolLibrary.getProtocolManager();
        protocol.addPacketListener(this);

        stacks = new HashMap<>();

        parent.getServer().getPluginManager().registerEvents(this, parent);
    }

    public static SecondLineManager getInstance(Plugin parent) {
        if (instance == null) {
            new SecondLineManager(parent);
        }
        return instance;
    }

    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public void clear(Player p) {
        stacks.remove(p.getUniqueId()).dispose();
    }

    public void add(Player p) {
        if (!stacks.containsKey(p.getUniqueId())) {
            stacks.put(p.getUniqueId(), new Stack(parent, p, defaultMessage));
        }
    }

    public boolean isEnabled(Player p) {
        return stacks.containsKey(p.getUniqueId());
    }

    public void setLine(Player p, String line) {
        add(p);
        stacks.get(p.getUniqueId()).setLine(line);
    }

    public void setName(Player p, String tag) {
        add(p);
        stacks.get(p.getUniqueId()).setName(tag);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        stacks.put(e.getPlayer().getUniqueId(), new Stack(parent, e.getPlayer(), defaultMessage));
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        stacks.remove(e.getPlayer().getUniqueId()).dispose();
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (e.getEntity().hasMetadata("STACK_ENTITY")) e.setCancelled(true);
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (isEnabled(e.getPlayer())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLoc();
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (isEnabled(e.getPlayer())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLoc();
        }
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (isEnabled(e.getPlayer())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLoc();
        }
    }

    public void dispose() {
        stacks.values().forEach(Stack::dispose);
        stacks.clear();
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        PacketType type = packetEvent.getPacketType();
        Stack stack;
        if ((stack = stacks.get(packetEvent.getPlayer().getUniqueId())) != null) {
            if (type.equals(PacketType.Play.Server.SPAWN_ENTITY)) {
                packetEvent.setCancelled(stack.hasEntity(packet.getIntegers().read(0)));
            } else if (type.equals(PacketType.Play.Server.MOUNT)) {
                packetEvent.setCancelled(stack.hasEntity(packet.getIntegers().read(0)) || packet.getIntegers().read
                        (0) == packetEvent.getPlayer().getEntityId());
            } else if (type.equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
                packetEvent.getPlayer().sendMessage("Player spawned!");
            } else {
                packetEvent.setCancelled(stack.hasEntity(packet.getIntegers().read(0)) || packet.getIntegers().read
                        (0) == packetEvent.getPlayer().getEntityId());
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {}

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().normal().gamePhase(GamePhase.PLAYING).types(
                PacketType.Play.Server.MOUNT,
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.ENTITY_HEAD_ROTATION,
                PacketType.Play.Server.ENTITY_LOOK,
                PacketType.Play.Server.ENTITY_TELEPORT,
                PacketType.Play.Server.REL_ENTITY_MOVE,
                PacketType.Play.Server.REL_ENTITY_MOVE_LOOK
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return parent;
    }
}
