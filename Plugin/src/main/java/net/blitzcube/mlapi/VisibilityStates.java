package net.blitzcube.mlapi;

import java.util.Collection;
import java.util.stream.Stream;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;

import org.bukkit.entity.Player;

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
        this.tagVisibility.put(target, tag, val);
    }

    public Boolean isVisible(ITag tag, Player target) {
        return tagVisibility.get(target, tag);
    }

    public void purge(Player player) {
        this.tagVisibility.rowMap().remove(player);
        this.spawnedLines.removeAll(player);
    }

    public Multimap<Player, RenderedTagLine> getVisibilityMap() {
        return spawnedLines;
    }

    public boolean isSpawned(Player viewer, Tag tag) {
        return spawnedTags.containsEntry(viewer, tag);
    }

    public Stream<Tag> getVisible(Player target) {
        return (ImmutableSet.copyOf(spawnedTags.get(target))).stream();
    }

    public void addSpawnedLine(Player target, RenderedTagLine line) {
        this.spawnedLines.put(target, line);
    }

    public boolean isLineSpawned(Player target, RenderedTagLine line) {
        return spawnedLines.containsEntry(target, line);
    }

    public Collection<RenderedTagLine> getSpawnedLines(Player target) {
        return spawnedLines.get(target);
    }

    public void addSpawnedTag(Player player, Tag tag) {
        this.spawnedTags.put(player, tag);
    }

    public void removeSpawnedTag(Player player, Tag tag) {
        this.spawnedTags.remove(player, tag);
    }
}
