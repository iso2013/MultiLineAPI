package net.blitzcube.mlapi.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.blitzcube.mlapi.api.IFakeEntity;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.api.tag.ITagLine;
import net.blitzcube.mlapi.packet.entities.ArmorStandEntity;
import net.blitzcube.mlapi.packet.entities.SilverfishEntity;
import net.blitzcube.mlapi.packet.entities.SlimeEntity;
import net.blitzcube.mlapi.util.PacketUtil;
import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */
public class Tag implements ITag {

    private static final Map<Integer, Object> ARMOR_STAND = ImmutableMap.of(
            0, (byte) 32,
            4, true,
            11, (byte) 16
    );

    private final List<ITagController> tagControllers = new ArrayList<>();
    private final List<IFakeEntity> base  = new ArrayList<>();
    private final List<IFakeEntity> entities  = new ArrayList<>();
    private final List<ITagLine> lines  = new ArrayList<>();
    private final Map<UUID, Boolean> hiddenFor = new HashMap<>();
    
    private IFakeEntity name;
    private boolean hiddenForAll;
    private int baseID;

    public Tag() {
        this.base.add(createSilverfish());
    }

    private static WrappedDataWatcher createArmorStandWatcher(String name, boolean visible) {
        Map<Integer, Object> data = new HashMap<>();
        data.putAll(ARMOR_STAND);
        data.put(2, name);
        data.put(3, visible);
        
        return PacketUtil.createWatcher(data);
    }

    private List<ITagLine> getLines(Entity forWhat, Player forWho) {
        List<ITagLine> tagLines = new LinkedList<>();
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));

        for (ITagController tc : tagControllers) {
            for (ITagLine line : tc.getLines(forWhat)) {
                line.setCached(line.getText(forWho));

                if (line.keepSpaceWhenNull() || line.getCached() != null) {
                    tagLines.add(line);
                }
            }
        }

        Collections.reverse(tagLines);
        return tagLines;
    }

    public TagRender render(Entity forWhat, Player forWho) {
        TagRender render = new TagRender();
        if (forWhat == null) return render;

        List<ITagLine> newLines = getLines(forWhat, forWho);
        this.lines.removeAll(newLines);
        boolean[] created = {false};

        newLines.stream().filter((l) -> ((TagLine) l).getLine(forWhat.getUniqueId()) == null).forEach(l -> {
        	((TagLine) l).setLine(forWhat.getUniqueId(), createArmorStand());
            created[0] = true;
        });

        this.lines.forEach(l -> render.addRemovedEntity(((TagLine) l).removeLine(forWhat.getUniqueId())));
        this.lines.clear();
        this.lines.addAll(newLines);

        if (render.getRemoved().size() > 0 || created[0]) {
            this.entities.removeAll(base);
            IFakeEntity nameLine = null;
            Iterator<IFakeEntity> sIterator = entities.iterator();

            while (sIterator.hasNext()) {
                IFakeEntity s = sIterator.next();

                if (s.getType().equals(EntityType.ARMOR_STAND)) {
                    if (newLines.size() < 1) {
                        render.addEntity(s);
                        nameLine = s;
                        sIterator.remove();
                        break;
                    }

                    render.addEntity(((TagLine) newLines.remove(0)).getLine(forWhat.getUniqueId()));
                    render.addRemovedEntity(s);
                }
                else {
                    render.addRemovedEntity(s);
                }

                sIterator.remove();
            }

            for (ITagLine line : newLines) {
                render.addEntity(createSilverfish());
                render.addEntity(createSilverfish());
                render.addEntity(createSlime());
                render.addEntity(((TagLine) line).getLine(forWhat.getUniqueId()));
            }

            if (nameLine == null) {
                nameLine = createArmorStand();
                render.addEntity(createSilverfish());
                render.addEntity(createSilverfish());
                render.addEntity(createSlime());
                render.addEntity(nameLine);
            }

            for (IFakeEntity e : base) {
                render.addEntity(0, e);
            }

            render.addRemovedEntities(entities);

            this.entities.clear();
            this.entities.addAll(render.getEntities());
            this.name = nameLine;
        }

        for (ITagLine t : this.lines) {
            ((TagLine) t).getLine(forWhat.getUniqueId()).setMetadata(createArmorStandWatcher(t.getCached() != null ? t.getCached()
                    : "", t.getCached() != null));
        }

        ((FakeEntity) this.name).setMetadata(createArmorStandWatcher(
                getName((forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER) ?
                        forWhat.getCustomName() : forWhat.getName(), forWhat, forWho),
                forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER)
        ));

        return render;
    }

    @Override
    public Set<PacketContainer> refreshLines(Entity forWhat, Player forWho) {
        Set<PacketContainer> packets = new HashSet<>();

        for (ITagLine l : lines) {
            if (((TagLine) l).getLine(forWhat.getUniqueId()) == null) continue;

            l.setCached(l.getText(forWho));
            ((TagLine) l).getLine(forWhat.getUniqueId()).setMetadata(createArmorStandWatcher(l.getCached() != null ? l.getCached() : "", l.getCached() != null));
            packets.add(PacketUtil.getMetadataPacket(((TagLine) l).getLine(forWhat.getUniqueId())));
        }

        return packets;
    }

    @Override
    public PacketContainer refreshName(Entity forWhat, Player forWho) {
        if (this.name == null) return null;

        ((FakeEntity) this.name).setMetadata(createArmorStandWatcher(
                getName((forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER) ?
                        forWhat.getCustomName() : forWhat.getName(), forWhat, forWho),
                forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER)
        ));

        return PacketUtil.getMetadataPacket((FakeEntity) name);
    }

    private String getName(String entityName, Entity forWhat, Player forWho) {
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getNamePriority(), o1.getNamePriority()));

        for (ITagController t : tagControllers) {
            entityName = t.getName(forWhat, forWho).replace("`PREV`", entityName);
        }

        return entityName;
    }

    @Override
    public List<ITagController> getControllers() {
        return tagControllers;
    }
    
    @Override
    public List<ITagLine> getLines() {
        return ImmutableList.copyOf(lines);
    }

    @Override
    public List<IFakeEntity> getLast() {
        return entities;
    }

    private FakeEntity createSilverfish() {
        return new SilverfishEntity();
    }

    private FakeEntity createSlime() {
        return new SlimeEntity(-1);
    }

    private FakeEntity createArmorStand() {
        return new ArmorStandEntity();
    }

    @Override
    public void setHidden(UUID u, boolean val) {
        this.hiddenFor.put(u, val);
    }

    @Override
    public boolean isHiddenForAll() {
        return hiddenForAll;
    }

    @Override
    public void setHiddenForAll(boolean hiddenForAll) {
        this.hiddenForAll = hiddenForAll;
    }

    @Override
    public boolean isVisible(UUID u) {
        if (hiddenFor.containsKey(u) && hiddenFor.get(u) != null) {
            return hiddenFor.get(u);
        }

        return hiddenForAll;
    }

    @Override
    public boolean isHidden(UUID uuid) {
        return hiddenFor.get(uuid);
    }

    @Override
    public int getBaseId() {
        return baseID;
    }

}
