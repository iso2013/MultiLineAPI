package net.blitzcube.mlapi.renderer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.structure.transactions.AddTransaction;
import net.blitzcube.mlapi.structure.transactions.NameTransaction;
import net.blitzcube.mlapi.structure.transactions.RemoveTransaction;
import net.blitzcube.mlapi.structure.transactions.StructureTransaction;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.packet.IEntityDestroyPacket;
import net.blitzcube.peapi.api.packet.IEntityMountPacket;
import net.blitzcube.peapi.api.packet.IEntityPacket;
import net.blitzcube.peapi.api.packet.IEntityPacketFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class TagRenderer {
    private static final double LINE_HEIGHT = 0.15;
    private static final double BOTTOM_LINE_HEIGHT = 0.12;
    private final IPacketEntityAPI packet;
    private final JavaPlugin parent;
    private final LineEntityFactory lineFactory;
    private final Multimap<Player, RenderedTagLine> visibleLines;
    private final Multimap<Player, Tag> visibleTags;
    private final Table<Player, Tag, Boolean> tagVisibility;

    public TagRenderer(IPacketEntityAPI packet, JavaPlugin parent) {
        this.packet = packet;
        this.parent = parent;
        this.lineFactory = new LineEntityFactory(packet.getModifierRegistry(), packet.getEntityFactory());
        this.visibleLines = HashMultimap.create();
        this.visibleTags = HashMultimap.create();
        this.tagVisibility = HashBasedTable.create();
    }

    public LineEntityFactory getLineEntityFactory() {
        return lineFactory;
    }

    public Stream<Player> getNearby(Tag tag, double err) {
        return packet.getViewers(packet.wrap(tag.getTarget()), err);
    }

    public void setVisible(Tag tag, Player target, Boolean val) {
        tagVisibility.put(target, tag, val);
    }

    public Boolean isVisible(ITag tag, Player target) {
        return tagVisibility.get(target, tag);
    }

    public void purge(Player player) {
        tagVisibility.rowMap().remove(player);
        visibleLines.removeAll(player);
    }

    private void generateObject(IEntityPacketFactory f, IEntityIdentifier i, Set<IEntityPacket> s, Set<IEntityPacket>
            s2) {
        IEntityPacket[] packets = f.createObjectSpawnPacket(i);
        s.add(packets[0]);
        s2.add(packets[1]);
    }

    public void processTransaction(StructureTransaction t) {
        IEntityPacketFactory f = packet.getPacketFactory();
        Set<IEntityPacket> firstPhase = new HashSet<>(), secondPhase = null;

        if (t instanceof AddTransaction) {
            AddTransaction at = (AddTransaction) t;
            secondPhase = new HashSet<>();
            List<IEntityIdentifier> stack = new LinkedList<>();
            stack.add(at.getBelow());

            Location loc = at.getTag().getTarget().getLocation();
            IHitbox hb = at.getTag().getTargetHitbox();
            loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);
            for (RenderedTagLine l : at.getAdded()) {
                loc.add(0, LINE_HEIGHT, 0);
                boolean skip = true;
                generateObject(f, l.getBottom().getIdentifier(), firstPhase, secondPhase);
                for (IFakeEntity e : l.getStack()) {
                    stack.add(e.getIdentifier());
                    if (skip) {
                        skip = false;
                        continue;
                    }
                    lineFactory.updateLocation(loc, e);
                    firstPhase.add(f.createEntitySpawnPacket(e.getIdentifier()));
                }
                visibleLines.put(t.getTarget(), l);
            }

            stack.add(at.getAbove());

            Iterator<IEntityIdentifier> i = stack.iterator();
            IEntityIdentifier prev = null;
            while (i.hasNext()) {
                IEntityIdentifier current = i.next();
                if (prev != null) {
                    secondPhase.add(f.createMountPacket(prev, current));
                }
                prev = current;
            }
        } else if (t instanceof RemoveTransaction) {
            RemoveTransaction rt = (RemoveTransaction) t;
            secondPhase = new HashSet<>();
            firstPhase.add(f.createDestroyPacket(rt.getRemoved().stream().flatMap(l -> l.getStack().stream())
                    .map(IFakeEntity::getIdentifier).toArray(IEntityIdentifier[]::new)));
            secondPhase.add(f.createMountPacket(rt.getBelow(), rt.getAbove()));
            visibleLines.get(t.getTarget()).removeAll(((RemoveTransaction) t).getRemoved());
        } else if (t instanceof NameTransaction) {
            NameTransaction nt = (NameTransaction) t;
            for (Map.Entry<RenderedTagLine, String> q : nt.getQueuedNames().entrySet()) {
                lineFactory.updateName(q.getKey().getBottom(), q.getValue());
                if (visibleLines.containsEntry(t.getTarget(), q.getKey())) {
                    firstPhase.add(f.createDataPacket(q.getKey().getBottom().getIdentifier()));
                }
            }
        }
        if (!firstPhase.isEmpty()) firstPhase.forEach(i -> packet.dispatchPacket(i, t.getTarget()));
        if (secondPhase != null) {
            Set<IEntityPacket> finalSecondPhase = secondPhase;
            Bukkit.getScheduler().runTask(parent,
                    () -> finalSecondPhase.forEach(i -> packet.dispatchPacket(i, t.getTarget())));
        }
    }

    public void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket) {
        Boolean visible = tagVisibility.get(p, t);
        visible = visible != null ? visible : t.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory f = packet.getPacketFactory();

        Set<IEntityPacket> firstPhase = new HashSet<>();
        Set<IEntityPacket> secondPhase = new HashSet<>();

        firstPhase.add(f.createEntitySpawnPacket(t.getBottom().getIdentifier()));

        Location loc = t.getTarget().getLocation();
        IHitbox hb = t.getTargetHitbox();
        loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);

        lineFactory.updateLocation(loc, t.getBottom());

        List<IFakeEntity> stack = new LinkedList<>();
        stack.add(t.getBottom());

        for (RenderedTagLine l : t.getLines()) {
            loc.add(0, LINE_HEIGHT, 0);
            boolean skip = true;
            String newName;
            lineFactory.updateName(l.getBottom(), (newName = l.get(p)));
            if (newName == null && l.shouldRemoveSpaceWhenNull()) continue;
            lineFactory.updateLocation(loc, l.getBottom());
            generateObject(f, l.getBottom().getIdentifier(), firstPhase, firstPhase);
            for (IFakeEntity e : l.getStack()) {
                lineFactory.updateLocation(loc, e);
                stack.add(e);
                if (skip) {
                    skip = false;
                    continue;
                }
                firstPhase.add(f.createEntitySpawnPacket(e.getIdentifier()));
            }
            visibleLines.put(p, l);
        }

        if (t.getTarget().isCustomNameVisible() || t.getTarget() instanceof Player) {
            String name = t.getTarget() instanceof Player ? ((Player) t.getTarget()).getDisplayName() : t.getTarget()
                    .getCustomName();
            for (ITagController tc : t.getTagControllers(false)) {
                name = tc.getName(t.getTarget(), p, name);
                if (name.contains(ChatColor.COLOR_CHAR + "")) name = name + ChatColor.RESET;
            }
            lineFactory.updateLocation(loc, t.getTop());
            lineFactory.updateName(t.getTop(), name);
            stack.add(t.getTop());
            generateObject(f, t.getTop().getIdentifier(), firstPhase, firstPhase);
        }

        Iterator<IFakeEntity> i = stack.iterator();
        IEntityIdentifier last = null;
        while (i.hasNext()) {
            IEntityIdentifier current = i.next().getIdentifier();
            if (last == null) {
                if (mountPacket == null) {
                    secondPhase.add(f.createMountPacket(packet.wrap(t.getTarget()), current));
                } else {
                    mountPacket.addToGroup(current);
                }
            } else {
                secondPhase.add(f.createMountPacket(last, current));
            }
            last = current;
        }

        firstPhase.forEach(ep -> packet.dispatchPacket(ep, p));
        Bukkit.getScheduler().runTask(
                parent,
                () -> secondPhase.forEach(ep -> packet.dispatchPacket(ep, p))
        );
        visibleTags.put(p, t);
    }

    public void destroyTag(Tag t, Player player, IEntityDestroyPacket destroyPacket) {
        boolean event = destroyPacket != null;
        if (destroyPacket == null)
            destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        IEntityDestroyPacket finalDestroyPacket = destroyPacket;
        Collection<RenderedTagLine> lines = visibleLines.get(player);
        t.getLines().stream()
                .filter(lines::remove)
                .forEach(r -> r.getStack().forEach(e -> finalDestroyPacket.addToGroup(e.getIdentifier())));
        finalDestroyPacket.addToGroup(t.getBottom().getIdentifier());
        finalDestroyPacket.addToGroup(t.getTop().getIdentifier());
        if (!event) {
            packet.dispatchPacket(finalDestroyPacket, player);
        }
        visibleTags.remove(player, t);
    }

    public void batchDestroyTags(Stream<Tag> tags, Player player) {
        IEntityDestroyPacket destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        Collection<RenderedTagLine> lines = visibleLines.get(player);
        tags.forEach(tag -> {
            tag.getLines().stream().filter(lines::remove)
                    .forEach(l -> l.getStack().forEach(e -> destroyPacket.addToGroup(e.getIdentifier())));
            destroyPacket.addToGroup(tag.getTop().getIdentifier());
            destroyPacket.addToGroup(tag.getBottom().getIdentifier());
        });
        packet.dispatchPacket(destroyPacket, player);

    }

    public Multimap<Player, RenderedTagLine> getVisibilityMap() {
        return visibleLines;
    }

    public boolean isSpawned(Player viewer, Tag t) {
        return visibleTags.containsEntry(viewer, t);
    }

    public void purge(Tag remove) {
        lineFactory.purge(remove.getTarget());
    }
}
