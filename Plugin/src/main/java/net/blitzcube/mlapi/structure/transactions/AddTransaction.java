package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.iso2013.peapi.api.entity.EntityIdentifier;

import java.util.List;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class AddTransaction extends StructureTransaction {

    private final EntityIdentifier below;
    private final EntityIdentifier above;
    private final List<RenderedTagLine> added;

    public AddTransaction(EntityIdentifier below, EntityIdentifier above, List<RenderedTagLine> added) {
        this.below = below;
        this.above = above;
        this.added = added;
    }

    public EntityIdentifier getBelow() {
        return below;
    }

    public EntityIdentifier getAbove() {
        return above;
    }

    public List<RenderedTagLine> getAdded() {
        return added;
    }
}
