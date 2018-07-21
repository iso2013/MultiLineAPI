package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            Collection<RenderedTagLine> added,
            Player target) {
        super(target);
        this.below = below;
        this.above = above;
        this.removed = added;
        String bI = below.isFakeEntity() ? " Fake entity - " + below.getFakeEntity().get().getEntityID() + " " : " " +
                "Real entity - " + below.getEntity().get().getCustomName() + " ";
        String aI = below.isFakeEntity() ? " Fake entity - " + above.getFakeEntity().get().getEntityID() + " " : " " +
                "Real entity - " + above.getEntity().get().getCustomName() + " ";
        Bukkit.broadcastMessage("MOUNTING ENTITY " + aI + " ONTO " + bI);
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
