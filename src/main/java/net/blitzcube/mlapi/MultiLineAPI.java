package net.blitzcube.mlapi;

import com.google.common.collect.Maps;
import net.blitzcube.mlapi.example.ExampleListener;
import net.blitzcube.mlapi.listener.EventListener;
import net.blitzcube.mlapi.listener.PacketListener;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        EntityUtil.init();
        VisibilityUtil.init(this);
        this.getServer().getPluginManager().registerEvents(new EventListener(new PacketListener(this)), this);
        this.getServer().getPluginManager().registerEvents(new ExampleListener(), this);
    }
}
