package net.blitzcube.mlapi.tag;

import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.Player;

/**
 * Created by iso2013 on 5/4/2017.
 */
public abstract class TagLine {
    private PacketUtil.FakeEntity line;

    final PacketUtil.FakeEntity getLine() {
        return this.line;
    }

    final void setLine(PacketUtil.FakeEntity line) {
        this.line = line;
    }

    public abstract String getText(Player forWho);
}
