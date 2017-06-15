package net.blitzcube.mlapi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.blitzcube.mlapi.example.ExampleListener;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public final class MultiLineAPI extends JavaPlugin {
    public static final Map<UUID, Tag> tags = Maps.newHashMap();

    public static boolean isAutoEnablePlayer() {
        return false;
    }

    public static void setAutoEnablePlayer(boolean val) {

    }

    public static boolean isAutoDisablePlayer() {
        return false;
    }

    public static void setAutoDisablePlayer(boolean val) {

    }

    public static void enable(Entity e) {

    }

    public static boolean isEnabled(Entity e) {
        return false;
    }

    public static void disable(Entity e) {

    }

    public static void disable() {

    }

    public static void show(Entity e) {

    }

    public static void hide(Entity e) {

    }

    public static void addTagController(Entity e, TagController tg) {

    }

    public static void removeTagController(Entity e, TagController tg) {

    }

    public static List<TagController> getTagControllers(Entity e) {
        return Lists.newLinkedList();
    }

    public static void refreshText(Entity e, Player... forWho) {

    }

    public static void refreshText(Entity e, TagController controller, Player... forWho) {

    }

    public static void refreshText(Entity e, TagLine line, Player... forWho) {

    }

    public static void refreshLayout(Entity e, Player... forWho) {

    }

    public static void showFor(Entity e, Player... forWho) {

    }

    public static void hideFor(Entity e, Player... forWho) {

    }

    public static void hideAllFor(Player... forWho) {

    }

    public static void showAllFor(boolean bypassGameMode, Player... forWho) {

    }

    @Override
    public void onEnable() {
        EntityUtil.init();
        VisibilityUtil.init(this);
        this.getServer().getPluginManager().registerEvents(new EventListener(new PacketListener(this)), this);
        this.getServer().getPluginManager().registerEvents(new ExampleListener(), this);
    }
}
