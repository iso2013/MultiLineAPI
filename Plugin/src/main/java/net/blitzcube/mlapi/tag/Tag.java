package net.blitzcube.mlapi.tag;

import com.google.common.collect.ImmutableList;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.TagStructure;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class Tag implements ITag {
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
    private final VisibilityStates state;
    //Default visibility state
    private boolean defaultVisible = true;

    private final IHitbox hitbox;

    public Tag(Entity target, TagRenderer renderer, Collection<ITagController> controllers, LineEntityFactory
            lineFactory, VisibilityStates state) {
        //Constructor parameters
        this.target = target;
        this.renderer = renderer;
        this.state = state;

        //Sorted collections
        this.structure = new TagStructure(this, state.getVisibilityMap());
        this.sortedControllers = new TreeSet<>(Comparator.comparingInt(ITagController::getNamePriority));

        //Bottom and top of stack
        bottom = this.renderer.createBottom(this);
        top = this.renderer.createTop(this);

        hitbox = lineFactory.getHitbox(target);

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
        Tag t = this;
        Bukkit.broadcastMessage("TICKS LIVED: " + this.target.getTicksLived());
        structure.addTagController(c,
                this.target.getTicksLived() > 0 ? renderer.getNearby(this, 1.0) : Stream.empty()
        ).forEach(e -> renderer.processTransactions(e.getValue(), t, e.getKey()));
    }

    @Override
    public void removeTagController(ITagController c) {
        if (!sortedControllers.contains(c)) return;
        sortedControllers.remove(c);
        Tag t = this;
        structure.removeTagController(c,
                this.target.getTicksLived() > 0 ? renderer.getNearby(this, 1.0) : Stream.empty()
        ).forEach(e -> renderer.processTransactions(e.getValue(), t, e.getKey()));
    }

    @Override
    public void setVisible(Player target, boolean val) {
        state.setVisible(this, target, val);
        if (state.isSpawned(target, this) && !val) {
            renderer.destroyTag(this, target, null);
        } else if (!state.isSpawned(target, this) && val) {
            renderer.spawnTag(this, target, null);
        }
    }

    @Override
    public void clearVisible(Player target) {
        Boolean oldVal = state.isVisible(this, target);
        if (oldVal != null && oldVal && state.isSpawned(target, this) && !this.defaultVisible) {
            renderer.destroyTag(this, target, null);
        } else if (oldVal != null && !oldVal && state.isSpawned(target, this) && this.defaultVisible) {
            renderer.spawnTag(this, target, null);
        }
        state.setVisible(this, target, null);
    }

    @Override
    public Boolean isVisible(Player target) {
        return state.isVisible(this, target);
    }

    @Override
    public boolean getDefaultVisible() {
        return this.defaultVisible;
    }

    @Override
    public void setDefaultVisible(boolean val) {
        Tag t = this;
        Stream<Player> ps = renderer.getNearby(this, 1.0).filter(player -> state.isVisible(t, player) == null);
        if (val && !this.defaultVisible) {
            ps.filter(player -> !state.isSpawned(player, t)).forEach(player -> renderer.spawnTag(t, player, null));
        } else if (!val && this.defaultVisible) {
            ps.filter(player -> state.isSpawned(player, t)).forEach(player -> renderer.destroyTag(t, player, null));
        }
        this.defaultVisible = val;
    }

    @Override
    public void update(Player target) {
        if (this.getTarget().getPassengers().size() > 0) return;
        Boolean b = state.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        renderer.processTransactions(structure.createUpdateTransactions(l -> true, target), this, target);
    }

    @Override
    public void update() {
        renderer.getNearby(this, 1.0).forEach(this::update);
    }

    @Override
    public void update(ITagController c, Player target) {
        if (!this.sortedControllers.contains(c)) return;
        if (this.getTarget().getPassengers().size() > 0 || target.getGameMode() == GameMode.SPECTATOR) return;
        Boolean b = state.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        renderer.processTransactions(structure.createUpdateTransactions(l -> l.getController().equals(c), target),
                this, target);
    }

    @Override
    public void update(ITagController controller) {
        if (!this.sortedControllers.contains(controller)) return;
        renderer.getNearby(this, 1.0).forEach(p -> update(controller, p));
    }

    @Override
    public void update(ITagController.TagLine line, Player target) {
        if (this.getTarget().getPassengers().size() > 0 || target.getGameMode() == GameMode.SPECTATOR) return;
        Boolean b = state.isVisible(this, target);
        if ((b == null && !this.defaultVisible) || (b != null && !b)) return;
        renderer.processTransactions(structure.createUpdateTransactions(l -> l.isRenderedBy(line), target), this, target);
    }

    @Override
    public void update(ITagController.TagLine line) {
        renderer.getNearby(this, 1.0).forEach(p -> update(line, p));
    }

    @Override
    public void updateName(Player viewer) {
        renderer.updateName(this, viewer);
    }

    @Override
    public void updateName() {
        renderer.getNearby(this, 1.0).forEach(this::updateName);
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
