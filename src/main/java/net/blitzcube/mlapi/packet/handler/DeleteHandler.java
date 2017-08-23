package net.blitzcube.mlapi.packet.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.util.EntityUtil;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class DeleteHandler extends PacketHandler {
    @Override
    public void handle(MultiLineAPI plugin, PacketEvent event, TagSender tags) {
        Player p = event.getPlayer();
        //TODO: Magic value
        int[] destroyed = event.getPacket().getIntegerArrays().read(0);
        boolean any = false;
        for (int i = 0; i < destroyed.length; i++)
            if (plugin.enabled.contains(destroyed[i])) {
                any = true;
                break;
            }
        if (!any) return;
        EntityUtil.getEntities(p, 1, destroyed)
                .filter(e -> plugin.tags.containsKey(e.getUniqueId()));
        //TODO: Make this actually despawn said entities' tags.
    }

    @Override
    public List<PacketType> getPackets() {
        return Collections.singletonList(PacketType.Play.Server.ENTITY_DESTROY);
    }
}
