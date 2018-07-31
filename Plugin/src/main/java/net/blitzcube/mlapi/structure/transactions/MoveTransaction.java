package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by iso2013 on 7/31/2018.
 */
public class MoveTransaction extends StructureTransaction {
    private final IEntityIdentifier below;
    private final IEntityIdentifier above;
    private final List<RenderedTagLine> moved;
    private final boolean toSameLevel;

    public MoveTransaction(
            IEntityIdentifier below,
            IEntityIdentifier above,
            List<RenderedTagLine> moved,
            Player target,
            boolean toSameLevel) {
        super(target);
        this.below = below;
        this.above = above;
        this.moved = moved;
        this.toSameLevel = toSameLevel;
    }

    public IEntityIdentifier getBelow() {
        return below;
    }

    public IEntityIdentifier getAbove() {
        return above;
    }

    public List<RenderedTagLine> getMoved() {
        return moved;
    }

    public boolean isToSameLevel() {
        return toSameLevel;
    }
}
