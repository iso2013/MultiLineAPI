package net.blitzcube.mlapi.structure.transactions;

import net.blitzcube.mlapi.tag.RenderedTagLine;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Created by iso2013 on 6/13/2018.
 */
public class NameTransaction extends StructureTransaction {
    private final Map<RenderedTagLine, String> queuedNames;

    public NameTransaction(Map<RenderedTagLine, String> queuedNames, Player target) {
        super(target);
        this.queuedNames = queuedNames;
    }

    public Map<RenderedTagLine, String> getQueuedNames() {
        return queuedNames;
    }
}
