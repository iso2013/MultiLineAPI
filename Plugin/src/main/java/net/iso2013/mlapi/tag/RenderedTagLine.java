package net.iso2013.mlapi.tag;

import net.iso2013.mlapi.api.tag.TagController;
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

    private final TagController controller;
    private final TagController.TagLine lineGenerator;
    private final Entity target;
    private final boolean spaceWhenNull;

    public RenderedTagLine(TagController controller, TagController.TagLine tagLine, Entity target,
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

    public TagController getController() {
        return controller;
    }

    boolean isRenderedBy(TagController.TagLine line) {
        return this.lineGenerator == line;
    }

    public boolean shouldRemoveSpaceWhenNull() {
        return !spaceWhenNull;
    }
}
