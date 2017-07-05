package net.blitzcube.mlapi.tag;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Class by iso2013 @ 2017.
 * <p>
 * Licensed under LGPLv3. See LICENSE.txt for more information.
 * You may copy, distribute and modify the software provided that modifications are described and licensed for free
 * under LGPL. Derivatives works (including modifications or anything statically linked to the library) can only be
 * redistributed under LGPL, but applications that use the library don't have to be.
 */

public class Tag {
    private static final Map<Integer, Object> armorStand = new HashMap<Integer, Object>() {{
        put(0, (byte) 32);
        put(4, true);
        put(11, (byte) 16);
    }};
    public final List<TagController> tagControllers;
    private final List<PacketUtil.FakeEntity> base;
    private final List<PacketUtil.FakeEntity> entities;
    private final List<TagLine> lines;

    public Tag() {
        this.tagControllers = Lists.newArrayList();
        this.lines = Lists.newArrayList();
        this.entities = Lists.newArrayList();
        this.base = Lists.newArrayList();
        base.add(createSilverfish());
    }

    private static WrappedDataWatcher createArmorStandWatcher(String name, boolean visible) {
        return PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
            putAll(armorStand);
            put(2, name);
            put(3, visible);
        }});
    }

    private List<TagLine> getLines(Entity forWhat, Player forWho) {
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
        return tagLines;
    }

    public TagRender render(Entity forWhat, Player forWho) {
        TagRender render = new TagRender();
        if (forWhat == null) return render;
        List<TagLine> newLines = getLines(forWhat, forWho);
        this.lines.removeAll(newLines);
        newLines.stream().filter((l) -> l.getLine(forWhat.getUniqueId()) == null).forEach(l -> l.setLine
                (createArmorStand(), forWhat.getUniqueId()));
        this.lines.forEach(tagLine -> render.removed.add(tagLine.removeLine(forWhat.getUniqueId())));
        this.lines.clear();
        this.lines.addAll(newLines);
        entities.removeAll(base);
        Iterator<PacketUtil.FakeEntity> sIterator = entities.iterator();
        PacketUtil.FakeEntity nameLine = null;
        for (PacketUtil.FakeEntity s; sIterator.hasNext(); ) {
            s = sIterator.next();
            if (s.getType().equals(EntityType.ARMOR_STAND)) {
                if (newLines.size() < 1) {
                    render.entities.add(s);
                    nameLine = s;
                    break;
                }
                render.entities.add(newLines.remove(0).getLine(forWhat.getUniqueId()));
            } else {
                render.entities.add(s);
            }
            sIterator.remove();
        }
        for (TagLine line : newLines) {
            render.entities.add(createSilverfish());
            render.entities.add(createSilverfish());
            render.entities.add(createSlime());
            render.entities.add(line.getLine(forWhat.getUniqueId()));
        }
        if (nameLine == null) {
            nameLine = createArmorStand();
            render.entities.add(createSilverfish());
            render.entities.add(createSilverfish());
            render.entities.add(createSlime());
            render.entities.add(nameLine);
        }
        for (PacketUtil.FakeEntity e : base) {
            render.entities.add(0, e);
        }
        render.removed.addAll(entities);
        entities.clear();
        this.entities.addAll(render.entities);
        for (TagLine t : this.lines) {
            t.getLine(forWhat.getUniqueId()).setWatcher(createArmorStandWatcher(t.getCached() != null ? t.getCached()
                    : "", t.getCached() != null));
        }
        nameLine.setWatcher(createArmorStandWatcher(
                getName((forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER) ?
                        forWhat.getCustomName() : forWhat.getName(), forWhat),
                forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER)
        ));
        return render;
    }



    private String getName(String entityName, Entity forWhat) {
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
