package net.blitzcube.mlapi.listener;

import com.google.common.base.Preconditions;
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
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Created by iso2013 on 5/24/2018.
 */
public class PacketListener implements IListener {

    private final MultiLineAPI parent;
    private final Map<Integer, Tag> entityTags;
    private final VisibilityStates state;
    private final IPacketEntityAPI packet;

    private final IEntityModifier<Boolean> invisible;
    private final IEntityModifier<Optional<BaseComponent[]>> name;
    private final IEntityModifier<String> legacyName;

    public PacketListener(MultiLineAPI parent, Map<Integer, Tag> entityTags, VisibilityStates visibilityState,
                          IPacketEntityAPI packet) {
        Preconditions.checkArgument(parent != null, "MLAPI instance must not be null");
        Preconditions.checkArgument(visibilityState != null, "VisibilityState instance must not be null");
        Preconditions.checkArgument(packet != null, "PacketEntityAPI instance must not be null");

        this.parent = parent;
        this.entityTags = (entityTags != null) ? entityTags : new HashMap<>();
        this.state = visibilityState;
        this.packet = packet;
        this.invisible = packet.getModifierRegistry().lookup(EntityType.SHEEP, "INVISIBLE", Boolean.class);
        //FIXME
        //this.name = packet.getModifierRegistry().lookupOptional(EntityType.SHEEP, "CUSTOM_NAME", BaseComponent[]
        // .class);
        this.name = null;
        if (this.name == null) {
            this.legacyName = packet.getModifierRegistry().lookup(EntityType.SHEEP, "CUSTOM_NAME", String.class);
        } else {
            this.legacyName = null;
        }
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.DESTROY) {
            this.manageDestroyPacket((IEntityDestroyPacket) e.getPacket(), e.getPlayer());
            return;
        }

        IEntityIdentifier identifier = e.getPacket().getIdentifier();
        if (identifier == null) return;

        identifier.moreSpecific();
        if (identifier.isFakeEntity()) return;

        Tag tag = entityTags.get(identifier.getEntityID());
        if (tag == null) return;

        TagRenderer renderer = tag.getRenderer();
        switch (e.getPacketType()) {
            case OBJECT_SPAWN:
            case ENTITY_SPAWN:
                renderer.spawnTag(tag, e.getPlayer(), null);
                break;
            case MOUNT:
                this.checkMount((IEntityMountPacket) e.getPacket(), identifier, tag, e.getPlayer());
                break;
            case DATA:
                IEntityDataPacket dataPacket = (IEntityDataPacket) e.getPacket();
                this.checkDataInvisible(dataPacket, tag, e.getPlayer());
                this.checkDataNames(dataPacket, tag, e.getPlayer());
                break;
            case ADD_EFFECT:
                IEntityPotionAddPacket potionAddPacket = (IEntityPotionAddPacket) e.getPacket();
                if (potionAddPacket.getEffect().getType() == PotionEffectType.INVISIBILITY) {
                    renderer.destroyTag(tag, e.getPlayer(), null);
                }
                break;
            case REMOVE_EFFECT:
                IEntityPotionRemovePacket potionRemovePacket = (IEntityPotionRemovePacket) e.getPacket();
                if (potionRemovePacket.getEffectType() == PotionEffectType.INVISIBILITY) {
                    renderer.spawnTag(tag, e.getPlayer(), null);
                }
                break;
            default:
                break;
        }
    }

    private void checkDataNames(IEntityDataPacket dataPacket, Tag tag, Player player) {
        IModifiableEntity modifiable = packet.wrap(dataPacket.getMetadata());
        if (name != null) {
            if (!name.specifies(modifiable)) return;
            name.setValue(modifiable, Optional.empty());
        } else {
            if (!legacyName.specifies(modifiable)) return;
            legacyName.setValue(modifiable, "");
        }
        dataPacket.setMetadata(modifiable.getWatchableObjects());
        dataPacket.rewriteMetadata();
        tag.updateName(player);
    }

    private void checkDataInvisible(IEntityDataPacket dataPacket, Tag tag, Player target) {
        TagRenderer renderer = tag.getRenderer();
        IModifiableEntity modifiable = packet.wrap(dataPacket.getMetadata());
        if (!invisible.specifies(modifiable)) return;

        if (invisible.getValue(modifiable) && state.isSpawned(target, tag)) {
            renderer.destroyTag(tag, target, null);
        } else if (!invisible.getValue(modifiable) && !state.isSpawned(target, tag)) {
            renderer.spawnTag(tag, target, null);
        }
    }

    private void checkMount(IEntityMountPacket mountPacket, IEntityIdentifier identifier, Tag tag, Player target) {
        TagRenderer renderer = tag.getRenderer();
        boolean tagEntities = mountPacket.getGroup().stream().allMatch(i1 -> {
            i1.moreSpecific();
            return i1.isFakeEntity();
        });

        Optional<WeakReference<Entity>> entity = Optional.ofNullable(identifier.getEntity());
        if (entity.isPresent() && entity.get().get().getPassengers().size() > 0) {
            tagEntities = false;
        }

        boolean isSpawned = state.isSpawned(target, tag);
        Boolean shouldSpawn = state.isVisible(tag, target);
        shouldSpawn = (shouldSpawn != null) ? shouldSpawn : tag.getDefaultVisible();
        if (!shouldSpawn) return;

        if (!tagEntities && isSpawned) {
            renderer.destroyTag(tag, target, null);
        } else if (tagEntities && !isSpawned) {
            renderer.spawnTag(tag, target, mountPacket);
        }
    }

    private void manageDestroyPacket(IEntityDestroyPacket destroyPacket, Player player) {
        Set<Tag> possibleDeletions = new HashSet<>();
        destroyPacket.getGroup().forEach(identifier -> {
            Tag tag = entityTags.get(identifier.getEntityID());
            if (tag == null) return;

            tag.getRenderer().destroyTag(tag, player, destroyPacket);
            possibleDeletions.add(tag);
        });

        Bukkit.getScheduler().runTaskLater(parent, () -> {
            for (Tag tag : possibleDeletions) {
                if (!tag.getTarget().isValid()) {
                    parent.deleteTag(tag.getTarget());
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
