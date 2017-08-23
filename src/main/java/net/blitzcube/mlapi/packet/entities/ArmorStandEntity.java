package net.blitzcube.mlapi.packet.entities;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class ArmorStandEntity extends PacketUtil.FakeEntity {
    private Map<Integer, Object> pending = new HashMap<>();
    private TagLine tagLine;

    public ArmorStandEntity() {
        super(EntityType.ARMOR_STAND);
        pending.put(0, (byte) 32);
        pending.put(4, true);
        pending.put(11, (byte) 16);
        pushMetadata();
    }

    public ArmorStandEntity(WrappedDataWatcher metadata) {
        super(EntityType.ARMOR_STAND, metadata);
        pending.put(0, (byte) 32);
        pending.put(4, true);
        pending.put(11, (byte) 16);
        pushMetadata();
    }

    public void setCustomNameVisible(boolean visible) {
        pending.put(3, visible);
    }

    public void setCustomName(String name) {
        pending.put(2, name);
    }

    public void pushMetadata() {
        super.getWatcher().asMap().forEach((i, wWO) -> {
            if (!pending.containsKey(i)) wWO.getRawValue();
        });
    }

    public TagLine getTagLine() {
        return tagLine;
    }

    public void setTagLine(TagLine tagLine) {
        this.tagLine = tagLine;
    }
}
