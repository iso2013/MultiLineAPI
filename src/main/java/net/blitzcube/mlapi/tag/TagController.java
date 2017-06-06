package net.blitzcube.mlapi.tag;

import org.bukkit.entity.Entity;

import java.util.Collection;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public abstract class TagController {
    public abstract Collection<? extends TagLine> getLines(Entity forWhat);

    public abstract String getName(Entity forWhat);

    public int getPriority() {
        return 0;
    }
}
