package net.blitzcube.mlapi.packet.handler;

import java.util.Collections;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.primitives.Ints;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.util.EntityUtil;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class RemountHandler extends PacketHandler {

    @Override
    public void handle(MultiLineAPI plugin, PacketEvent event, TagSender tags) {
        Player p = event.getPlayer();
        PacketContainer pc = event.getPacket();
        //TODO: Magic value
        int modified = pc.getIntegers().read(0);
        if (!plugin.isTagEnabled(modified)) return;

        Entity e = EntityUtil.getEntities(p, 1.05, modified).findAny().orElse(null);
        if (e == null) return;

        ITag t;
        if ((t = plugin.getTag(e)) == null) return;

        //TODO: Magic value
        List<Integer> passengers = Ints.asList(pc.getIntegerArrays().read(0));
        int baseEntity = t.getBaseId();
        if (passengers.contains(baseEntity) && passengers.size() > 1 || !passengers.contains(baseEntity) && passengers.size() > 0) {
            //TODO: Hide the tags of the name.
        }

        if (!passengers.contains(baseEntity)) {
            passengers.add(baseEntity);
            //TODO: Magic value
            pc.getIntegerArrays().write(0, Ints.toArray(passengers));
        }
    }

    @Override
    public List<PacketType> getPackets() {
        return Collections.singletonList(PacketType.Play.Server.MOUNT);
    }

}
