package net.blitzcube.mlapi.packet.entities;

import java.util.HashMap;
import java.util.Map;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import org.bukkit.entity.EntityType;

import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class ArmorStandEntity extends FakeEntity {

    private static final Map<Integer, Object> DEFAULT_METADATA = new HashMap<>();

    static {
        DEFAULT_METADATA.put(0, (byte) 32);
        DEFAULT_METADATA.put(4, true);
        DEFAULT_METADATA.put(11, (byte) 16);
    }
    
    private TagLine tagLine;

    public ArmorStandEntity() {
        super(EntityType.ARMOR_STAND, new WrappedDataWatcher(), EntityType.ARMOR_STAND.getTypeId());

        this.pendingChanges.putAll(DEFAULT_METADATA);
        this.pushMetadata();
    }

    public ArmorStandEntity(WrappedDataWatcher metadata) {
        super(EntityType.ARMOR_STAND, metadata, EntityType.ARMOR_STAND.getTypeId());

        this.pendingChanges.putAll(DEFAULT_METADATA);
        this.pushMetadata();
    }

    public void setCustomNameVisible(boolean visible) {
        this.pendingChanges.put(3, visible);
    }

    public void setCustomName(String name) {
        this.pendingChanges.put(2, name);
    }

    public void pushMetadata() {
        this.metadata.asMap().forEach((i, wWO) -> {
            if (!pendingChanges.containsKey(i)) wWO.getRawValue();
        });
    }

    public TagLine getTagLine() {
        return tagLine;
    }

    public void setTagLine(TagLine tagLine) {
        this.tagLine = tagLine;
    }

}
