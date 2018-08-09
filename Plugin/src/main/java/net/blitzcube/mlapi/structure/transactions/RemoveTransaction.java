package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;

import java.util.Collection;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class RemoveTransaction extends StructureTransaction {
    private final IEntityIdentifier below;
    private final IEntityIdentifier above;
    private final Collection<RenderedTagLine> removed;

    public RemoveTransaction(
            IEntityIdentifier below,
            IEntityIdentifier above,
            Collection<RenderedTagLine> removed) {
        this.below = below;
        this.above = above;
        this.removed = removed;
    }

    public IEntityIdentifier getBelow() {
        return below;
    }

    public IEntityIdentifier getAbove() {
        return above;
    }

    public Collection<RenderedTagLine> getRemoved() {
        return removed;
    }
}
