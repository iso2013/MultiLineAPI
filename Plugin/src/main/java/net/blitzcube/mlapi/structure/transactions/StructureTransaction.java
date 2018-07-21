package net.blitzcube.mlapi.structure.transactions;

import org.bukkit.entity.Player;

/**
 * Created by iso2013 on 6/13/2018.
 */
public abstract class StructureTransaction {
    private final Player target;

    public StructureTransaction(Player target) {
        this.target = target;
    }

    public Player getTarget() {
        return target;
    }
}
