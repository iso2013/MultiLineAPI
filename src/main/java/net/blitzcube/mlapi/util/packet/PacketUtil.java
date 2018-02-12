package net.blitzcube.mlapi.util.packet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.blitzcube.mlapi.util.packet.handler.PacketGoal;
import net.blitzcube.mlapi.util.packet.handler.PacketHandler;

/**
 * Created by iso2013 on 8/23/2017.
 */
public class PacketUtil {

    private static final Map<PacketType, ConcurrentSkipListSet<PacketGoal>> HANDLERS = new HashMap<>();
    private static Field entityID;
    private static Method getISNMSCopy;

    private static ProtocolManager manager;
    private static JavaPlugin plugin;
    private static PacketUtilImpl listener;
    private static int lastInt = Integer.MIN_VALUE;

    public static boolean init(ProtocolManager manager, JavaPlugin plugin, boolean itemSupport) {
        PacketUtil.manager = manager;
        PacketUtil.plugin = plugin;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);
            entityID = Class.forName("net.minecraft.server." + version + ".Entity").getDeclaredField("entityCount");

            if (!entityID.isAccessible()) entityID.setAccessible(true);
            if (itemSupport) {
                getISNMSCopy = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack")
                        .getDeclaredMethod("asNMSCopy", ItemStack.class);
            }

            return true;
        } catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
            entityID = null;
            getISNMSCopy = null;
            plugin.getLogger().severe("Failed to initialize PacketUtil! Error given:");
            e.printStackTrace();
        }

        return false;
    }

    public static void registerPacketEvents(Object handler, JavaPlugin parent) {
        for (Method m : handler.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != PacketEvent.class) continue;

            for (Annotation a : m.getAnnotations()) {
                if (a instanceof PacketHandler) {
                    PacketHandler ph = (PacketHandler) a;
                    HANDLERS.computeIfAbsent(PacketType.findCurrent(ph.protocol(), ph.sender(), ph.name()),
                            input -> new ConcurrentSkipListSet<>(Comparator.comparingInt(PacketGoal::getPriority))
                    ).add(new PacketGoal(parent, handler, m, ((PacketHandler) a).priority()));
                }
            }
        }
    }

    public static void registerListener() {
        if (listener == null) {
            manager.addPacketListener(listener = new PacketUtilImpl(plugin));
        }
        else {
            manager.removePacketListener(listener);
            listener = new PacketUtilImpl(plugin);
            manager.addPacketListener(listener);
        }
    }

    public static FakeEntityFactory getFakeEntityFactory() {
        return new FakeEntityFactory();
    }

    public static void updateWatcher(WrappedDataWatcher metadata, Map<Integer, Object> pendingChanges) {
        for (Map.Entry<Integer, Object> changeEntry : pendingChanges.entrySet()) {
            if (changeEntry.getValue().getClass() == org.bukkit.inventory.ItemStack.class) {
                if (getISNMSCopy == null)
                    throw new IllegalArgumentException("PacketUtil was not enabled with item support.");

                try {
                    metadata.setObject(changeEntry.getKey(), WrappedDataWatcher.Registry.getItemStackSerializer(false),
                            getISNMSCopy.invoke(null, changeEntry.getValue()));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                continue;
            }
            metadata.setObject(changeEntry.getKey(), WrappedDataWatcher.Registry.get(changeEntry.getValue().getClass()), changeEntry.getValue());
        }

        pendingChanges.clear();
    }

    public static int getNextEntityID() {
        boolean regen = false;

        try {
            if (lastInt == Integer.MIN_VALUE) {
                int id = entityID.getInt(null);
                entityID.setInt(null, id + 1);
                return id;
            }
        } catch (IllegalAccessException e) {
            plugin.getLogger().severe("Failed to access the NMS field. Error is printed below. We will now revert to " +
                    "integer incrementation, this may conflict with other plugins. (%0.0000119209318 chance of " +
                    "conflict.)");
            e.printStackTrace();
            regen = true;
        }

        if (regen || lastInt >= 0) lastInt = (ThreadLocalRandom.current().nextInt(8388606) * -256) - 256;
        return ++lastInt;
    }

    public static void sendFriendly(PacketContainer packet, Player[] forWho) {
        for (Player p : forWho) {
            if (Bukkit.getPlayer(p.getUniqueId()) == null) continue;

            try {
                manager.sendServerPacket(p, packet);
            } catch (InvocationTargetException e) {
                plugin.getLogger().warning("Failed to send packet of type " + packet.getType().name() + " to player " + p.getName());
                e.printStackTrace();
            }
        }
    }

    private static class PacketUtilImpl implements PacketListener {

        private ListeningWhitelist allPackets;
        private JavaPlugin parent;

        public PacketUtilImpl(JavaPlugin plugin) {
            this.parent = plugin;
            this.allPackets = ListeningWhitelist.newBuilder()
                    .normal()
                    .gamePhase(GamePhase.BOTH)
                    .types(HANDLERS.keySet())
                    .build();
        }

        @Override
        public void onPacketSending(PacketEvent e) {
            if (!HANDLERS.containsKey(e.getPacketType())) return;
            HANDLERS.get(e.getPacketType()).forEach(packetGoal -> packetGoal.invoke(e));
        }

        @Override
        public void onPacketReceiving(PacketEvent e) {
            if (!HANDLERS.containsKey(e.getPacketType())) return;
            HANDLERS.get(e.getPacketType()).forEach(packetGoal -> packetGoal.invoke(e));
        }

        @Override
        public ListeningWhitelist getSendingWhitelist() {
            return allPackets;
        }

        @Override
        public ListeningWhitelist getReceivingWhitelist() {
            return allPackets;
        }

        @Override
        public org.bukkit.plugin.Plugin getPlugin() {
            return parent;
        }

    }

}
