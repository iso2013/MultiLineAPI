package net.iso2013.mlapi;

import com.google.common.collect.*;
import net.iso2013.mlapi.api.tag.Tag;
import net.iso2013.mlapi.tag.RenderedTagLine;
import net.iso2013.mlapi.tag.TagImpl;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class VisibilityStates {

    private final Multimap<Player, RenderedTagLine> spawnedLines;
    private final Multimap<Player, TagImpl> spawnedTags;
    private final Table<Player, TagImpl, Boolean> tagVisibility;

    public VisibilityStates() {
        this.spawnedLines = HashMultimap.create();
        this.spawnedTags = HashMultimap.create();
        this.tagVisibility = HashBasedTable.create();
    }

    public void setVisible(TagImpl tag, Player target, Boolean val) {
        this.tagVisibility.put(target, tag, val);
    }

    public Boolean isVisible(Tag tag, Player target) {
        return tagVisibility.get(target, tag);
    }

    public void purge(Player player) {
        this.tagVisibility.rowMap().remove(player);
        this.spawnedLines.removeAll(player);
    }

    public Multimap<Player, RenderedTagLine> getVisibilityMap() {
        return spawnedLines;
    }

    public boolean isSpawned(Player viewer, TagImpl tag) {
        return spawnedTags.containsEntry(viewer, tag);
    }

    public Stream<TagImpl> getVisible(Player target) {
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

    public void addSpawnedTag(Player player, TagImpl tag) {
        this.spawnedTags.put(player, tag);
    }

    public void removeSpawnedTag(Player player, TagImpl tag) {
        this.spawnedTags.remove(player, tag);
    }
}
