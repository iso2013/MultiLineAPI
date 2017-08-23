package net.blitzcube.mlapi.packet.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.primitives.Ints;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.util.EntityUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

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
        if (!plugin.enabled.contains(modified)) return;
        Entity e = EntityUtil.getEntities(p, 1.05, modified).findAny().orElse(null);
        if (e == null) return;
        Tag t;
        if ((t = plugin.tags.get(e.getUniqueId())) == null) return;
        //TODO: Magic value
        List<Integer> passengers = Ints.asList(pc.getIntegerArrays().read(0));
        int baseEntity = t.getBaseID();
        if (passengers.contains(baseEntity) && passengers.size() > 1 || !passengers.contains(baseEntity) &&
                passengers.size() > 0) {
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
