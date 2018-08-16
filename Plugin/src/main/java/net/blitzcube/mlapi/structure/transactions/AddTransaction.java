package net.blitzcube.mlapi.structure.transactions;

import java.util.List;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class AddTransaction extends StructureTransaction {

    private final IEntityIdentifier below;
    private final IEntityIdentifier above;
    private final List<RenderedTagLine> added;

    public AddTransaction(IEntityIdentifier below, IEntityIdentifier above, List<RenderedTagLine> added) {
        this.below = below;
        this.above = above;
        this.added = added;
    }

    public IEntityIdentifier getBelow() {
        return below;
    }

    public IEntityIdentifier getAbove() {
        return above;
    }

    public List<RenderedTagLine> getAdded() {
        return added;
    }
}
