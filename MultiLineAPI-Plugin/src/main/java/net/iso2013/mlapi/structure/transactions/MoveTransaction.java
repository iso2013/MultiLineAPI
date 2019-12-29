package net.iso2013.mlapi.structure.transactions;

import net.iso2013.mlapi.tag.RenderedTagLine;
import net.iso2013.peapi.api.entity.EntityIdentifier;

import java.util.List;

/**
 * Created by iso2013 on 7/31/2018.
 */
public class MoveTransaction extends StructureTransaction {

    private final EntityIdentifier below;
    private final EntityIdentifier above;
    private final List<RenderedTagLine> moved;
    private final boolean toSameLevel;

    public MoveTransaction(EntityIdentifier below, EntityIdentifier above, List<RenderedTagLine> moved, boolean toSameLevel) {
        this.below = below;
        this.above = above;
        this.moved = moved;
        this.toSameLevel = toSameLevel;
    }

    public EntityIdentifier getBelow() {
        return below;
    }

    public EntityIdentifier getAbove() {
        return above;
    }

    public List<RenderedTagLine> getMoved() {
        return moved;
    }

    public boolean isToSameLevel() {
        return toSameLevel;
    }
}
