package net.blitzcube.mlapi.tag;

import com.google.common.collect.ImmutableList;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.TagStructure;
import net.blitzcube.mlapi.structure.transactions.StructureTransaction;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

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

    //Structure information
    private final TagStructure structure;

    //Tag controller information
    private final SortedSet<ITagController> sortedControllers;
    //Bottom and top entities
    private final IFakeEntity bottom;
    private final IFakeEntity top;
    //Default visibility state
    private boolean defaultVisible = true;

    private final IHitbox hitbox;

    public Tag(Entity target, TagRenderer renderer, Collection<ITagController> controllers) {
        //Constructor parameters
        this.target = target;
        this.renderer = renderer;

        //Sorted collections
        this.structure = new TagStructure(this, renderer.getLineEntityFactory(), renderer.getVisibilityMap());
        this.sortedControllers = new TreeSet<>(Comparator.comparingInt(ITagController::getNamePriority));

        //Bottom and top of stack
        bottom = renderer.getLineEntityFactory().createSilverfish(target.getLocation(), target);
        top = renderer.getLineEntityFactory().createArmorStand(target.getLocation(), target);

        hitbox = renderer.getLineEntityFactory().getHitbox(target);

        controllers.forEach(this::addTagController);
    }

    @Override
    public ImmutableList<ITagController> getTagControllers(boolean sortByLines) {
        if (sortByLines) {
            return structure.getLines().stream().map(RenderedTagLine::getController).distinct().collect(ImmutableList
                    .toImmutableList());
        }
        return ImmutableList.copyOf(sortedControllers);
    }

    @Override
    public void addTagController(ITagController c) {
        sortedControllers.add(c);
        Stream<StructureTransaction> s = structure.addTagController(c, renderer.getNearby(this, 1.0));
        if (s != null) s.forEach(renderer::processTransaction);
    }

    @Override
    public void removeTagController(ITagController c) {
        if (!sortedControllers.contains(c)) return;
        sortedControllers.remove(c);
        Stream<StructureTransaction> s = structure.removeTagController(c, renderer.getNearby(this, 1.0));
        if (s != null) s.forEach(renderer::processTransaction);
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
        if (this.getTarget().getPassengers().size() > 0) return;
        Boolean b = renderer.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        structure.createUpdateTransactions(l -> true, target).forEach(renderer::processTransaction);
    }

    @Override
    public void update() {
        renderer.getNearby(this, 1.0).forEach(this::update);
    }

    @Override
    public void update(ITagController c, Player target) {
        if (this.getTarget().getPassengers().size() > 0) return;
        Boolean b = renderer.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        structure.createUpdateTransactions(l -> l.getController().equals(c), target).forEach
                (renderer::processTransaction);
    }

    @Override
    public void update(ITagController controller) {
        renderer.getNearby(this, 1.0).forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        if (this.getTarget().getPassengers().size() > 0) return;
        Boolean b = renderer.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        structure.createUpdateTransactions(l -> l.isRenderedBy(line), target).forEach(renderer::processTransaction);
    }

    @Override
    public void update(ITagController.TagLine line) {
        renderer.getNearby(this, 1.0).forEach(p -> update(line, p));
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
        return structure.getLines();
    }

    public IHitbox getTargetHitbox() {
        return hitbox;
    }
}
