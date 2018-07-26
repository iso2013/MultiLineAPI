package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class AddTransaction extends StructureTransaction {
    private final IEntityIdentifier below;
    private final IEntityIdentifier above;
    private final List<RenderedTagLine> added;
    private final Entity tagged;

    public AddTransaction(
            IEntityIdentifier below,
            IEntityIdentifier above,
            List<RenderedTagLine> added,
            Player target,
            Entity tagged) {
        super(target);
        this.below = below;
        this.above = above;
        this.added = added;
        this.tagged = tagged;
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

    public Entity getTagged() {
        return tagged;
    }
}
