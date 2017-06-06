package net.blitzcube.mlapi.example;

import com.google.common.collect.Lists;
import net.blitzcube.mlapi.tag.TagController;
import net.blitzcube.mlapi.tag.TagLine;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by iso2013 on 6/2/2017.
 */
public class ExampleSecondTagController extends TagController {
    LinkedList<TagLine> lines;

    public ExampleSecondTagController() {
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
    public String getName(Entity forWhat) {
        return ChatColor.GREEN + "" + ChatColor.ITALIC + "Owner" + ChatColor.RESET + " `PREV`";
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
