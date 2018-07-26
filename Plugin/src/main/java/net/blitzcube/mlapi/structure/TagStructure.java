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
        Collections.reverse(newLines);
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
        for (int i = idx; i < lines.size(); i++) {
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i)))
                return lines.get(i).getBottom().getIdentifier();
        }
        return tag.getTop().getIdentifier();
    }

    private IEntityIdentifier getBelow(int idx, Player player, RangeSeries added, RangeSeries removed) {
        for (int i = idx; i >= 0; i--) {
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i)))
                return lines.get(i).getStack().getLast().getIdentifier();
        }
        return tag.getBottom().getIdentifier();
    }

    public ImmutableList<RenderedTagLine> getLines() {
        return ImmutableList.copyOf(lines);
    }
}
