package net.blitzcube.mlapi;

import com.google.common.collect.*;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class VisibilityStates {
    private final Multimap<Player, RenderedTagLine> visibleLines;
    private final Multimap<Player, Tag> visibleTags;
    private final Table<Player, Tag, Boolean> tagVisibility;

    public VisibilityStates() {
        this.visibleLines = HashMultimap.create();
        this.visibleTags = HashMultimap.create();
        this.tagVisibility = HashBasedTable.create();
    }

    public void setVisible(Tag tag, Player target, Boolean val) {
        tagVisibility.put(target, tag, val);
    }

    public Boolean isVisible(ITag tag, Player target) {
        return tagVisibility.get(target, tag);
    }

    public void purge(Player player) {
        tagVisibility.rowMap().remove(player);
        visibleLines.removeAll(player);
    }

    public Multimap<Player, RenderedTagLine> getVisibilityMap() {
        return visibleLines;
    }

    public boolean isSpawned(Player viewer, Tag t) {
        return visibleTags.containsEntry(viewer, t);
    }

    public Stream<Tag> getVisible(Player target) {
        return (ImmutableSet.copyOf(visibleTags.get(target))).stream();
    }

    public void addVisibleLine(Player target, RenderedTagLine l) {
        visibleLines.put(target, l);
    }

    public Collection<RenderedTagLine> getVisibleLines(Player target) {
        return visibleLines.get(target);
    }

    public void addVisibleTag(Player p, Tag t) {
        visibleTags.put(p, t);
    }

    public void removeVisibleTag(Player player, Tag t) {
        visibleTags.remove(player, t);
    }
}
