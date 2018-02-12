package net.blitzcube.mlapi.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.api.MLAPI;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.api.tag.ITagLine;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class ExampleTagController implements ITagController {

    private final List<ITagLine> lines;
    public String lastMessage = null;

    public ExampleTagController() {
        this.lines = new LinkedList<>();
        this.lines.add(MLAPI.createTagLine("Hello!", p -> "Hello, " + p.getName() + "!"));
        this.lines.add(MLAPI.createTagLine("", p -> lastMessage));
    }

    @Override
    public Collection<ITagLine> getLines(Entity forWhat) {
        return lines;
    }

    @Override
    public String getName(Entity forWhat, Player forWho) {
        return ChatColor.AQUA + "" + ChatColor.BOLD + "Owner" + ChatColor.RESET + " `PREV`";
    }
}
