package net.blitzcube.mlapi.listener;

import com.google.common.base.Preconditions;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.tag.Tag;
import net.iso2013.peapi.api.PacketEntityAPI;
import net.iso2013.peapi.api.entity.EntityIdentifier;
import net.iso2013.peapi.api.entity.RealEntityIdentifier;
import net.iso2013.peapi.api.entity.fake.FakeEntity;
import net.iso2013.peapi.api.entity.modifier.EntityModifier;
import net.iso2013.peapi.api.entity.modifier.EntityModifierRegistry;
import net.iso2013.peapi.api.entity.modifier.ModifiableEntity;
import net.iso2013.peapi.api.event.EntityPacketEvent;
import net.iso2013.peapi.api.listener.Listener;
import net.iso2013.peapi.api.packet.*;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Created by iso2013 on 5/24/2018.
 */
public class PacketListener implements Listener {

    private final MultiLineAPI parent;
    private final Map<Integer, Tag> entityTags;
    private final VisibilityStates state;
    private final PacketEntityAPI packet;

    private final EntityModifier<Boolean> invisible;
    private final EntityModifier<Optional<BaseComponent[]>> name;
    private final EntityModifier<Boolean> nameVisible;
    private final EntityModifier<String> legacyName;

    public PacketListener(MultiLineAPI parent, Map<Integer, Tag> entityTags, VisibilityStates visibilityState,
                          PacketEntityAPI packet) {
        Preconditions.checkArgument(parent != null, "MLAPI instance must not be null");
        Preconditions.checkArgument(visibilityState != null, "VisibilityState instance must not be null");
        Preconditions.checkArgument(packet != null, "PacketEntityAPI instance must not be null");

        this.parent = parent;
        this.entityTags = (entityTags != null) ? entityTags : new HashMap<>();
        this.state = visibilityState;
        this.packet = packet;

        EntityModifierRegistry reg = packet.getModifierRegistry();

        this.invisible = reg.lookup(EntityType.SHEEP, "INVISIBLE", Boolean.class);
        this.name = reg.lookupOptional(EntityType.SHEEP, "CUSTOM_NAME", BaseComponent[]
                .class);
        this.nameVisible = reg.lookup(EntityType.SHEEP, "CUSTOM_NAME_VISIBLE", Boolean.class);
        if (this.name == null) {
            this.legacyName = reg.lookup(EntityType.SHEEP, "CUSTOM_NAME", String.class);
        } else {
            this.legacyName = null;
        }
    }

    @Override
    public void onEvent(EntityPacketEvent e) {
        if (e.getPacketType() == EntityPacketEvent.EntityPacketType.DESTROY) {
            this.manageDestroyPacket((EntityDestroyPacket) e.getPacket(), e.getPlayer());
            return;
        }

        EntityIdentifier identifier = e.getPacket().getIdentifier();
        if (identifier == null) return;

        if (identifier instanceof FakeEntity) return;

        Tag tag = entityTags.get(identifier.getEntityID());
        if (tag == null) return;

        TagRenderer renderer = tag.getRenderer();
        switch (e.getPacketType()) {
            case ENTITY_SPAWN:
                this.clearNameData(((EntitySpawnPacket) e.getPacket()));
            case OBJECT_SPAWN:
                renderer.spawnTag(tag, e.getPlayer(), null);
                break;
            case MOUNT:
                this.checkMount((EntityMountPacket) e.getPacket(), identifier, tag, e.getPlayer());
                break;
            case DATA:
                EntityDataPacket dataPacket = (EntityDataPacket) e.getPacket();
                this.checkDataInvisible(dataPacket, tag, e.getPlayer());
                this.checkDataNames(dataPacket, tag, e.getPlayer());
                break;
            case ADD_EFFECT:
                EntityPotionAddPacket potionAddPacket = (EntityPotionAddPacket) e.getPacket();
                if (potionAddPacket.getEffect().getType() == PotionEffectType.INVISIBILITY) {
                    renderer.destroyTag(tag, e.getPlayer(), null);
                }
                break;
            case REMOVE_EFFECT:
                EntityPotionRemovePacket potionRemovePacket = (EntityPotionRemovePacket) e.getPacket();
                if (potionRemovePacket.getEffectType() == PotionEffectType.INVISIBILITY) {
                    renderer.spawnTag(tag, e.getPlayer(), null);
                }
                break;
            default:
                break;
        }
    }

    private void clearNameData(EntitySpawnPacket spawnPacket) {
        ModifiableEntity modifiable = packet.wrap(spawnPacket.getMetadata());
        if (name != null) {
            name.setValue(modifiable, Optional.empty());
        } else {
            if (!legacyName.specifies(modifiable)) return;
            legacyName.setValue(modifiable, "");
        }
        nameVisible.setValue(modifiable, false);
        spawnPacket.setMetadata(modifiable.getWatchableObjects());
        spawnPacket.rewriteMetadata();
    }

    private void checkDataNames(EntityDataPacket dataPacket, Tag tag, Player player) {
        ModifiableEntity modifiable = packet.wrap(dataPacket.getMetadata());
        if (name != null) {
            name.setValue(modifiable, Optional.empty());
        } else {
            if (!legacyName.specifies(modifiable)) return;
            legacyName.setValue(modifiable, "");
        }
        nameVisible.setValue(modifiable, false);
        dataPacket.setMetadata(modifiable.getWatchableObjects());
        dataPacket.rewriteMetadata();
        tag.updateName(player);
    }

    private void checkDataInvisible(EntityDataPacket dataPacket, Tag tag, Player target) {
        TagRenderer renderer = tag.getRenderer();
        ModifiableEntity modifiable = packet.wrap(dataPacket.getMetadata());
        if (!invisible.specifies(modifiable)) return;

        if (invisible.getValue(modifiable) && state.isSpawned(target, tag)) {
            renderer.destroyTag(tag, target, null);
        } else if (!invisible.getValue(modifiable) && !state.isSpawned(target, tag)) {
            renderer.spawnTag(tag, target, null);
        }
    }

    private void checkMount(EntityMountPacket mountPacket, EntityIdentifier identifier, Tag tag, Player target) {
        if (!(identifier instanceof RealEntityIdentifier)) return;

        TagRenderer renderer = tag.getRenderer();
        boolean tagEntities = mountPacket.getGroup().stream().allMatch(i -> i instanceof FakeEntity);

        Entity entity = ((RealEntityIdentifier) identifier).getEntity();
        if (entity.getPassengers().size() > 0) {
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

    private void manageDestroyPacket(EntityDestroyPacket destroyPacket, Player player) {
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
