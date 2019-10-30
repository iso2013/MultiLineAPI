package net.blitzcube.mlapi.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.blitzcube.mlapi.api.tag.ITagController;
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
    private final Multimap<Player, RenderedTagLine> visible;

    public TagStructure(Tag tag, Multimap<Player, RenderedTagLine> visible) {
        this.lines = new ArrayList<>();
        this.tag = tag;
        this.visible = visible;
        this.renderer = tag.getRenderer();
    }

    public Stream<Map.Entry<Player, Collection<StructureTransaction>>> addTagController(ITagController controller, Stream<Player> players) {
        int index = 0;
        for (int i = 0; i < lines.size(); i++) {
            int comp = CONTROLLER_COMPARATOR.compare(controller, lines.get(i).getController());

            if (comp > 0) {
                index = i + 1;
            }
        }

        RangeSeries added = new RangeSeries();
        int lineIndex = index;
        List<RenderedTagLine> newLines = new LinkedList<>();

        for (ITagController.TagLine line : controller.getFor(tag.getTarget())) {
            added.put(lineIndex);
            newLines.add(new RenderedTagLine(controller, line, tag.getTarget(), renderer.createStack(tag, lineIndex)));
            lineIndex++;
        }

        if (newLines.size() == 0) return Stream.empty();

        Collections.reverse(newLines);
        this.lines.addAll(index, newLines);

        int fIdx = index;
        Map<Player, Collection<StructureTransaction>> transactions = new HashMap<>();

        players.forEach(p -> transactions.put(p, Collections.singleton(new AddTransaction(
                getBelow(fIdx - 1, p, added, null),
                getAbove(fIdx + newLines.size(), p, added, null),
                newLines
        ))));

        return transactions.entrySet().stream();
    }

    public Stream<Map.Entry<Player, Collection<StructureTransaction>>> removeTagController(ITagController controller, Stream<Player> players) {
        int index = -1;
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).getController() == controller) {
                index = i;
                break;
            }

        if (index == -1) {
            return null;
        }

        List<RenderedTagLine> removed = lines.stream().filter(l -> l.getController() == controller).collect(Collectors.toList());
        this.lines.removeAll(removed);
        removed.forEach(line -> line.getStack().forEach(renderer::purge));

        int fIdx = index;
        Map<Player, Collection<StructureTransaction>> transactions = new HashMap<>();

        players.forEach(p -> transactions.put(p, Collections.singleton(new RemoveTransaction(
                getBelow(fIdx - 1, p, null, null),
                getAbove(fIdx, p, null, null),
                removed
        ))));

        return transactions.entrySet().stream();
    }

    public Collection<StructureTransaction> createUpdateTransactions(Predicate<RenderedTagLine> matcher, Player player) {
        List<StructureTransaction> transactions = new LinkedList<>();
        Map<RenderedTagLine, String> lineNames = new HashMap<>();

        RangeSeries added = new RangeSeries(), removed = new RangeSeries();
        RenderedTagLine line;
        for (int i = 0; i < lines.size(); i++) {
            line = this.lines.get(i);

            if (matcher != null && !matcher.test(line)) continue;
            boolean visible = this.visible.containsEntry(player, line);
            String newValue = line.get(player);

            if (newValue == null && visible && line.shouldRemoveSpaceWhenNull()) {
                removed.put(i);
                lineNames.put(line, null);
            } else if (newValue != null && !visible) {
                added.put(i);
                lineNames.put(line, newValue);
            } else {
                lineNames.put(line, newValue);
            }
        }

        transactions.add(new NameTransaction(lineNames));

        List<RenderedTagLine> subjectLines = new LinkedList<>();
        for (RangeSeries.Range range : removed.getRanges()) {
            for (int i : range) {
                subjectLines.add(this.lines.get(i));
            }

            transactions.add(new MoveTransaction(
                    getBelow(range.getLower() - 1, player, added, removed),
                    getAbove(range.getUpper() + 1, player, added, removed),
                    ImmutableList.copyOf(subjectLines),
                    true
            ));
            subjectLines.clear();
        }

        for (RangeSeries.Range range : added.getRanges()) {
            subjectLines = new LinkedList<>();
            for (int j : range) {
                subjectLines.add(this.lines.get(j));
            }

            transactions.add(new MoveTransaction(
                    getBelow(range.getLower() - 1, player, added, removed),
                    getAbove(range.getUpper() + 1, player, added, removed),
                    subjectLines,
                    false
            ));
        }

        return transactions;
    }

    private IEntityIdentifier getAbove(int idx, Player player, RangeSeries added, RangeSeries removed) {
        for (int i = idx; i < lines.size(); i++) {
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i)))
                return lines.get(i).getBottom();
        }

        return tag.getTop();
    }

    private IEntityIdentifier getBelow(int idx, Player player, RangeSeries added, RangeSeries removed) {
        for (int i = idx; i >= 0; i--) {
            if ((visible.containsEntry(player, lines.get(i))
                    && (removed == null || !removed.contains(i)))
                    || (added != null && added.contains(i)))
                return lines.get(i).getStack().getLast();
        }

        if (tag.getBottom() == null) return null;
        return tag.getBottom();
    }

    public ImmutableList<RenderedTagLine> getLines() {
        return ImmutableList.copyOf(lines);
    }
}
