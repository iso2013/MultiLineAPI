package net.blitzcube.mlapi.tag;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.blitzcube.mlapi.util.PacketUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by iso2013 on 5/4/2017.
 */
public class Tag {
    public final List<TagController> tagControllers;

    private final List<PacketUtil.FakeEntity> base;
    private LinkedList<PacketUtil.FakeEntity> entities;

    public Tag() {
        this.tagControllers = Lists.newArrayList();
        this.base = Lists.newArrayList();
        this.entities = Lists.newLinkedList();
        base.add(createSilverfish());
    }

    public LinkedList<PacketUtil.FakeEntity> render(Entity forWhat, Player forWho) {
        LinkedList<TagLine> tagLines = Lists.newLinkedList();
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        for (TagController tc : this.tagControllers) {
            for (TagLine line : tc.getLines(forWhat)) {
                line.setCached(line.getText(forWho));
                if (line.keepSpaceWhenNull() || line.getCached() != null) {
                    tagLines.add(line);
                }
            }
        }
        if (this.entities.size() != base.size() + 1 + (tagLines.size() * 4)) {
            this.entities.removeAll(base);
            Set<PacketUtil.FakeEntity> armorStands = Sets.newHashSet(), silverfish = Sets.newHashSet(), slimes = Sets
                    .newHashSet();
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
            this.entities.addAll(base);
            Iterator<PacketUtil.FakeEntity> asI = armorStands.iterator(), sI = silverfish.iterator(), slI = slimes
                    .iterator();
            Collections.reverse(tagLines);
            for (TagLine line : tagLines) {
                line.setLine(asI.hasNext() ? asI.next() : (createArmorStand()));
                this.entities.add(line.getLine());
                this.entities.add(slI.hasNext() ? slI.next() : (createSlime()));
                this.entities.add(sI.hasNext() ? sI.next() : (createSilverfish()));
                this.entities.add(sI.hasNext() ? sI.next() : (createSilverfish()));
            }
            this.entities.add(asI.hasNext() ? asI.next() : (createArmorStand()));
            armorStands.clear();
            silverfish.clear();
            slimes.clear();
        }
        if (forWho == null) return this.entities;
        int idx = base.size();
        for (TagLine t : tagLines) {
            this.entities.get(idx).setWatcher(PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
                put(0, (byte) 32);
                put(2, t.getCached() != null ? t.getCached() : "");
                put(3, t.getCached() != null);
                put(4, true);
                put(11, (byte) 16);
            }}));
            idx += 4;
        }
        this.entities.get(idx).setWatcher(PacketUtil.createWatcher(new HashMap<Integer, Object>() {{
            put(0, (byte) 32);
            put(2, getName(forWhat.getCustomName() != null && forWhat.getType() != EntityType.PLAYER ? forWhat
                    .getCustomName() : forWhat.getName(), forWhat));
            put(3, forWhat.isCustomNameVisible() || forWhat.getType().equals(EntityType.PLAYER));
            put(4, true);
            put(11, (byte) 16);
        }}));
        return this.entities;
    }

    public String getName(String entityName, Entity forWhat) {
        this.tagControllers.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
        for (TagController t : tagControllers) {
            entityName = t.getName(forWhat).replace("`PREV`", entityName);
        }
        return entityName;
    }

    public LinkedList<PacketUtil.FakeEntity> last() {
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
}
