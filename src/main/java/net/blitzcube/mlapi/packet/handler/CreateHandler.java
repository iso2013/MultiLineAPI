package net.blitzcube.mlapi.packet.handler;

import java.util.Arrays;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class CreateHandler extends PacketHandler {

    @Override
    public void handle(MultiLineAPI plugin, PacketEvent event, TagSender tags) {
        Player p = event.getPlayer();
        PacketContainer pc = event.getPacket();
        //TODO: Magic value
        if (!plugin.isTagEnabled(pc.getIntegers().read(0))) return;

        //TODO: Magic value
        Entity e = EntityUtil.getEntities(p, 1.05, pc.getIntegers().read(0)).findAny().orElse(null);
        if (e == null) return;
        if (!plugin.isEnabled(e)) return;
        if (!VisibilityUtil.isViewable(p, e, false)) return;

        //TODO: Spawn entities.
    }

    @Override
    public List<PacketType> getPackets() {
        return Arrays.asList(
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB
        );
    }
}
