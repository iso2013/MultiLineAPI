package net.blitzcube.score.secondlineapi.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Optional;
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
            stacks.put(p.getUniqueId(), new Stack(parent, p, defaultMessage, protocol));
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
        //Bukkit.getScheduler().runTaskLater(parent, () -> stacks.put(e.getPlayer().getUniqueId(), new Stack(parent, e
        //        .getPlayer(), defaultMessage, protocol)), 1L);
        Bukkit.getScheduler().runTaskLater(parent, () -> stacks.values().stream().filter(s -> Bukkit.getPlayer(s
                .getOwner()).getWorld().getUID().equals(e.getPlayer()
                .getWorld().getUID())).forEach(s -> {
            s.createPairings(e.getPlayer());
        }), 1L);
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
        if (stacks.containsKey(e.getPlayer().getUniqueId())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLocs();
        }
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        if (stacks.containsKey(e.getPlayer().getUniqueId())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLocs();
            stacks.get(e.getPlayer().getUniqueId()).hideFromPlayer();
        }
        Bukkit.getScheduler().runTaskLater(parent, () -> stacks.values().stream().filter(s -> Bukkit.getPlayer(s
                .getOwner()).getWorld().getUID().equals(e.getPlayer()
                .getWorld().getUID())).forEach(s -> {
            s.createPairings(e.getPlayer());
        }), 1L);
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        if (stacks.containsKey(e.getPlayer().getUniqueId())) {
            stacks.get(e.getPlayer().getUniqueId()).updateLocs();
            stacks.get(e.getPlayer().getUniqueId()).hideFromPlayer();
        }
        Bukkit.getScheduler().runTaskLater(parent, () -> stacks.values().stream().filter(s -> Bukkit.getPlayer(s
                .getOwner()).getWorld().getUID().equals(e.getPlayer()
                .getWorld().getUID())).forEach(s -> {
            s.createPairings(e.getPlayer());
        }), 1L);
    }

    public void dispose() {
        stacks.values().forEach(Stack::dispose);
        stacks.clear();
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        if (packet.getType().equals(PacketType.Play.Server.NAMED_ENTITY_SPAWN)) {
            UUID sending = packet.getUUIDs().read(0);
            Bukkit.getScheduler().runTaskLater(parent, () -> {
                Stack s;
                if ((s = stacks.get(sending)) != null) {
                    s.createPairings(packetEvent.getPlayer());
                }
            }, 2L);
        } else if (packet.getType().equals(PacketType.Play.Server.MOUNT)) {
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
                    Stack s;
                    if ((s = stacks.get(p.get().getUniqueId())) != null) {
                        packetEvent.setCancelled(true);
                        s.createPairings(packetEvent.getPlayer());
                    }
                }
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {}

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
        return parent;
    }
}
