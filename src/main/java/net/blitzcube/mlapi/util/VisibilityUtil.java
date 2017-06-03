package net.blitzcube.mlapi.util;

import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import java.util.List;

/**
 * Created by iso2013 on 6/3/2017.
 */
public class VisibilityUtil {
    public static boolean isMetadataInvisible(List<WrappedWatchableObject> metadata) {
        WrappedWatchableObject ob = metadata.stream().filter(wrappedWatchableObject -> wrappedWatchableObject
                .getIndex() == 0).findAny().orElse(null);
        if (ob == null) return false;
        return (((Byte) ob.getRawValue()) & 0x20) > 0;
    }
}
