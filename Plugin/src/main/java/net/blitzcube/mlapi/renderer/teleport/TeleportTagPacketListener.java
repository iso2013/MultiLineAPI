package net.blitzcube.mlapi.renderer.teleport;

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
class TeleportTagPacketListener implements IListener {
    private final double lineHeight, bottomLineHeight;
    private final MultiLineAPI parent;
    private final VisibilityStates state;

    TeleportTagPacketListener(double lineHeight, double bottomLineHeight, MultiLineAPI parent, VisibilityStates state) {
        this.lineHeight = lineHeight;
        this.bottomLineHeight = bottomLineHeight;
        this.parent = parent;
        this.state = state;
    }

    @Override
    public void onEvent(IEntityPacketEvent e) {
        IEntityPacket packet = e.getPacket();
        IEntityIdentifier id = packet.getIdentifier();
        if (id == null) {
            return;
        }
        id.moreSpecific();
        if (id.isFakeEntity()) return;
        if (e.getPacket() instanceof IEntityMovePacket) {
            IEntityMovePacket p = (IEntityMovePacket) e.getPacket();
            if (p.getMoveType() == IEntityMovePacket.MoveType.LOOK) return;
            if (id.getEntity() == null) return;
            Entity entity = id.getEntity().get();
            if (entity == null) return;
            Tag t = parent.getTag(entity);
            if (t == null) return;

            if (p.getMoveType() == IEntityMovePacket.MoveType.TELEPORT) {
                Vector loc = p.getNewPosition().clone();
                IHitbox hb = t.getTargetHitbox();
                if (hb != null)
                    loc.setY(loc.getY() + (hb.getMax().getY() - hb.getMin().getY()) + bottomLineHeight);

                for (RenderedTagLine l : t.getLines()) {
                    if (!state.isLineSpawned(e.getPlayer(), l)) {
                        if (!l.shouldRemoveSpaceWhenNull()) {
                            loc.setY(loc.getY() + lineHeight);
                        }
                        continue;
                    }
                    IEntityMovePacket nP = (IEntityMovePacket) p.clone();
                    nP.setIdentifier(l.getBottom().getIdentifier());
                    nP.setNewPosition(loc, true);
                    e.context().queueDispatch(nP, 0);
                    loc.setY(loc.getY() + lineHeight);
                }

                IEntityMovePacket nP = (IEntityMovePacket) p.clone();
                nP.setIdentifier(t.getTop().getIdentifier());
                nP.setNewPosition(loc, true);
                e.context().queueDispatch(nP, 0);
            } else {
                for (RenderedTagLine l : t.getLines()) {
                    if (!state.isLineSpawned(e.getPlayer(), l)) {
                        continue;
                    }
                    IEntityMovePacket nP = (IEntityMovePacket) p.clone();
                    nP.setIdentifier(l.getBottom().getIdentifier());
                    e.context().queueDispatch(nP, 0);
                }

                IEntityMovePacket nP = (IEntityMovePacket) p.clone();
                nP.setIdentifier(t.getTop().getIdentifier());
                e.context().queueDispatch(nP, 0);
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
