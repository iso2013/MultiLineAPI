package net.blitzcube.mlapi.packet.entities;

import java.util.HashMap;
import java.util.Map;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import org.bukkit.entity.EntityType;

import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.PacketUtil;
import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class SilverfishEntity extends FakeEntity {

    private static final Map<Integer, Object> DEFAULT_METADATA = new HashMap<>();

    static {
        DEFAULT_METADATA.put(0, (byte) 32);
        DEFAULT_METADATA.put(3, false);
        DEFAULT_METADATA.put(4, true);
        DEFAULT_METADATA.put(11, (byte) 1);
    }

    private TagLine tagLine;

    public SilverfishEntity() {
        super(EntityType.SILVERFISH, PacketUtil.createWatcher(new HashMap<>(DEFAULT_METADATA)), EntityType.SILVERFISH.getTypeId());
    }

    public SilverfishEntity(WrappedDataWatcher metadata) {
        super(EntityType.SILVERFISH, metadata, EntityType.SILVERFISH.getTypeId());
    }

    public TagLine getTagLine() {
        return tagLine;
    }

    public void setTagLine(TagLine tagLine) {
        this.tagLine = tagLine;
    }
}
