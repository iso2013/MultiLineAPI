package net.blitzcube.mlapi.renderer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.IEntityIdentifier;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.event.IEntityPacketContext;
import net.blitzcube.peapi.api.packet.IEntityDestroyPacket;
import net.blitzcube.peapi.api.packet.IEntityMountPacket;
import net.blitzcube.peapi.api.packet.IEntityPacket;
import net.blitzcube.peapi.api.packet.IEntityPacketFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class TagRenderer {
    private final IPacketEntityAPI packet;
    private final JavaPlugin parent;
    private final LineEntityFactory lineFactory;
    private final Map<Player, Set<RenderedTagLine>> visibleLines;
    private final Table<Player, Tag, Boolean> tagVisibility;

    public TagRenderer(IPacketEntityAPI packet, JavaPlugin parent) {
        this.packet = packet;
        this.parent = parent;
        this.lineFactory = new LineEntityFactory(packet.getModifierRegistry(), packet.getEntityFactory());
        this.visibleLines = new HashMap<>();
        this.tagVisibility = HashBasedTable.create();
    }

    public LineEntityFactory getLineEntityFactory() {
        return lineFactory;
    }

    //This method can be used in a variety of ways:
    //A group of lines is added:
    //        index - the index of the first added line in the list of lines
    //        added - the list of all lines that were added
    //        removed - null
    //        lines - the list of all lines
    //    In this case, the method should send 2 + (n*4) mount packets.
    //A group of lines is removed:
    //        index - the index of where the old lines USED to be
    //        added - null
    //        removed - the list of all lines that were removed and should be destroyed
    //        lines - the list of all lines
    public void renderStructureChange(
            int index,
            List<RenderedTagLine> added,
            List<RenderedTagLine> removed,
            Tag t,
            Set<Player> targets,
            int removalSize
    ) {
        for (Player p : targets) {
            IEntityIdentifier below = null, above = null;
            for (int i = index - 1; i >= -1; i--) {
                if (i == -1) {
                    below = t.getBottom().getIdentifier();
                    break;
                }
                RenderedTagLine l;
                if (visibleLines.get(p).contains(l = t.getLines().get(i))) {
                    below = l.getStack().getLast().getIdentifier();
                }
            }

            int addedSize = added != null ? added.size() : 0;
            for (int i = index + removalSize + addedSize; i <= t.getLines().size(); i++) {
                if (i == t.getLines().size()) {
                    above = t.getBottom().getIdentifier();
                    break;
                }
                RenderedTagLine l;
                if (visibleLines.get(p).contains(l = t.getLines().get(i))) {
                    above = l.getBottom().getIdentifier();
                }
            }

            if (below == null || above == null) throw new IllegalStateException();

            IEntityPacketFactory f = packet.getPacketFactory();

            Set<IEntityPacket> firstPhase = new HashSet<>();
            Set<IEntityPacket> secondPhase = new HashSet<>();

            if (added == null && removed != null) {
                firstPhase.add(f.createDestroyPacket(removed.stream().flatMap(l -> l.getStack().stream())
                        .map(IFakeEntity::getIdentifier).toArray(IEntityIdentifier[]::new)));
                firstPhase.add(f.createMountPacket(below, above));
                visibleLines.get(p).removeAll(removed);
            } else if (added != null && removed == null) {
                generateObject(f, t.getTop().getIdentifier(), firstPhase, secondPhase);

                List<IEntityIdentifier> stack = new LinkedList<>();
                stack.add(below);

                for (RenderedTagLine l : added) {
                    boolean skip = true;
                    generateObject(f, l.getBottom().getIdentifier(), firstPhase, secondPhase);
                    for (IFakeEntity e : l.getStack()) {
                        stack.add(e.getIdentifier());
                        if (skip) {
                            skip = false;
                            continue;
                        }
                        firstPhase.add(f.createEntitySpawnPacket(e.getIdentifier()));
                    }
                    visibleLines.get(p).add(l);
                }

                stack.add(above);

                Iterator<IEntityIdentifier> i = stack.iterator();
                IEntityIdentifier prev = null;
                while (i.hasNext()) {
                    IEntityIdentifier current = i.next();
                    if (prev != null) {
                        secondPhase.add(f.createMountPacket(prev, current));
                    }
                    prev = current;
                }

            }
            firstPhase.forEach(i -> packet.dispatchPacket(i, p));
            Bukkit.getScheduler().runTaskLater(
                    parent,
                    () -> secondPhase.forEach(i -> packet.dispatchPacket(i, p)),
                    1);
        }
    }

    public void renderStructureChange(
            int index,
            RenderedTagLine added,
            RenderedTagLine removed,
            Tag t,
            Set<Player> targets
    ) {
        for (Player p : targets) {
            IEntityIdentifier below = null, above = null;
            for (int i = index - 1; i >= -1; i--) {
                if (i == -1) below = t.getBottom().getIdentifier();
                RenderedTagLine l;
                if (visibleLines.get(p).contains(l = t.getLines().get(i))) {
                    below = l.getStack().getLast().getIdentifier();
                }
            }

            int addedSize = added != null ? 1 : 0;
            for (int i = index + addedSize; i <= t.getLines().size(); i++) {
                if (i == t.getLines().size()) above = t.getBottom().getIdentifier();
                RenderedTagLine l;
                if (visibleLines.get(p).contains(l = t.getLines().get(i))) {
                    above = l.getBottom().getIdentifier();
                }
            }

            if (below == null || above == null) throw new IllegalStateException();

            IEntityPacketFactory f = packet.getPacketFactory();

            Set<IEntityPacket> firstPhase = new HashSet<>();
            Set<IEntityPacket> secondPhase = new HashSet<>();

            if (added == null && removed != null) {
                firstPhase.add(f.createDestroyPacket(removed.getStack().stream()
                        .map(IFakeEntity::getIdentifier).toArray(IEntityIdentifier[]::new)));
                firstPhase.add(f.createMountPacket(below, above));
                visibleLines.get(p).remove(removed);
            } else if (added != null && removed == null) {
                generateObject(f, t.getTop().getIdentifier(), firstPhase, secondPhase);

                List<IEntityIdentifier> stack = new LinkedList<>();
                stack.add(below);

                boolean skip = true;
                generateObject(f, added.getBottom().getIdentifier(), firstPhase, secondPhase);
                for (IFakeEntity e : added.getStack()) {
                    stack.add(e.getIdentifier());
                    if (skip) {
                        skip = false;
                        continue;
                    }
                    firstPhase.add(f.createEntitySpawnPacket(e.getIdentifier()));
                }

                stack.add(above);

                Iterator<IEntityIdentifier> i = stack.iterator();
                IEntityIdentifier prev = null;
                while (i.hasNext()) {
                    IEntityIdentifier current = i.next();
                    if (prev != null) {
                        secondPhase.add(f.createMountPacket(prev, current));
                    }
                    prev = current;
                }

                visibleLines.get(p).add(added);
            }
            firstPhase.forEach(i -> packet.dispatchPacket(i, p));
            Bukkit.getScheduler().runTaskLater(
                    parent,
                    () -> secondPhase.forEach(i -> packet.dispatchPacket(i, p)),
                    1);
        }
    }

    public void renderLineChange(RenderedTagLine line, String value, Set<Player> targets) {
        lineFactory.updateName(line.getBottom(), value);
        IEntityPacket d = packet.getPacketFactory().createDataPacket(line.getBottom().getIdentifier());
        targets.forEach(p -> packet.dispatchPacket(d, p));
    }

    public Stream<Player> getNearby(Tag tag, double err) {
        return packet.getViewers(packet.wrap(tag.getTarget()), err);
    }

    public boolean isShown(Player target, RenderedTagLine l) {
        return visibleLines.get(target).contains(l);
    }

    public void queueName(RenderedTagLine line, String value) {
        lineFactory.updateName(line.getBottom(), value);
    }

    public void setVisible(Tag tag, Player target, Boolean val) {
        tagVisibility.put(target, tag, val);
    }

    public Boolean isVisible(ITag tag, Player target) {
        return tagVisibility.get(target, tag);
    }

    public void purge(Player player) {
        tagVisibility.rowMap().remove(player);
        visibleLines.remove(player);
    }

    public void spawnTag(Tag t, Player p, IEntityPacketContext c) {
        spawnTag(t, p, null, c);
    }

    private void generateObject(IEntityPacketFactory f, IEntityIdentifier i, Set<IEntityPacket> s, Set<IEntityPacket>
            s2) {
        IEntityPacket[] packets = f.createObjectSpawnPacket(i);
        s.add(packets[0]);
        s2.add(packets[1]);
    }


    public void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket, IEntityPacketContext c) {
        Boolean visible = tagVisibility.get(p, t);
        visible = visible != null ? visible : t.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory f = packet.getPacketFactory();

        Set<IEntityPacket> firstPhase = new HashSet<>();
        Set<IEntityPacket> secondPhase = new HashSet<>();

        firstPhase.add(f.createEntitySpawnPacket(t.getBottom().getIdentifier()));
        generateObject(f, t.getTop().getIdentifier(), firstPhase, secondPhase);

        List<IFakeEntity> stack = new LinkedList<>();
        stack.add(t.getBottom());

        for (RenderedTagLine l : t.getLines()) {
            Bukkit.broadcastMessage("ADDING LINE TO " + t.getTarget().getCustomName());
            boolean skip = true;
            generateObject(f, l.getBottom().getIdentifier(), firstPhase, secondPhase);
            for (IFakeEntity e : l.getStack()) {
                stack.add(e);
                if (skip) {
                    skip = false;
                    continue;
                }
                firstPhase.add(f.createEntitySpawnPacket(e.getIdentifier()));
            }
            lineFactory.updateName(l.getBottom(), l.get(p));
            secondPhase.add(packet.getPacketFactory().createDataPacket(l.getBottom().getIdentifier()));
            visibleLines.get(p).add(l);
        }

        if (t.getTarget().isCustomNameVisible()) {
            lineFactory.updateName(t.getTop(), t.getTarget().getCustomName());
            stack.add(t.getTop());
            secondPhase.add(packet.getPacketFactory().createDataPacket(t.getTop().getIdentifier()));
        }

        lineFactory.updateLocations(t.getTarget().getLocation(), stack);

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

        if (c != null) {
            c.queueDispatch(firstPhase).queueDispatch(secondPhase, 2);
        } else {
            firstPhase.forEach(ep -> packet.dispatchPacket(ep, p));
            Bukkit.getScheduler().runTaskLater(
                    parent,
                    () -> secondPhase.forEach(ep -> packet.dispatchPacket(ep, p)),
                    1
            );
        }
    }

    public void destroyTag(Tag t, Player player, IEntityDestroyPacket destroyPacket) {
        Bukkit.broadcastMessage("DESTROY CALLED");
        if (destroyPacket == null)
            destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        IEntityDestroyPacket finalDestroyPacket = destroyPacket;
        Set<RenderedTagLine> lines = visibleLines.get(player);
        if (lines == null) lines = new HashSet<>();
        t.getLines().stream().filter(lines::remove)
                .forEach(r -> {
                    Bukkit.broadcastMessage("DESTROYING LINE");
                    r.getStack().forEach(e -> {
                        Bukkit.broadcastMessage("ADDING DESTROY ENTITY");
                        finalDestroyPacket.addToGroup(e.getIdentifier());
                    });
                });
        destroyPacket.addToGroup(t.getBottom().getIdentifier());
        Bukkit.broadcastMessage("DESTROYED BOTTOm");
        destroyPacket.addToGroup(t.getTop().getIdentifier());
        Bukkit.broadcastMessage("DESTROYING TOP");
    }

    public void batchDestroyTags(Stream<Tag> tags, Player player) {
        IEntityDestroyPacket destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        Set<RenderedTagLine> lines = visibleLines.get(player);
        tags.flatMap(tag -> tag.getLines().stream())
                .filter(lines::remove)
                .forEach(r -> r.getStack().forEach(e -> destroyPacket.addToGroup(e.getIdentifier())));
    }
}
