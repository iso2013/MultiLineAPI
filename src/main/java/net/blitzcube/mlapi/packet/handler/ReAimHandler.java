package net.blitzcube.mlapi.packet.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.packet.PacketHandler;
import net.blitzcube.mlapi.packet.TagSender;
import net.blitzcube.mlapi.util.EntityUtil;
import net.blitzcube.mlapi.util.HitboxUtil;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class ReAimHandler extends PacketHandler {

    private final Map<Integer, UUID> tagMap = new HashMap<>();

    @Override
    public void handle(MultiLineAPI plugin, PacketEvent e, TagSender tags) {
        Player p = e.getPlayer();
        PacketContainer pc = e.getPacket();

        //TODO: Magic value
        UUID u = tagMap.get(pc.getIntegers().read(0));
        if (u == null) return;

        Entity en = EntityUtil.getEntities(p, new int[]{8, 8, 8}, u).findAny().orElse(null);
        if (en == null) return;
        if (!HitboxUtil.isLookingAt(p, en)) return;

        //TODO: Magic value
        pc.getIntegers().write(0, en.getEntityId());
    }

    @Override
    public List<PacketType> getPackets() {
        return Collections.singletonList(PacketType.Play.Client.USE_ENTITY);
    }

    public void addEntity(Integer entityID, UUID forWhat) {
        this.tagMap.put(entityID, forWhat);
    }

    public void removeEntity(Integer entityID) {
        this.tagMap.remove(entityID);
    }

    public void removeEntity(Integer entityID, UUID forWhat) {
        this.tagMap.remove(entityID, forWhat);
    }

    public void removeAllFor(UUID who) {
        if (who == null) return;
        this.tagMap.entrySet().removeIf(entry -> who.equals(entry.getValue()));
    }
}
