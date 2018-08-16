package net.blitzcube.mlapi.renderer.mount;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.event.IEntityPacketEvent;
import net.blitzcube.peapi.api.listener.IListener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class MountTagPacketListener implements IListener {

    private final IPacketEntityAPI packetAPI;
    private final LineEntityFactory lineFactory;
    private final Map<IFakeEntity, Entity> tagEntities;

    protected MountTagPacketListener(IPacketEntityAPI packetAPI, LineEntityFactory lineFactory, Map<IFakeEntity, Entity> tagEntities) {
        Preconditions.checkArgument(packetAPI != null, "PacketEntityAPI instance must not be null");
        Preconditions.checkArgument(lineFactory != null, "LineEntityFactory instance must not be null");

        this.packetAPI = packetAPI;
        this.lineFactory = lineFactory;
        this.tagEntities = (tagEntities != null) ? tagEntities : new HashMap<>();
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.CLICK) {
            IEntityIdentifier identifier = e.getPacket().getIdentifier();
            if (identifier == null) return;

            identifier.moreSpecific();
            WeakReference<IFakeEntity> fakeEntity = identifier.getFakeEntity();
            if (fakeEntity == null || fakeEntity.get() == null) return;

            Entity newEntity = tagEntities.get(fakeEntity.get());
            if (newEntity == null) return;

            IHitbox hitbox = lineFactory.getHitbox(newEntity);
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
