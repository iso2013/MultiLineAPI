package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.entity.modifier.IEntityModifier;
import net.blitzcube.peapi.api.entity.modifier.IModifiableEntity;
import net.blitzcube.peapi.api.event.IEntityPacketEvent;
import net.blitzcube.peapi.api.listener.IListener;
import net.blitzcube.peapi.api.packet.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by iso2013 on 5/24/2018.
 */
public class PacketListener implements IListener {
    private final MultiLineAPI parent;
    private final Map<Integer, Tag> entityTags;
    private final TagRenderer renderer;
    private final IPacketEntityAPI packet;
    private final IEntityModifier<Boolean> invisible;

    public PacketListener(MultiLineAPI parent, Map<Integer, Tag> entityTags, TagRenderer renderer, IPacketEntityAPI
            packet) {
        this.parent = parent;
        this.entityTags = entityTags;
        this.renderer = renderer;
        this.packet = packet;
        this.invisible = packet.getModifierRegistry().lookup(EntityType.SHEEP, "INVISIBLE", Boolean.class);
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.DESTROY) {
            manageDestroyPacket((IEntityDestroyPacket) e.getPacket(), e.getPlayer());
            return;
        } else if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.CLICK) {
            manageClick(e, e.getPlayer());
            return;
        }
        IEntityIdentifier i = e.getPacket().getIdentifier();
        i.moreSpecific();
        if (i == null || i.isFakeEntity()) return;
        Tag t = entityTags.get(i.getEntityID());
        if (t == null) return;
        switch (e.getPacketType()) {
            case OBJECT_SPAWN:
            case ENTITY_SPAWN:
                renderer.spawnTag(t, e.getPlayer(), null);
                break;
            case MOUNT:
                IEntityMountPacket p = (IEntityMountPacket) e.getPacket();
                boolean tagEntities = p.getGroup().stream().allMatch(i1 -> {
                    i1.moreSpecific();
                    return i1.isFakeEntity();
                });
                boolean isSpawned = renderer.isSpawned(e.getPlayer(), t);
                Boolean shouldSpawn = renderer.isVisible(t, e.getPlayer());
                shouldSpawn = shouldSpawn != null ? shouldSpawn : t.getDefaultVisible();
                if (!shouldSpawn) break;
                if (!tagEntities && isSpawned) {
                    renderer.destroyTag(t, e.getPlayer(), null);
                } else if (tagEntities && !isSpawned) {
                    renderer.spawnTag(t, e.getPlayer(), (IEntityMountPacket) e.getPacket());
                }
                break;
            case DATA:
                IModifiableEntity m = packet.wrap(((IEntityDataPacket) e.getPacket()).getMetadata());
                if (!invisible.specifies(m)) break;
                if (invisible.getValue(m) && renderer.isSpawned(e.getPlayer(), t)) {
                    renderer.destroyTag(t, e.getPlayer(), null);
                } else if (!invisible.getValue(m) && !renderer.isSpawned(e.getPlayer(), t)) {
                    renderer.spawnTag(t, e.getPlayer(), null);
                }
                break;
            case ADD_EFFECT:
                IEntityPotionAddPacket p2 = (IEntityPotionAddPacket) e.getPacket();
                if (p2.getEffect().getType() == PotionEffectType.INVISIBILITY) {
                    renderer.destroyTag(t, e.getPlayer(), null);
                }
                break;
            case REMOVE_EFFECT:
                IEntityPotionRemovePacket p3 = (IEntityPotionRemovePacket) e.getPacket();
                if (p3.getEffectType() == PotionEffectType.INVISIBILITY) {
                    renderer.spawnTag(t, e.getPlayer(), null);
                }
                break;
        }
    }

    private void manageClick(IEntityPacketEvent event, Player player) {
        IEntityIdentifier identifier = event.getPacket().getIdentifier();
        if (identifier == null) return;
        identifier.moreSpecific();
        WeakReference<IFakeEntity> fe = identifier.getFakeEntity();
        if (fe == null || fe.get() == null) return;
        Entity newEntity = renderer.getLineEntityFactory().getByTagEntity(fe.get());
        if (newEntity == null) return;
        IHitbox hitbox = renderer.getLineEntityFactory().getHitbox(newEntity);
        Location eyeLoc = player.getEyeLocation();
        if (hitbox.intersects(
                eyeLoc.toVector(),
                eyeLoc.getDirection(),
                newEntity.getLocation().toVector()
        )) {
            event.getPacket().setIdentifier(this.packet.wrap(newEntity));
        } else event.setCancelled(true);
    }

    private void manageDestroyPacket(IEntityDestroyPacket packet, Player player) {
        Set<Tag> possibleDeletions = new HashSet<>();
        packet.getGroup().forEach(identifier -> {
            Tag t1;
            if ((t1 = entityTags.get(identifier.getEntityID())) == null) return;
            renderer.destroyTag(t1, player, packet);
            possibleDeletions.add(t1);
        });
        Bukkit.getScheduler().runTaskLater(parent, () -> {
            for (Tag t : possibleDeletions) {
                if (!t.getTarget().isValid()) {
                    parent.deleteTag(t.getTarget());
                }
            }
        }, 1L);
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGH;
    }

    @Override
    public EntityType[] getTargets() {
        return EntityType.values();
    }

    @Override
    public boolean shouldFireForFake() {
        return true;
    }
}
