package net.blitzcube.mlapi.packet.entities;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.blitzcube.mlapi.tag.TagLine;
import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.EntityType;

import java.util.HashMap;

/**
 * Created by iso2013 on 7/28/2017.
 */
public class SlimeEntity extends PacketUtil.FakeEntity {
    private TagLine tagLine;

    public SlimeEntity(int size) {
        super(EntityType.SLIME);
        super.setWatcher(PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
            put(0, (byte) 32);
            put(3, false);
            put(4, true);
            put(11, (byte) 11);
            put(12, size);
        }}));
    }

    public SlimeEntity(WrappedDataWatcher metadata, int size) {
        this(size);
        setWatcher(metadata);
    }

    public TagLine getTagLine() {
        return tagLine;
    }

    public void setTagLine(TagLine tagLine) {
        this.tagLine = tagLine;
    }

}