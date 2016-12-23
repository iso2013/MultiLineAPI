package net.blitzcube.line2;

import net.blitzcube.line2.listener.EventListener;
import net.blitzcube.line2.listener.PacketListener;
import net.blitzcube.line2.tag.Tag;
import net.blitzcube.line2.tag.TagLine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public final class SecondLineAPI extends JavaPlugin implements Listener {
    private static SecondLineAPI inst;
    public HashMap<UUID, Tag> tags;
    private PacketListener pckt;
    private EventListener evnt;

    public static boolean isAutoEnable() {
        return inst.evnt.autoEnable;
    }

    public static void setAutoEnable(boolean val) {
        inst.evnt.autoEnable = val;
    }

    public static void enable(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId())) {
            inst.tags.put(p.getUniqueId(), new Tag(p));
        }
    }

    public static void disable(Player p) {
        if (inst.tags.containsKey(p.getUniqueId())) {
            inst.tags.remove(p.getUniqueId()).remove();
        }
    }

    public static void disable() {
        inst.tags.values().forEach(Tag::remove);
        inst.tags.clear();
    }

    public static boolean isEnabled(Player p) {
        return inst.tags.containsKey(p.getUniqueId());
    }

    public static TagLine getName(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getName();
    }

    public static TagLine getLine(Player p, int lineIndex) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getLine(lineIndex);
    }

    public static TagLine addLine(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).addLine();
    }

    public static void removeLine(Player p, int lineIndex) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        inst.tags.get(p.getUniqueId()).removeLine(lineIndex);
    }

    public static void removeLine(Player p, TagLine line) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        inst.tags.get(p.getUniqueId()).removeLine(line);
    }

    public static int getLineCount(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        return inst.tags.get(p.getUniqueId()).getNumLines();
    }

    public static void refresh(Player p) {
        if (!inst.tags.containsKey(p.getUniqueId()))
            throw new IllegalArgumentException("Player does not have API enabled!");
        inst.refreshForEveryone(p);
    }

    public static void refreshOthers(Player p) {
        inst.refreshView(p);
    }

    public static void clearLines(Player p) {
        inst.tags.clear();
    }

    public static void updateLocs(Player p) {
        inst.tags.get(p.getUniqueId()).updateEntityLoc();
    }

    @Override
    public void onEnable() {
        tags = new HashMap<>();
        SecondLineAPI.inst = this;
        evnt = new EventListener(this);
        this.getServer().getPluginManager().registerEvents(evnt, this);
        pckt = new PacketListener(this);

        setAutoEnable(true);
    }

    @Override
    public void onDisable() {
        disable();
    }

    private void refreshView(Player p) {
        tags.values().stream().filter(s -> Bukkit.getPlayer(s
                .getOwner()).getWorld().getUID().equals(p
                .getWorld().getUID())).forEach(s -> {
            createPairs(s, p);
        });
    }

    public void createPairs(Tag t, Player p) {
        t.refreshPairings();
        int[] keys = t.getEntityPairings()[0];
        int[] values = t.getEntityPairings()[1];
        for (int i = 0; i < keys.length; i++) {
            pckt.sendMountPacket(p, keys[i], values[i]);
        }
    }

    private void refreshForEveryone(Player p) {
        Bukkit.getOnlinePlayers().stream().filter(o -> o.getWorld().getUID().equals(p.getWorld().getUID())).forEach(o
                -> {
            createPairs(tags.get(p.getUniqueId()), o);
        });
    }

    public void hide(Player p) {
        pckt.hide(p, inst.tags.get(p.getUniqueId()).getEntityIds());
    }
}
