package net.blitzcube.mlapi.api;

import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;

/**
 * Created by iso2013 on 5/23/2018.
 */
public interface IMultiLineAPI {
    ITag getTag(Entity entity);

    ITag createTagIfMissing(Entity entity);

    void deleteTag(Entity entity);

    boolean hasTag(Entity entity);

    void addDefaultTagController(ITagController val);

    void removeDefaultTagController(ITagController val);

    Set<ITagController> getDefaultTagControllers();

    Collection<ITagController> getDefaultTagControllers(EntityType type);

    void update(Entity entity, Player target);

    void update(Entity entity);

    void update(ITagController controller, Player target);

    void update(ITagController controller);

    void update(ITagController.TagLine line, Player target);

    void update(ITagController.TagLine line);

    void updateNames(Player target);

    void updateNames();
}
