package net.blitzcube.mlapi.tag;

import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.LinkedList;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class RenderedTagLine {
    private final LinkedList<IFakeEntity> stack;
    private final IFakeEntity bottom;

    private final ITagController controller;
    private final ITagController.TagLine lineGenerator;
    private final Entity target;
    private final boolean spaceWhenNull;

    public RenderedTagLine(ITagController controller, ITagController.TagLine tagLine, Entity target,
                           LineEntityFactory factory) {
        this.controller = controller;
        this.lineGenerator = tagLine;
        this.target = target;
        this.spaceWhenNull = tagLine.keepSpaceWhenNull(target);

        this.stack = new LinkedList<>();
        this.bottom = factory.createArmorStand(target.getLocation(), target);
        this.stack.add(bottom);
        this.stack.add(factory.createSlime(target.getLocation(), target));
        this.stack.add(factory.createSilverfish(target.getLocation(), target));
        this.stack.add(factory.createSilverfish(target.getLocation(), target));
    }

    public String get(Player viewer) {
        return lineGenerator.getText(target, viewer);
    }

    public IFakeEntity getBottom() {
        return bottom;
    }

    public LinkedList<IFakeEntity> getStack() {
        return stack;
    }

    public ITagController getController() {
        return controller;
    }

    public boolean isRenderedBy(ITagController.TagLine line) {
        return this.lineGenerator == line;
    }

    public boolean shouldRemoveSpaceWhenNull() {
        return !spaceWhenNull;
    }
}
