package net.blitzcube.mlapi.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.transactions.*;
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
    private static final Comparator<ITagController> CONTROLLER_COMPARATOR = Comparator.comparingInt
            (ITagController::getPriority);

    private final List<RenderedTagLine> lines;
    private final Tag tag;
    private final TagRenderer renderer;
    private final LineEntityFactory factory;
    private final Multimap<Player, RenderedTagLine> visible;

    public TagStructure(Tag tag, LineEntityFactory factory, Multimap<Player, RenderedTagLine> visible) {
        this.lines = new ArrayList<>();
        this.tag = tag;
        this.factory = factory;
        this.visible = visible;
        this.renderer = tag.getRenderer();
    }

    public Stream<StructureTransaction> addTagController(ITagController c, Stream<Player> players) {
        int idx = 0;
        for (int i = 0; i < lines.size(); i++) {
            int comp = CONTROLLER_COMPARATOR.compare(c, lines.get(i).getController());
            if (comp > 0) {
                idx = i + 1;
            }
        }

        RangeSeries added = new RangeSeries();
        int lIdx = idx;
        List<RenderedTagLine> newLines = new LinkedList<>();
        for (ITagController.TagLine line : c.getFor(tag.getTarget())) {
            added.put(lIdx);
            newLines.add(new RenderedTagLine(c, line, tag.getTarget(), renderer.createStack(tag, lIdx++)));
        }
        Collections.reverse(newLines);
        if (newLines.size() == 0) return null;

        lines.addAll(idx, newLines);

        int fIdx = idx;
        return players.map(p -> new AddTransaction(
                getBelow(fIdx - 1, p, added, null),
                getAbove(fIdx + newLines.size(), p, added, null),
                newLines,
                p,
                tag
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
        removed.forEach(line -> line.getStack().forEach(renderer::purge));

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
                lines.put(l, null);
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
            transactions.add(new MoveTransaction(
                    getBelow(r.getLower() - 1, player, added, removed),
                    getAbove(r.getUpper() + 1, player, added, removed),
                    ImmutableList.copyOf(subjectLines),
                    player,
                    true
            ));
            subjectLines.clear();
        }

        for (RangeSeries.Range r : added.getRanges()) {
            subjectLines = new LinkedList<>();
            for (int j : r) subjectLines.add(this.lines.get(j));
            transactions.add(new MoveTransaction(
                    getBelow(r.getLower() - 1, player, added, removed),
                    getAbove(r.getUpper() + 1, player, added, removed),
                    subjectLines,
                    player,
                    false
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
