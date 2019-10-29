package net.blitzcube.mlapi.renderer.teleport;

import com.google.common.base.Preconditions;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.event.IEntityPacketEvent;
import net.blitzcube.peapi.api.listener.IListener;
import net.blitzcube.peapi.api.packet.IEntityMovePacket;
import net.blitzcube.peapi.api.packet.IEntityPacket;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class TeleportTagPacketListener implements IListener {

    private final double lineHeight, bottomLineHeight;
    private final MultiLineAPI parent;
    private final VisibilityStates state;

    protected TeleportTagPacketListener(double lineHeight, double bottomLineHeight, MultiLineAPI parent, VisibilityStates state) {
        Preconditions.checkArgument(parent != null, "MLAPI instance must not be null");
        Preconditions.checkArgument(state != null, "VisibilityState instance must not be null");

        this.lineHeight = lineHeight;
        this.bottomLineHeight = bottomLineHeight;
        this.parent = parent;
        this.state = state;
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        IEntityPacket packet = e.getPacket();
        IEntityIdentifier id = packet.getIdentifier();
        if (id == null) return;

        if (id.isFakeEntity()) return;

        if (e.getPacket() instanceof IEntityMovePacket) {
            IEntityMovePacket movePacket = (IEntityMovePacket) e.getPacket();
            if (movePacket.getMoveType() == IEntityMovePacket.MoveType.LOOK || id.getEntity() == null) return;

            Entity entity = id.getEntity();
            if (entity == null) return;

            Tag tag = parent.getTag(entity);
            if (tag == null) return;

            if (movePacket.getMoveType() == IEntityMovePacket.MoveType.TELEPORT) {
                Vector location = movePacket.getNewPosition().clone();
                IHitbox hitbox = tag.getTargetHitbox();
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

                    IEntityMovePacket newMovePacket = (IEntityMovePacket) movePacket.clone();
                    newMovePacket.setIdentifier(line.getBottom().getIdentifier());
                    newMovePacket.setNewPosition(location, true);
                    e.context().queueDispatch(newMovePacket, 0);
                    location.setY(location.getY() + lineHeight);
                }

                IEntityMovePacket newMovePacket = (IEntityMovePacket) movePacket.clone();
                newMovePacket.setIdentifier(tag.getTop().getIdentifier());
                newMovePacket.setNewPosition(location, true);
                e.context().queueDispatch(newMovePacket, 0);
            }
            else {
                for (RenderedTagLine line : tag.getLines()) {
                    if (!state.isLineSpawned(e.getPlayer(), line)) continue;

                    IEntityMovePacket newMovePacket = (IEntityMovePacket) movePacket.clone();
                    newMovePacket.setIdentifier(line.getBottom().getIdentifier());
                    e.context().queueDispatch(newMovePacket, 0);
                }

                IEntityMovePacket newMovePacket = (IEntityMovePacket) movePacket.clone();
                newMovePacket.setIdentifier(tag.getTop().getIdentifier());
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
