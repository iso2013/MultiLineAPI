package net.blitzcube.mlapi.tag;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Class by iso2013 @ 2017.
 *
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class Tag {
    public final List<TagController> tagControllers;

    private final List<PacketUtil.FakeEntity> base;
    private List<PacketUtil.FakeEntity> entities;

    public Tag() {
        this.tagControllers = Lists.newArrayList();
        this.base = Lists.newArrayList();
        this.entities = Lists.newLinkedList();
        base.add(createSilverfish());
    }

    public TagRender render(Entity forWhat, Player forWho) {
        List<TagLine> tagLines = Lists.newLinkedList();
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        for (TagController tc : this.tagControllers) {
            for (TagLine line : tc.getLines(forWhat)) {
                line.setCached(line.getText(forWho));
                if (line.keepSpaceWhenNull() || line.getCached() != null) {
                    tagLines.add(line);
                }
            }
        }
        Collections.reverse(tagLines);
        TagRender render = new TagRender();
        stackEntities(render, tagLines);
        this.entities = render.entities;
        if (forWho == null) return render;
        int idx = base.size();
        for (TagLine t : tagLines) {
            Bukkit.broadcastMessage(t.getCached());
            render.entities.get(idx).setWatcher(PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
                put(0, (byte) 32);
                put(2, t.getCached() != null ? t.getCached() : "");
                put(3, t.getCached() != null);
                put(4, true);
                put(11, (byte) 16);
            }}));
            idx += 4;
        }
        render.entities.get(idx).setWatcher(PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
            put(0, (byte) 32);
            put(2, getName(forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER ? forWhat
                    .getCustomName() : forWhat.getName(), forWhat));
            put(3, forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER));
            put(4, true);
            put(11, (byte) 16);
        }}));
        return render;
    }

    public void stackEntities(TagRender render, List<TagLine> lines) {
        if (this.entities.size() != base.size() + 1 + (lines.size() * 4)) {
            this.entities.removeAll(base);
            Set<PacketUtil.FakeEntity> armorStands = Sets.newHashSet(),
                    silverfish = Sets.newHashSet(),
                    slimes = Sets.newHashSet();
            Collections.reverse(this.entities);
            for (PacketUtil.FakeEntity e : this.entities) {
                switch (e.getType()) {
                    case ARMOR_STAND:
                        armorStands.add(e);
                        continue;
                    case SILVERFISH:
                        silverfish.add(e);
                        continue;
                    case SLIME:
                        slimes.add(e);
                }
            }
            this.entities.clear();
            render.entities.addAll(base);
            Iterator<PacketUtil.FakeEntity> asI = armorStands.iterator(),
                    sI = silverfish.iterator(),
                    slI = slimes.iterator();
            for (TagLine line : lines) {
                render.entities.add(line.setLine(asI.hasNext() ? asI.next() : createArmorStand()));
                render.entities.add(slI.hasNext() ? slI.next() : createSlime());
                render.entities.add(sI.hasNext() ? sI.next() : createSilverfish());
                render.entities.add(sI.hasNext() ? sI.next() : createSilverfish());
            }
            render.entities.add(asI.hasNext() ? asI.next() : createArmorStand());
            render.removed.addAll(armorStands);
            render.removed.addAll(silverfish);
            render.removed.addAll(slimes);
            armorStands.clear();
            silverfish.clear();
            slimes.clear();
        } else {
            render.entities.addAll(this.entities);
        }
    }

    public String getName(String entityName, Entity forWhat) {
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getNamePriority(), o1.getNamePriority()));
        for (TagController t : tagControllers) {
            entityName = t.getName(forWhat).replace("`PREV`", entityName);
        }
        return entityName;
    }

    public List<PacketUtil.FakeEntity> last() {
        return entities;
    }

    private PacketUtil.FakeEntity createSilverfish() {
        return new PacketUtil.FakeEntity(EntityType.SILVERFISH, PacketUtil.createWatcher(new HashMap<Integer, Object>
                () {{
            put(0, (byte) 32);
            put(3, false);
            put(4, true);
            put(11, (byte) 1);
        }}));
    }

    private PacketUtil.FakeEntity createSlime() {
        return new PacketUtil.FakeEntity(EntityType.SLIME, PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
            put(0, (byte) 32);
            put(3, false);
            put(4, true);
            put(11, (byte) 11);
            put(12, -1);
        }}));
    }

    private PacketUtil.FakeEntity createArmorStand() {
        return new PacketUtil.FakeEntity(EntityType.ARMOR_STAND, PacketUtil.createWatcher(new HashMap<Integer,
                Object>() {{
            put(0, (byte) 32);
            put(3, true);
            put(4, true);
            put(11, (byte) 16);
        }}));
    }

    public static class TagRender {
        private final List<PacketUtil.FakeEntity> removed;
        private final List<PacketUtil.FakeEntity> entities;

        TagRender() {
            this.removed = Lists.newLinkedList();
            this.entities = Lists.newLinkedList();
        }

        public List<PacketUtil.FakeEntity> getRemoved() {
            return removed;
        }

        public List<PacketUtil.FakeEntity> getEntities() {
            return entities;
        }
    }
}
