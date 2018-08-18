package net.blitzcube.mlapi.tag;

import com.google.common.collect.ImmutableList;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.TagStructure;
import net.blitzcube.mlapi.util.SortedList;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class Tag implements ITag {

    // The renderer
    private final TagRenderer renderer;

    // Target information
    private final Entity target;

    // Structure information
    private final TagStructure structure;

    // Tag controller information
    private final SortedList<ITagController> sortedControllers;
    // Bottom and top entities
    private final IFakeEntity bottom;
    private final IFakeEntity top;
    private final VisibilityStates state;
    // Default visibility state
    private boolean defaultVisible = true;

    private final IHitbox hitbox;

    public Tag(Entity target, TagRenderer renderer, Collection<ITagController> controllers, LineEntityFactory lineFactory, VisibilityStates state) {
        // Constructor parameters
        this.target = target;
        this.renderer = renderer;
        this.state = state;

        // Sorted collections
        this.structure = new TagStructure(this, state.getVisibilityMap());
        this.sortedControllers = new SortedList<>(Comparator.comparingInt(ITagController::getNamePriority));

        // Bottom and top of stack
        this.bottom = renderer.createBottom(this);
        this.top = renderer.createTop(this);
        this.hitbox = lineFactory.getHitbox(target);

        controllers.forEach(this::addTagController);
    }

    @Override
    public ImmutableList<ITagController> getTagControllers(boolean sortByLines) {
        if (sortByLines) {
            return structure.getLines().stream().map(RenderedTagLine::getController).distinct().collect(ImmutableList.toImmutableList());
        }

        return ImmutableList.copyOf(sortedControllers);
    }

    @Override
    public void addTagController(ITagController controller) {
        this.sortedControllers.add(controller);
        Tag tag = this;

        this.structure.addTagController(controller, renderer.getNearby(this, 1.0))
            .forEach(e -> renderer.processTransactions(e.getValue(), tag, e.getKey()));
    }

    @Override
    public void removeTagController(ITagController controller) {
        if (!sortedControllers.contains(controller)) return;

        this.sortedControllers.remove(controller);
        Tag tag = this;

        this.structure.removeTagController(controller, (target.getTicksLived() > 0) ? renderer.getNearby(this, 1.0) : Stream.empty())
            .forEach(e -> renderer.processTransactions(e.getValue(), tag, e.getKey()));
    }

    @Override
    public void setVisible(Player target, boolean visible) {
        this.state.setVisible(this, target, visible);

        if (state.isSpawned(target, this) && !visible) {
            this.renderer.destroyTag(this, target, null);
        } else if (!state.isSpawned(target, this) && visible) {
            this.renderer.spawnTag(this, target, null);
        }
    }

    @Override
    public void clearVisible(Player target) {
        Boolean oldVal = state.isVisible(this, target);

        if (oldVal != null && oldVal && state.isSpawned(target, this) && !defaultVisible) {
            this.renderer.destroyTag(this, target, null);
        } else if (oldVal != null && !oldVal && state.isSpawned(target, this) && defaultVisible) {
            this.renderer.spawnTag(this, target, null);
        }

        this.state.setVisible(this, target, null);
    }

    @Override
    public Boolean isVisible(Player target) {
        return state.isVisible(this, target);
    }

    @Override
    public boolean getDefaultVisible() {
        return defaultVisible;
    }

    @Override
    public void setDefaultVisible(boolean val) {
        Tag tag = this;
        Stream<Player> playerStream = renderer.getNearby(this, 1.0).filter(player -> state.isVisible(tag, player) == null);

        if (val && !defaultVisible) {
            playerStream.filter(player -> !state.isSpawned(player, tag)).forEach(player -> renderer.spawnTag(tag, player, null));
        } else if (!val && defaultVisible) {
            playerStream.filter(player -> state.isSpawned(player, tag)).forEach(player -> renderer.destroyTag(tag, player, null));
        }

        this.defaultVisible = val;
    }

    @Override
    public void update(Player target) {
        if (this.getTarget().getPassengers().size() > 0) return;
        Boolean visible = state.isVisible(this, target);

        if ((visible == null && !defaultVisible) || (visible != null && !visible)) return;
        this.renderer.processTransactions(structure.createUpdateTransactions(l -> true, target), this, target);
    }

    @Override
    public void update() {
        this.renderer.getNearby(this, 1.0).forEach(this::update);
    }

    @Override
    public void update(ITagController c, Player target) {
        if (!this.sortedControllers.contains(c)) return;
        if (this.getTarget().getPassengers().size() > 0 || target.getGameMode() == GameMode.SPECTATOR) return;

        Boolean visible = state.isVisible(this, target);
        if ((visible == null && !defaultVisible) || (visible != null && !visible)) return;

        this.renderer.processTransactions(structure.createUpdateTransactions(l -> l.getController().equals(c), target), this, target);
    }

    @Override
    public void update(ITagController controller) {
        if (!sortedControllers.contains(controller)) return;
        this.renderer.getNearby(this, 1.0).forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        if (this.getTarget().getPassengers().size() > 0 || target.getGameMode() == GameMode.SPECTATOR) return;
        Boolean visible = state.isVisible(this, target);

        if ((visible == null && !this.defaultVisible) || (visible != null && !visible)) return;
        this.renderer.processTransactions(structure.createUpdateTransactions(l -> l.isRenderedBy(line), target), this, target);
    }

    @Override
    public void update(ITagController.TagLine line) {
        this.renderer.getNearby(this, 1.0).forEach(p -> update(line, p));
    }

    @Override
    public void updateName(Player viewer) {
        this.renderer.updateName(this, viewer);
    }

    @Override
    public void updateName() {
        this.renderer.getNearby(this, 1.0).forEach(this::updateName);
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

    public TagRenderer getRenderer() {
        return renderer;
    }
}
