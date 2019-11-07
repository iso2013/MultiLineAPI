package net.blitzcube.mlapi.renderer.mount;

import com.google.common.base.Preconditions;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.iso2013.peapi.api.PacketEntityAPI;
import net.iso2013.peapi.api.entity.EntityIdentifier;
import net.iso2013.peapi.api.entity.fake.FakeEntity;
import net.iso2013.peapi.api.entity.hitbox.Hitbox;
import net.iso2013.peapi.api.event.EntityPacketEvent;
import net.iso2013.peapi.api.listener.Listener;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class MountTagPacketListener implements Listener {

    private final PacketEntityAPI packetAPI;
    private final LineEntityFactory lineFactory;
    private final Map<FakeEntity, Entity> tagEntities;

    protected MountTagPacketListener(PacketEntityAPI packetAPI, LineEntityFactory lineFactory, Map<FakeEntity,
            Entity> tagEntities) {
        Preconditions.checkArgument(packetAPI != null, "PacketEntityAPI instance must not be null");
        Preconditions.checkArgument(lineFactory != null, "LineEntityFactory instance must not be null");

        this.packetAPI = packetAPI;
        this.lineFactory = lineFactory;
        this.tagEntities = (tagEntities != null) ? tagEntities : new HashMap<>();
    }

    @Override
    public void onEvent(EntityPacketEvent e) {
        if (e.getPacketType() == EntityPacketEvent.EntityPacketType.CLICK) {
            EntityIdentifier identifier = e.getPacket().getIdentifier();
            if (identifier == null) return;

            if (!(identifier instanceof FakeEntity)) return;

            Entity newEntity = tagEntities.get(identifier);
            if (newEntity == null) return;

            Hitbox hitbox = lineFactory.getHitbox(newEntity);
            Location eyeLoc = e.getPlayer().getEyeLocation();
            if (hitbox.intersects(eyeLoc.toVector(), eyeLoc.getDirection(), newEntity.getLocation().toVector())) {
                e.getPacket().setIdentifier(packetAPI.wrap(newEntity));
            } else {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGH;
    }

    @Override
    public boolean shouldFireForFake() {
        return true;
    }

    @Override
    public EntityType[] getTargets() {
        return EntityType.values();
    }
}
