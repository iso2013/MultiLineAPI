package net.blitzcube.mlapi.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.blitzcube.mlapi.api.IFakeEntity;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class TagRender {
    
    private final List<IFakeEntity> removed = new ArrayList<>();
    private final List<IFakeEntity> entities = new ArrayList<>();

    public void addRemovedEntity(IFakeEntity entity) {
        this.entities.add(entity);
    }

    public void addRemovedEntities(Collection<IFakeEntity> entities) {
        this.entities.addAll(entities);
    }

    public List<IFakeEntity> getRemoved() {
        return removed;
    }

    public void addEntity(int position, IFakeEntity entity) {
        this.entities.add(position, entity);
    }

    public void addEntity(IFakeEntity entity) {
        this.entities.add(entity);
    }

    public List<IFakeEntity> getEntities() {
        return entities;
    }

}