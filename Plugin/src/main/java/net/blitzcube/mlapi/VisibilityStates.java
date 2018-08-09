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
    private final Multimap<Player, RenderedTagLine> spawnedLines;
    private final Multimap<Player, Tag> spawnedTags;
    private final Table<Player, Tag, Boolean> tagVisibility;

    public VisibilityStates() {
        this.spawnedLines = HashMultimap.create();
        this.spawnedTags = HashMultimap.create();
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
        spawnedLines.removeAll(player);
    }

    public Multimap<Player, RenderedTagLine> getVisibilityMap() {
        return spawnedLines;
    }

    public boolean isSpawned(Player viewer, Tag t) {
        return spawnedTags.containsEntry(viewer, t);
    }

    public Stream<Tag> getVisible(Player target) {
        return (ImmutableSet.copyOf(spawnedTags.get(target))).stream();
    }

    public void addSpawnedLine(Player target, RenderedTagLine l) {
        spawnedLines.put(target, l);
    }

    public boolean isLineSpawned(Player target, RenderedTagLine l) {
        return spawnedLines.containsEntry(target, l);
    }

    public Collection<RenderedTagLine> getSpawnedLines(Player target) {
        return spawnedLines.get(target);
    }

    public void addSpawnedTag(Player p, Tag t) {
        spawnedTags.put(p, t);
    }

    public void removeSpawnedTag(Player player, Tag t) {
        spawnedTags.remove(player, t);
    }
}
