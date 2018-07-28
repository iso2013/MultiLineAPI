package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class AddTransaction extends StructureTransaction {
    private final IEntityIdentifier below;
    private final IEntityIdentifier above;
    private final List<RenderedTagLine> added;
    private final Tag tag;

    public AddTransaction(
            IEntityIdentifier below,
            IEntityIdentifier above,
            List<RenderedTagLine> added,
            Player target,
            Tag tag) {
        super(target);
        this.below = below;
        this.above = above;
        this.added = added;
        this.tag = tag;
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

    public Tag getTag() {
        return tag;
    }
}
