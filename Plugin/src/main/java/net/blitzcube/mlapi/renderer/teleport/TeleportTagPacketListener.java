package net.blitzcube.mlapi.renderer.teleport;

import com.google.common.base.Preconditions;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.iso2013.peapi.api.entity.EntityIdentifier;
import net.iso2013.peapi.api.entity.RealEntityIdentifier;
import net.iso2013.peapi.api.entity.hitbox.Hitbox;
import net.iso2013.peapi.api.event.EntityPacketEvent;
import net.iso2013.peapi.api.listener.Listener;
import net.iso2013.peapi.api.packet.EntityMovePacket;
import net.iso2013.peapi.api.packet.EntityPacket;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class TeleportTagPacketListener implements Listener {

    private final double lineHeight, bottomLineHeight;
    private final MultiLineAPI parent;
    private final VisibilityStates state;

    protected TeleportTagPacketListener(double lineHeight, double bottomLineHeight, MultiLineAPI parent,
                                        VisibilityStates state) {
        Preconditions.checkArgument(parent != null, "MLAPI instance must not be null");
        Preconditions.checkArgument(state != null, "VisibilityState instance must not be null");

        this.lineHeight = lineHeight;
        this.bottomLineHeight = bottomLineHeight;
        this.parent = parent;
        this.state = state;
    }

    @Override
    public void onEvent(EntityPacketEvent e) {
        EntityPacket packet = e.getPacket();
        EntityIdentifier id = packet.getIdentifier();
        if (id == null) return;

        if (!(id instanceof RealEntityIdentifier)) return;

        if (e.getPacket() instanceof EntityMovePacket) {
            EntityMovePacket movePacket = (EntityMovePacket) e.getPacket();

            Entity entity;
            if (movePacket.getMoveType() == EntityMovePacket.MoveType.LOOK ||
                    (entity = ((RealEntityIdentifier) id).getEntity()) == null)
                return;

            Tag tag = parent.getTag(entity);
            if (tag == null) return;

            if (movePacket.getMoveType() == EntityMovePacket.MoveType.TELEPORT) {
                Vector location = movePacket.getNewPosition().clone();
                Hitbox hitbox = tag.getTargetHitbox();
                if (hitbox != null) {
                    location.setY(location.getY() + (hitbox.getMax().getY() - hitbox.getMin().getY()) + bottomLineHeight);
                }

                for (RenderedTagLine line : tag.getLines()) {
                    if (!state.isLineSpawned(e.getPlayer(), line)) {
                        if (!line.shouldRemoveSpaceWhenNull()) {
                            location.setY(location.getY() + lineHeight);
                        }

                        continue;
                    }

                    EntityMovePacket newMovePacket = (EntityMovePacket) movePacket.clone();
                    newMovePacket.setIdentifier(line.getBottom());
                    newMovePacket.setNewPosition(location, true);
                    e.context().queueDispatch(newMovePacket, 0);
                    location.setY(location.getY() + lineHeight);
                }

                EntityMovePacket newMovePacket = (EntityMovePacket) movePacket.clone();
                newMovePacket.setIdentifier(tag.getTop());
                newMovePacket.setNewPosition(location, true);
                e.context().queueDispatch(newMovePacket, 0);
            } else {
                for (RenderedTagLine line : tag.getLines()) {
                    if (!state.isLineSpawned(e.getPlayer(), line)) continue;

                    EntityMovePacket newMovePacket = (EntityMovePacket) movePacket.clone();
                    newMovePacket.setIdentifier(line.getBottom());
                    e.context().queueDispatch(newMovePacket, 0);
                }

                EntityMovePacket newMovePacket = (EntityMovePacket) movePacket.clone();
                newMovePacket.setIdentifier(tag.getTop());
                e.context().queueDispatch(newMovePacket, 0);
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
