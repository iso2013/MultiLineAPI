package net.blitzcube.mlapi.listener;

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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Created by iso2013 on 5/24/2018.
 */
public class PacketListener implements IListener {
    private final Map<Integer, Tag> entityTags;
    private final TagRenderer renderer;
    private final IPacketEntityAPI packet;
    private final IEntityModifier<Boolean> invisible;
    //private final IEntityModifier<Color> effectColor;

    public PacketListener(Map<Integer, Tag> entityTags, TagRenderer renderer, IPacketEntityAPI packet) {
        this.entityTags = entityTags;
        this.renderer = renderer;
        this.packet = packet;
        this.invisible = packet.getModifierRegistry().lookup(EntityType.SHEEP, "INVISIBLE", Boolean.class);
        //this.effectColor = packet.getModifierRegistry().lookup(EntityType.SHEEP, "POTION_COLOR", Color.class);
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        if (e.getPacketType() == IEntityPacketEvent.EntityPacketType.DESTROY) {
            manageDestroyPacket((IEntityDestroyPacket) e.getPacket(), e.getPlayer());
        }
        IEntityIdentifier i = e.getPacket().getIdentifier();
        if (i == null || i.isFakeEntity()) return;
        Tag t = entityTags.get(i.getEntityID());
        if (t == null) return;
        switch (e.getPacketType()) {
            case OBJECT_SPAWN:
            case ENTITY_SPAWN:
                Bukkit.broadcastMessage("SPAWNED " + i.getEntityID());
                renderer.spawnTag(t, e.getPlayer(), e.context());
                break;
            case MOUNT:
                IEntityMountPacket p = (IEntityMountPacket) e.getPacket();
                if (p.getGroup().stream().allMatch(IEntityIdentifier::isFakeEntity)) {
                    renderer.spawnTag(t, e.getPlayer(), (IEntityMountPacket) e.getPacket(), e.context());
                } else {
                    renderer.destroyTag(t, e.getPlayer(),
                            (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket());
                }
                break;
            case DATA:
                IModifiableEntity m = packet.wrap(((IEntityDataPacket) e.getPacket()).getMetadata());
                if (invisible.specifies(m) && invisible.getValue(m)) {
                    renderer.destroyTag(t, e.getPlayer(), null);
                }
                break;// else if(effectColor.specifies(m) &&
//                        (effectColor.getValue(m) == null || effectColor.getValue(m).asRGB() == 0)){
//                    i.moreSpecific();
//                    Entity en = i.getEntity().get();
//                    if(en instanceof LivingEntity){
//                        if(((LivingEntity) en).getActivePotionEffects().stream()
//                                .anyMatch(pe -> pe.getType() == PotionEffectType.INVISIBILITY)){
//
//                        }
//                    }
//                }
            case ADD_EFFECT:
                IEntityPotionAddPacket p2 = (IEntityPotionAddPacket) e.getPacket();
                if (p2.getEffect().getType() == PotionEffectType.INVISIBILITY) {
                    renderer.destroyTag(t, e.getPlayer(), null);
                }
                break;
            case REMOVE_EFFECT:
                IEntityPotionRemovePacket p3 = (IEntityPotionRemovePacket) e.getPacket();
                if (p3.getEffectType() == PotionEffectType.INVISIBILITY) {
                    renderer.spawnTag(t, e.getPlayer(), e.context());
                }
                break;
        }
    }

    private void manageDestroyPacket(IEntityDestroyPacket packet, Player player) {
        Bukkit.broadcastMessage("DESTROY PACKET CALLED " + entityTags.size());
        packet.getGroup().forEach(identifier -> {
            Bukkit.broadcastMessage("DESTROY IDENTIFIER CALLED " + identifier.getEntityID());
            for (int i : entityTags.keySet()) System.out.println(i);
            Tag t1;
            if ((t1 = entityTags.get(identifier.getEntityID())) == null) return;
            Bukkit.broadcastMessage("DESTROYING TAG?!?!");
            renderer.destroyTag(t1, player, packet);
        });
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGH;
    }

    @Override
    public EntityType[] getTargets() {
        return EntityType.values();
    }
}
