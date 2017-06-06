package net.blitzcube.mlapi.tag;

import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.Player;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public abstract class TagLine {
    private PacketUtil.FakeEntity line;
    private String cached;

    final PacketUtil.FakeEntity getLine() {
        return this.line;
    }

    final void setLine(PacketUtil.FakeEntity line) {
        this.line = line;
    }

    final String getCached() {
        return cached;
    }

    final void setCached(String cached) {
        this.cached = cached;
    }

    public abstract String getText(Player forWho);

    public boolean keepSpaceWhenNull() {return false;}
}
