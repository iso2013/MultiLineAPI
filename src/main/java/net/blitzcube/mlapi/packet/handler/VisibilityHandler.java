package net.blitzcube.mlapi.packet.handler;

import java.util.Collections;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.VisibilityUtil;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class VisibilityHandler extends PacketHandler {

    private static final String INVISIBLE_CONST = "MLAPI_INVISIBLE";

    @Override
    public void handle(MultiLineAPI plugin, PacketEvent event, TagSender tags) {
        Player p = event.getPlayer();
        PacketContainer pc = event.getPacket();

        //TODO: Magic value
        int modified = pc.getIntegers().read(0);
        if (!plugin.isTagEnabled(modified)) return;

        //TODO: Magic value
        boolean invisible = VisibilityUtil.isMetadataInvisible(pc.getWatchableCollectionModifier().read(0));
        Entity e = EntityUtil.getEntities(p, 1, modified).findAny().orElse(null);
        if (e == null) return;

        boolean current = e.hasMetadata(INVISIBLE_CONST) && e.getMetadata(INVISIBLE_CONST).get(0).asBoolean();

        if (invisible != current) {
            e.removeMetadata(INVISIBLE_CONST, plugin);

            if (invisible) {
                //TODO: Despawn entities
            } else {
                //TODO: Spawn entities
            }

            e.setMetadata(INVISIBLE_CONST, new FixedMetadataValue(plugin, invisible));
        }
    }

    @Override
    public List<PacketType> getPackets() {
        return Collections.singletonList(PacketType.Play.Server.ENTITY_METADATA);
    }
}
