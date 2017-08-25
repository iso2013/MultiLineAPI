package net.blitzcube.mlapi.util.packet.entity.object;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.blitzcube.mlapi.util.packet.entity.FakeEntity;
import org.bukkit.entity.EntityType;

/**
 * Created by iso2013 on 8/24/2017.
 */
public class FakeObject extends FakeEntity {
    public FakeObject(EntityType type, WrappedDataWatcher metadata, int entityID) {
        super(type, metadata, entityID);
    }
}
