package net.blitzcube.mlapi.tag;

import com.google.common.collect.ImmutableList;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.util.RangeSeries;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class Tag implements ITag {
    //Order comparator
    private final static Comparator<ITagController> controllerComparator =
            Comparator.comparingInt(ITagController::getPriority);
    //The renderer
    private final TagRenderer renderer;

    //Target information
    private final Entity target;

    //Lines
    private final List<RenderedTagLine> lines;

    //Tag controller information
    private final SortedSet<ITagController> sortedControllers;
    //Bottom and top entities
    private final IFakeEntity bottom;
    private final IFakeEntity top;
    //Default visibility state
    private boolean defaultVisible = true;

    public Tag(Entity target, TagRenderer renderer, Collection<ITagController> controllers) {
        //Constructor parameters
        this.target = target;
        this.renderer = renderer;

        //Sorted collections
        this.lines = new ArrayList<>();
        this.sortedControllers = new TreeSet<>(Comparator.comparingInt(ITagController::getNamePriority));

        //Bottom and top of stack
        bottom = renderer.getLineEntityFactory().createSilverfish(target.getLocation());
        top = renderer.getLineEntityFactory().createArmorStand(target.getLocation());

        controllers.forEach(this::addTagController);
    }

    @Override
    public ImmutableList<ITagController> getTagControllers(boolean sortByLines) {
        if (sortByLines) {
            return lines.stream().map(RenderedTagLine::getController).distinct().collect(ImmutableList
                    .toImmutableList());
        }
        return ImmutableList.copyOf(sortedControllers);
    }

    @Override
    public void addTagController(ITagController c) {
        //Get the lines from the controller
        List<RenderedTagLine> newLines =
                c.getFor(target).stream()
                        .map(l -> new RenderedTagLine(c, l, target, renderer.getLineEntityFactory()))
                        .collect(Collectors.toList());
        if (newLines.size() == 0) return;
        //Add the controller to the sorted set
        sortedControllers.add(c);
        //Insert lines at the proper position in the array
        int idx = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (controllerComparator.compare(c, lines.get(i).getController()) < 0) idx = i;
        }
        lines.addAll(
                idx,
                newLines
        );
        //Determine if updating is necessary
        Set<Player> nearby;
        if (!(nearby = renderer.getNearby(this, 1.0).collect(Collectors.toSet())).isEmpty()) {
            renderer.renderStructureChange(idx, newLines, null, this, nearby, 0);
        }
    }

    @Override
    public void removeTagController(ITagController c) {
        if (!sortedControllers.contains(c)) return;
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getController() == c) {
                idx = i;
                break;
            }
        }
        List<RenderedTagLine> removed = lines.stream().filter(l -> l.getController() == c).collect(Collectors.toList());
        lines.removeAll(removed);

        Set<Player> nearby;
        if (!(nearby = renderer.getNearby(this, 1.0).collect(Collectors.toSet())).isEmpty()) {
            renderer.renderStructureChange(idx, null, removed, this, nearby, 0);
        }
    }

    @Override
    public void setVisible(Player target, boolean val) {
        renderer.setVisible(this, target, val);
    }

    @Override
    public void clearVisible(Player target) {
        renderer.setVisible(this, target, null);
    }

    @Override
    public Boolean isVisible(Player target) {
        return renderer.isVisible(this, target);
    }

    @Override
    public boolean getDefaultVisible() {
        return this.defaultVisible;
    }

    @Override
    public void setDefaultVisible(boolean val) {
        this.defaultVisible = val;
    }

    @Override
    public void update(Player target) {
        if (!renderer.isVisible(this, target)) return;
        RangeSeries removed = new RangeSeries(), added = new RangeSeries();
        Map<Integer, String> newNames = new HashMap<>();
        for (int i = 0; i < this.lines.size(); i++) {
            RenderedTagLine l = this.lines.get(i);
            boolean v = renderer.isShown(target, l);
            String newVal = l.get(target);
            if (newVal == null && v && l.shouldRemoveSpaceWhenNull()) {
                removed.put(i);
            } else if (newVal != null && !v) {
                added.put(i);
                renderer.queueName(l, newVal);
            } else {
                newNames.put(i, newVal);
            }
        }
        Set<Player> targets = Collections.singleton(target);
        updateInternal(added, removed, newNames, targets);
    }

    @Override
    public void update() {
        renderer.getNearby(this, 1.0).forEach(this::update);
    }

    @Override
    public void update(ITagController controller, Player target) {
        if (!renderer.isVisible(this, target)) return;
        RangeSeries removed = new RangeSeries(), added = new RangeSeries();
        Map<Integer, String> newNames = new HashMap<>();
        for (int i = 0; i < this.lines.size(); i++) {
            RenderedTagLine l = this.lines.get(i);
            if (l.getController() != controller) continue;
            boolean v = renderer.isShown(target, l);
            String newVal = l.get(target);
            if (newVal == null && v && l.shouldRemoveSpaceWhenNull()) {
                removed.put(i);
            } else if (newVal != null && !v) {
                added.put(i);
                renderer.queueName(l, newVal);
            } else {
                newNames.put(i, newVal);
            }
        }
        Set<Player> targets = Collections.singleton(target);
        updateInternal(added, removed, newNames, targets);
    }

    @Override
    public void update(ITagController controller) {
        renderer.getNearby(this, 1.0).forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        if (!renderer.isVisible(this, target)) return;
        RenderedTagLine l = null;
        int idx = -1;
        for (int i = 0; i < this.lines.size(); i++) {
            if ((l = this.lines.get(i)).isRenderedBy(line)) {
                idx = i;
                break;
            }
        }
        if (idx == -1)
            throw new IllegalArgumentException("Cannot update a line object that is not registered to this tag!");
        boolean v = renderer.isShown(target, l);
        String newVal = l.get(target);
        if (newVal == null && v && l.shouldRemoveSpaceWhenNull()) {
            renderer.renderStructureChange(idx, null, l, this, Collections.singleton(target));
        } else if (newVal != null && !v) {
            renderer.queueName(l, newVal);
            renderer.renderStructureChange(idx, l, null, this, Collections.singleton(target));
        } else {
            renderer.renderLineChange(l, newVal, Collections.singleton(target));
        }
    }

    @Override
    public void update(ITagController.TagLine line) {
        renderer.getNearby(this, 1.0).forEach(p -> update(line, p));
    }

    private void updateInternal(RangeSeries added, RangeSeries removed, Map<Integer, String> newNames, Set<Player>
            targets) {
        List<RenderedTagLine> subjectLines = new ArrayList<>();
        for (RangeSeries.Range r : removed.getRanges()) {
            for (int j : r) subjectLines.add(this.lines.get(j));
            renderer.renderStructureChange(r.getLower(), null, subjectLines, this,
                    targets, r.size());
            subjectLines.clear();
        }
        for (RangeSeries.Range r : added.getRanges()) {
            for (int j : r) subjectLines.add(this.lines.get(j));
            renderer.renderStructureChange(r.getLower(), subjectLines, null, this, targets, 0);
            subjectLines.clear();
        }
        for (Map.Entry<Integer, String> e : newNames.entrySet())
            renderer.renderLineChange(this.lines.get(e.getKey()), e.getValue(), targets);
    }


    @Override
    public Entity getTarget() {
        return target;
    }

    public IFakeEntity getBottom() {
        return bottom;
    }

    public IFakeEntity getTop() {
        return top;
    }

    public List<RenderedTagLine> getLines() {
        return lines;
    }
}
