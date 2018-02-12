package net.blitzcube.mlapi.util.packet.entity.object;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import org.bukkit.entity.EntityType;

import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Created by iso2013 on 8/24/2017.
 */
public class FakeObject extends FakeEntity {

    private int objectData;

    public FakeObject(EntityType type, WrappedDataWatcher metadata, int entityID, int objectData) {
        super(type, metadata, entityID);
        this.objectData = objectData;
    }

    public int getObjectData() {
        return objectData;
    }

}
