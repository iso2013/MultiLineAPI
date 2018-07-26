package net.blitzcube.mlapi.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.structure.transactions.AddTransaction;
import net.blitzcube.mlapi.structure.transactions.NameTransaction;
import net.blitzcube.mlapi.structure.transactions.RemoveTransaction;
import net.blitzcube.mlapi.structure.transactions.StructureTransaction;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.mlapi.util.RangeSeries;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 6/12/2018.
 */
public class TagStructure {
    private static final Comparator<ITagController> comp = Comparator.comparingInt(ITagController::getPriority);

    private final List<RenderedTagLine> lines;
    private final Tag tag;
    private final LineEntityFactory factory;
    private final Multimap<Player, RenderedTagLine> visible;

    public TagStructure(Tag tag, LineEntityFactory factory, Multimap<Player, RenderedTagLine> visible) {
        this.lines = new ArrayList<>();
        this.tag = tag;
        this.factory = factory;
        this.visible = visible;
    }

    public Stream<StructureTransaction> addTagController(ITagController c, Stream<Player> players) {
        List<RenderedTagLine> newLines = c.getFor(tag.getTarget()).stream()
                .map(l -> new RenderedTagLine(c, l, tag.getTarget(), factory)).collect(Collectors.toList());
        if (newLines.size() == 0) return null;

        int idx = lines.size();
        for (int i = 0; i < lines.size(); i++)
            if (comp.compare(c, lines.get(i).getController()) > 0) idx = i + 1;
        lines.addAll(idx, newLines);

        int fIdx = idx;
        return players.map(p -> new AddTransaction(
                getBelow(fIdx - 1, p, null, null),
                getAbove(fIdx + lines.size(), p, null, null),
                newLines,
                p,
                tag.getTarget()
        ));
    }

    public Stream<StructureTransaction> removeTagController(ITagController c, Stream<Player> players) {
        int idx = -1;

        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).getController() == c) {
                idx = i;
                break;
            }
        if (idx == -1) return null;

        List<RenderedTagLine> removed = lines.stream().filter(l -> l.getController() == c).collect(Collectors.toList());
        lines.removeAll(removed);

        int fIdx = idx;
        return players.map(p -> new RemoveTransaction(
                getBelow(fIdx - 1, p, null, null),
                getAbove(fIdx, p, null, null),
                removed,
                p
        ));
    }

    public Stream<StructureTransaction> createUpdateTransactions(Predicate<RenderedTagLine> matcher, Player player) {
        if (matcher == null) matcher = l -> true;
        List<StructureTransaction> transactions = new LinkedList<>();
        Map<RenderedTagLine, String> lines = new HashMap<>();

        RangeSeries added = new RangeSeries(), removed = new RangeSeries();
        RenderedTagLine l;
        for (int i = 0; i < this.lines.size(); i++) {
            if (!matcher.test(l = this.lines.get(i))) continue;
            boolean v = visible.containsEntry(player, l);
            String newVal = l.get(player);
            if (newVal == null && v && l.shouldRemoveSpaceWhenNull()) {
                removed.put(i);
            } else if (newVal != null && !v) {
                added.put(i);
                lines.put(l, newVal);
            } else {
                lines.put(l, newVal);
            }
        }

        transactions.add(new NameTransaction(lines, player));

        List<RenderedTagLine> subjectLines = new LinkedList<>();
        for (RangeSeries.Range r : removed.getRanges()) {
            for (int j : r) subjectLines.add(this.lines.get(j));
            transactions.add(new RemoveTransaction(
                    getBelow(r.getLower() - 1, player, added, removed),
                    getAbove(r.getUpper() + 1, player, added, removed),
                    ImmutableSet.copyOf(subjectLines),
                    player
            ));
            subjectLines.clear();
        }

        for (RangeSeries.Range r : added.getRanges()) {
            subjectLines = new LinkedList<>();
            for (int j : r) subjectLines.add(this.lines.get(j));
            transactions.add(new AddTransaction(
                    getBelow(r.getLower() - 1, player, added, removed),
                    getAbove(r.getUpper() + 1, player, added, removed),
                    subjectLines,
                    player,
                    tag.getTarget()
            ));
        }

        return transactions.stream();
    }

    private IEntityIdentifier getAbove(int idx, Player player, RangeSeries added, RangeSeries removed) {
        Bukkit.broadcastMessage("Calculating line above " + (idx - 1) + "! (" + (idx >= 1 && idx < lines.size() ?
                lines.get(idx - 1).get(player) : "bottom of tag?") + ")");
        for (int i = 0; i < lines.size(); i++) {
            Bukkit.broadcastMessage("Current value: " + i + " - " + lines.get(i).get(player));
        }
        for (int i = idx; i <= lines.size(); i++) {
            if (i == lines.size()) {
                Bukkit.broadcastMessage("Returned top of tag for above.");
                return tag.getTop().getIdentifier();
            }
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i))) {
                Bukkit.broadcastMessage("Returned line for above: " + lines.get(i).get(player));
                return lines.get(i).getBottom().getIdentifier();
            } else {
                if (visible.containsEntry(player, lines.get(i))) {
                    Bukkit.broadcastMessage("Above: " + i + " is currently visible to the player. (" + lines.get(i)
                            .get(player) + ")");
                }
                if (!(removed == null || !removed.contains(i))) {
                    Bukkit.broadcastMessage("Above: " + i + " is scheduled for removal. (" + lines.get(i).get(player)
                            + ")");
                }
                if (added != null && added.contains(i)) {
                    Bukkit.broadcastMessage("Above: " + i + " is scheduled for addition. (" + lines.get(i).get(player) + ")");
                }
            }
        }
        throw new IllegalStateException("This should never happen! Failed to find a suitable top entity");
    }

    private IEntityIdentifier getBelow(int idx, Player player, RangeSeries added, RangeSeries removed) {
        Bukkit.broadcastMessage("Calculating line below " + (idx + 1) + "! (" + ((idx + 1 < lines.size() && idx >=
                -1) ? lines.get(idx + 1).get(player) : "top of tag?") + ")");
        for (int i = 0; i < lines.size(); i++) {
            Bukkit.broadcastMessage("Current value: " + i + " - " + lines.get(i).get(player));
        }
        for (int i = idx; i >= -1; i--) {
            if (i == -1) {
                Bukkit.broadcastMessage("Returned bottom of tag for below.");
                return tag.getBottom().getIdentifier();
            }
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i))) {
                Bukkit.broadcastMessage("Returned line for below: " + lines.get(i).get(player));
                return lines.get(i).getStack().getLast().getIdentifier();
            } else {
                if (visible.containsEntry(player, lines.get(i))) {
                    Bukkit.broadcastMessage("Below: " + i + " is currently visible to the player. (" + lines.get(i)
                            .get(player) + ")");
                }
                if (!(removed == null || !removed.contains(i))) {
                    Bukkit.broadcastMessage("Below: " + i + " is scheduled for removal. (" + lines.get(i).get(player)
                            + ")");
                }
                if (added != null && added.contains(i)) {
                    Bukkit.broadcastMessage("Below: " + i + " is scheduled for addition. (" + lines.get(i).get(player) + ")");
                }
            }
        }
        throw new IllegalStateException("This should never happen! Failed to find a suitable bottom entity");
    }

    public ImmutableList<RenderedTagLine> getLines() {
        return ImmutableList.copyOf(lines);
    }
}
