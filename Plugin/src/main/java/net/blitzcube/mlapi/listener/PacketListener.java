package net.blitzcube.mlapi.listener;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.modifier.IEntityModifier;
import net.blitzcube.peapi.api.entity.modifier.IModifiableEntity;
import net.blitzcube.peapi.api.event.IEntityPacketEvent;
import net.blitzcube.peapi.api.listener.IListener;
import net.blitzcube.peapi.api.packet.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by iso2013 on 5/24/2018.
 */
public class PacketListener implements IListener {
    private final MultiLineAPI parent;
    private final Map<Integer, Tag> entityTags;
    private final VisibilityStates state;
    private final IPacketEntityAPI packet;
    private final IEntityModifier<Boolean> invisible;

    public PacketListener(MultiLineAPI parent, Map<Integer, Tag> entityTags, VisibilityStates visibilityState,
                          IPacketEntityAPI
                                  packet) {
        this.parent = parent;
        this.entityTags = entityTags;
        this.state = visibilityState;
        this.packet = packet;
        this.invisible = packet.getModifierRegistry().lookup(EntityType.SHEEP, "INVISIBLE", Boolean.class);
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.DESTROY) {
            manageDestroyPacket((IEntityDestroyPacket) e.getPacket(), e.getPlayer());
            return;
        }
        IEntityIdentifier i = e.getPacket().getIdentifier();
        if (i == null) return;
        i.moreSpecific();
        if (i.isFakeEntity()) return;
        Tag t = entityTags.get(i.getEntityID());
        if (t == null) return;
        TagRenderer r = t.getRenderer();
        switch (e.getPacketType()) {
            case OBJECT_SPAWN:
            case ENTITY_SPAWN:
                r.spawnTag(t, e.getPlayer(), null);
                break;
            case MOUNT:
                checkMount((IEntityMountPacket) e.getPacket(), i, t, e.getPlayer());
                break;
            case DATA:
                checkData((IEntityDataPacket) e.getPacket(), t, e.getPlayer());
                break;
            case ADD_EFFECT:
                IEntityPotionAddPacket p2 = (IEntityPotionAddPacket) e.getPacket();
                if (p2.getEffect().getType() == PotionEffectType.INVISIBILITY) {
                    r.destroyTag(t, e.getPlayer(), null);
                }
                break;
            case REMOVE_EFFECT:
                IEntityPotionRemovePacket p3 = (IEntityPotionRemovePacket) e.getPacket();
                if (p3.getEffectType() == PotionEffectType.INVISIBILITY) {
                    r.spawnTag(t, e.getPlayer(), null);
                }
                break;
        }
    }

    private void checkData(IEntityDataPacket p, Tag t, Player target) {
        TagRenderer r = t.getRenderer();
        IModifiableEntity m = packet.wrap(p.getMetadata());
        if (!invisible.specifies(m)) return;
        if (invisible.getValue(m) && state.isSpawned(target, t)) {
            r.destroyTag(t, target, null);
        } else if (!invisible.getValue(m) && !state.isSpawned(target, t)) {
            r.spawnTag(t, target, null);
        }
    }

    private void checkMount(IEntityMountPacket p, IEntityIdentifier i, Tag t, Player target) {
        TagRenderer r = t.getRenderer();
        boolean tagEntities = p.getGroup().stream().allMatch(i1 -> {
            i1.moreSpecific();
            return i1.isFakeEntity();
        });
        Entity e;
        if (i.getEntity() != null &&
                ((e = i.getEntity().get()) != null) &&
                e.getPassengers().size() > 0)
            tagEntities = false;
        boolean isSpawned = state.isSpawned(target, t);
        Boolean shouldSpawn = state.isVisible(t, target);
        shouldSpawn = shouldSpawn != null ? shouldSpawn : t.getDefaultVisible();
        if (!shouldSpawn) return;
        if (!tagEntities && isSpawned) {
            r.destroyTag(t, target, null);
        } else if (tagEntities && !isSpawned) {
            r.spawnTag(t, target, p);
        }
    }

    private void manageDestroyPacket(IEntityDestroyPacket packet, Player player) {
        Set<Tag> possibleDeletions = new HashSet<>();
        packet.getGroup().forEach(identifier -> {
            Tag t1;
            if ((t1 = entityTags.get(identifier.getEntityID())) == null) return;
            t1.getRenderer().destroyTag(t1, player, packet);
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
