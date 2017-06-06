package net.blitzcube.mlapi.tag;

import org.bukkit.entity.Entity;

import java.util.Collection;

/**
 * Created by iso2013 on 5/4/2017.
 */
public abstract class TagController {
    public abstract Collection<? extends TagLine> getLines(Entity forWhat);

    public abstract String getName(Entity forWhat);

    public int getPriority() {
        return 0;
    }
}
