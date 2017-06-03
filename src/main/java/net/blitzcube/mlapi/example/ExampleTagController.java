package net.blitzcube.mlapi.example;

import com.google.common.collect.Lists;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by iso2013 on 6/2/2017.
 */
public class ExampleTagController extends net.blitzcube.mlapi.tag.TagController {
    LinkedList<TagLine> lines;

    public ExampleTagController() {
        lines = Lists.newLinkedList();
        lines.add(new TagLine() {
            @Override
            public String getText(Player forWho) {
                return "Hello, " + forWho.getName() + "!";
            }
        });
    }

    @Override
    public Collection<? extends TagLine> getLines(Entity forWhat) {
        return lines;
    }

    @Override
    public String getName(Entity forWhat) {
        return ChatColor.AQUA + "" + ChatColor.BOLD + "Owner" + ChatColor.RESET + " `PREV`";
    }
}
