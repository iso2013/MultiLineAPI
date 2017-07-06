package net.blitzcube.mlapi.tag;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
    private final Map<UUID, Boolean> hiddenFor;
    private PacketUtil.FakeEntity name;
    private boolean hiddenForAll;

    public Tag() {
        this.tagControllers = Lists.newArrayList();
        this.lines = Lists.newArrayList();
        this.entities = Lists.newArrayList();
        this.base = Lists.newArrayList();
        this.hiddenFor = Maps.newHashMap();
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
        final boolean[] created = {false};
        newLines.stream().filter((l) -> l.getLine(forWhat.getUniqueId()) == null).forEach(l -> {
            l.setLine(createArmorStand(), forWhat.getUniqueId());
            created[0] = true;
        });
        this.lines.forEach(tagLine -> render.removed.add(tagLine.removeLine(forWhat.getUniqueId())));
        this.lines.clear();
        this.lines.addAll(newLines);
        if (render.removed.size() > 0 || created[0]) {
            entities.removeAll(base);
            Iterator<PacketUtil.FakeEntity> sIterator = entities.iterator();
            PacketUtil.FakeEntity nameLine = null;
            for (PacketUtil.FakeEntity s; sIterator.hasNext(); ) {
                s = sIterator.next();
                if (s.getType().equals(EntityType.ARMOR_STAND)) {
                    if (newLines.size() < 1) {
                        render.entities.add(s);
                        nameLine = s;
                        sIterator.remove();
                        break;
                    }
                    render.entities.add(newLines.remove(0).getLine(forWhat.getUniqueId()));
                    render.removed.add(s);
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
            this.entities.clear();
            this.entities.addAll(render.entities);
            this.name = nameLine;
        }
        for (TagLine t : this.lines) {
            t.getLine(forWhat.getUniqueId()).setWatcher(createArmorStandWatcher(t.getCached() != null ? t.getCached()
                    : "", t.getCached() != null));
        }
        this.name.setWatcher(createArmorStandWatcher(
                getName((forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER) ?
                        forWhat.getCustomName() : forWhat.getName(), forWhat, forWho),
                forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER)
        ));
        return render;
    }

    public Set<PacketContainer> refreshLines(Entity forWhat, Player forWho) {
        Set<PacketContainer> packets = Sets.newHashSet();
        for (TagLine l : lines) {
            if (l.getLine(forWhat.getUniqueId()) == null) continue;
            l.setCached(l.getText(forWho));
            l.getLine(forWhat.getUniqueId()).setWatcher(createArmorStandWatcher(l.getCached() != null ? l.getCached()
                            : "",
                    l.getCached() != null));
            packets.add(PacketUtil.getMetadataPacket(l.getLine(forWhat.getUniqueId())));
        }
        return packets;
    }

    public PacketContainer refreshName(Entity forWhat, Player forWho) {
        if (this.name == null) return null;
        this.name.setWatcher(createArmorStandWatcher(
                getName((forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER) ?
                        forWhat.getCustomName() : forWhat.getName(), forWhat, forWho),
                forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER)
        ));
        return PacketUtil.getMetadataPacket(name);
    }

    private String getName(String entityName, Entity forWhat, Player forWho) {
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getNamePriority(), o1.getNamePriority()));
        for (TagController t : tagControllers) {
            entityName = t.getName(forWhat, forWho).replace("`PREV`", entityName);
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

    public void setHidden(UUID u, Boolean val) {
        hiddenFor.put(u, val);
    }

    public boolean isHiddenForAll() {
        return hiddenForAll;
    }

    public void setHiddenForAll(boolean hiddenForAll) {
        this.hiddenForAll = hiddenForAll;
    }

    public boolean isVisible(UUID u) {
        if (hiddenFor.containsKey(u) && hiddenFor.get(u) != null) {
            return hiddenFor.get(u);
        }
        return hiddenForAll;
    }

    public boolean isHiddenFor(UUID uniqueId) {
        return hiddenFor.get(uniqueId);
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
