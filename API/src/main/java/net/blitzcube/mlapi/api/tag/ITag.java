package net.blitzcube.mlapi.api.tag;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Created by iso2013 on 5/24/2018.
 */
public interface ITag {
    ImmutableList<ITagController> getTagControllers(boolean b);

    void addTagController(ITagController controller);

    void removeTagController(ITagController controller);

    void setVisible(Player target, boolean val);

    void clearVisible(Player target);

    Boolean isVisible(Player target);

    boolean getDefaultVisible();

    void setDefaultVisible(boolean val);

    void update(Player target);

    void update();

    void update(ITagController controller, Player target);

    void update(ITagController controller);

    void update(ITagController.TagLine line, Player target);

    void update(ITagController.TagLine line);

    void updateName(Player target);

    void updateName();

    Entity getTarget();
}
