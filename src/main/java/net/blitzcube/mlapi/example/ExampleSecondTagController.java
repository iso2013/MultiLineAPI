package net.blitzcube.mlapi.example;

import com.google.common.collect.Lists;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class ExampleSecondTagController extends TagController {
    private final List<TagLine> lines;

    ExampleSecondTagController() {
        lines = Lists.newLinkedList();
        lines.add(new TagLine() {
            @Override
            public String getText(Player forWho) {
                return "I'M MORE IMPORTANT!";
            }
        });
    }

    @Override
    public Collection<? extends TagLine> getLines(Entity forWhat) {
        return lines;
    }

    @Override
    public String getName(Entity forWhat, Player forWho) {
        return ChatColor.GREEN + "" + ChatColor.ITALIC + "Owner" + ChatColor.RESET + " `PREV`";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public int getNamePriority() { return -10; }
}
