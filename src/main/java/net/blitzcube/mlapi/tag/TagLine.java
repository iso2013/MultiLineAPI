package net.blitzcube.mlapi.tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.entity.Player;

import net.blitzcube.mlapi.api.IFakeEntity;
import net.blitzcube.mlapi.api.tag.ITagLine;
import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Class by 2008Choco @ 2018.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class TagLine implements ITagLine {
    
    protected Map<UUID, FakeEntity> line = new HashMap<>();
    
    private String cached;
    private Function<Player, String> dynamicText;
    private boolean keepSpaceWhenNull;
    
    public TagLine(String cached, boolean keepSpaceWhenNull, Function<Player, String> dynamicText) {
        this.cached = cached;
        this.keepSpaceWhenNull = keepSpaceWhenNull;
        this.dynamicText = dynamicText;
    }
    
    public TagLine(String cached, boolean keepSpaceWhenNull) {
        this(cached, keepSpaceWhenNull, p -> cached);
    }
    
    public TagLine(String cached, Function<Player, String> dynamicText) {
        this(cached, false, dynamicText);
    }
    
    public TagLine(String cached) {
        this(cached, false, p -> cached);
    }

    @Override
    public String getCached() {
        return cached;
    }

    @Override
    public void setCached(String cached) {
        this.cached = cached;
    }

    @Override
    public String getText(Player forWho) {
        return dynamicText.apply(forWho);
    }

    @Override
    public boolean keepSpaceWhenNull() {
        return keepSpaceWhenNull;
    }

    public void setLine(UUID uuid, FakeEntity entity) {
        this.line.put(uuid, entity);
    }

    public IFakeEntity removeLine(UUID uuid) {
        return this.line.remove(uuid);
    }

    public FakeEntity getLine(UUID uuid) {
        return line.get(uuid);
    }

}
