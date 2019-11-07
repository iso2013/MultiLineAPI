package net.blitzcube.mlapi.tag;

import net.blitzcube.mlapi.api.tag.ITagController;
import net.iso2013.peapi.api.entity.fake.FakeEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class RenderedTagLine {
    private final LinkedList<FakeEntity> stack;
    private final FakeEntity bottom;

    private final ITagController controller;
    private final ITagController.TagLine lineGenerator;
    private final Entity target;
    private final boolean spaceWhenNull;

    public RenderedTagLine(ITagController controller, ITagController.TagLine tagLine, Entity target,
                           LinkedList<FakeEntity> stack) {
        this.controller = controller;
        this.lineGenerator = tagLine;
        this.target = target;
        this.spaceWhenNull = tagLine.keepSpaceWhenNull(target);

        this.stack = stack;
        this.bottom = stack.getFirst();
    }

    public String get(Player viewer) {
        return lineGenerator.getText(target, viewer);
    }

    public FakeEntity getBottom() {
        return bottom;
    }

    public Deque<FakeEntity> getStack() {
        return stack;
    }

    public ITagController getController() {
        return controller;
    }

    boolean isRenderedBy(ITagController.TagLine line) {
        return this.lineGenerator == line;
    }

    public boolean shouldRemoveSpaceWhenNull() {
        return !spaceWhenNull;
    }
}
