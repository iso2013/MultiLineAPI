package net.blitzcube.mlapi.renderer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import net.blitzcube.mlapi.api.tag.ITag;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.structure.transactions.*;
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

    public void processTransaction(StructureTransaction t) {
        List<IEntityPacket> firstPhase = new LinkedList<>(),
                secondPhase = t instanceof RemoveTransaction ? null : new LinkedList<>();

        processTransaction(t, firstPhase, secondPhase);

        if (!firstPhase.isEmpty()) firstPhase.forEach(i -> packet.dispatchPacket(i, t.getTarget(), 0));
        if (secondPhase != null && !secondPhase.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(
                    parent,
                    () -> secondPhase.forEach(i -> packet.dispatchPacket(i, t.getTarget(), 0)),
                    1L
            );
        }
    }

    private void processTransaction(StructureTransaction t, List<IEntityPacket> firstPhase, List<IEntityPacket>
            secondPhase) {
        IEntityPacketFactory f = packet.getPacketFactory();
        if (t instanceof AddTransaction) {
            AddTransaction at = (AddTransaction) t;
            if (t.getTarget() == at.getTag().getTarget()) return;

            Location loc = at.getTag().getTarget().getLocation();
            IHitbox hb = at.getTag().getTargetHitbox();
            if (hb != null)
                loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);

            RenderedTagLine last = null;

            Set<IEntityIdentifier> passengers = new HashSet<>();
            for (RenderedTagLine l : at.getAdded()) {
                loc.add(0, LINE_HEIGHT, 0);

                String value = l.get(at.getTarget());

                lineFactory.updateName(l.getBottom(), value);
                lineFactory.updateLocation(loc, l.getBottom());
                Collections.addAll(firstPhase, f.createObjectSpawnPacket(l.getBottom().getIdentifier()));

                if (value != null || !l.shouldRemoveSpaceWhenNull()) {
                    IEntityIdentifier vehicle = last == null ? at.getBelow() : last.getStack().getLast()
                            .getIdentifier();
                    if (!passengers.isEmpty()) {
                        passengers.add(l.getBottom().getIdentifier());
                        secondPhase.add(f.createMountPacket(
                                vehicle,
                                passengers.toArray(new IEntityIdentifier[0])
                        ));
                        passengers.clear();
                        last = l;
                    } else {
                        secondPhase.add(f.createMountPacket(
                                vehicle,
                                l.getBottom().getIdentifier()
                        ));
                        last = l;
                    }
                } else {
                    passengers.add(l.getBottom().getIdentifier());
                }

                IEntityIdentifier prev = null;
                for (IFakeEntity fe : l.getStack()) {
                    if (prev != null) {
                        lineFactory.updateLocation(loc, l.getBottom());
                        firstPhase.add(f.createEntitySpawnPacket(fe.getIdentifier()));
                        secondPhase.add(f.createMountPacket(prev, fe.getIdentifier()));
                    }
                    prev = fe.getIdentifier();
                }

                visibleLines.put(at.getTarget(), l);
            }

            IEntityIdentifier vehicle = last == null ? at.getBelow() : last.getStack().getLast().getIdentifier();
            if (!passengers.isEmpty()) {
                passengers.add(at.getAbove());
                secondPhase.add(f.createMountPacket(
                        vehicle,
                        passengers.toArray(new IEntityIdentifier[0])
                ));
                passengers.clear();
            } else {
                secondPhase.add(f.createMountPacket(
                        vehicle,
                        at.getAbove()
                ));
            }
        } else if (t instanceof MoveTransaction) {
            MoveTransaction mt = (MoveTransaction) t;
            if (mt.isToSameLevel()) {
                List<IEntityIdentifier> identifiers = new LinkedList<>();
                mt.getMoved().forEach(renderedTagLine -> {
                    identifiers.add(renderedTagLine.getBottom().getIdentifier());
                    lineFactory.updateName(renderedTagLine.getBottom(), null);
                    firstPhase.add(f.createDataPacket(renderedTagLine.getBottom().getIdentifier()));
                });
                identifiers.add(mt.getAbove());
                firstPhase.add(f.createMountPacket(mt.getBelow(), identifiers.toArray(new IEntityIdentifier[0])));
                visibleLines.get(mt.getTarget()).removeAll(mt.getMoved());
            } else {
                RenderedTagLine last = null;
                for (RenderedTagLine l : mt.getMoved()) {
                    if (last != null) {
                        firstPhase.add(f.createMountPacket(
                                last.getStack().getLast().getIdentifier(),
                                l.getBottom().getIdentifier()
                        ));
                    } else {
                        firstPhase.add(f.createMountPacket(
                                mt.getBelow(),
                                l.getBottom().getIdentifier()
                        ));
                    }
                    last = l;
                    visibleLines.put(mt.getTarget(), l);
                }
                if (last != null) {
                    firstPhase.add(f.createMountPacket(last.getStack().getLast().getIdentifier(), mt.getAbove()));
                }
            }
        } else if (t instanceof NameTransaction) {
            ((NameTransaction) t).getQueuedNames()
                    .forEach((key, value) -> {
                        lineFactory.updateName(key.getBottom(), value);
                        secondPhase.add(f.createDataPacket(key.getBottom().getIdentifier()));
                    });
        } else if (t instanceof RemoveTransaction) {
            RemoveTransaction rt = (RemoveTransaction) t;
            IEntityDestroyPacket destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
            rt.getRemoved().forEach(r -> r.getStack().forEach(e -> destroyPacket.addToGroup(e.getIdentifier())));
            firstPhase.add(destroyPacket);
            visibleLines.get(rt.getTarget()).removeAll(rt.getRemoved());
        }
    }

    public void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket) {
        if (t.getTarget() == p) return;
        Boolean visible = tagVisibility.get(p, t);
        visible = visible != null ? visible : t.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory f = packet.getPacketFactory();

        List<IEntityPacket> firstPhase = new LinkedList<>();
        List<IEntityPacket> secondPhase = new LinkedList<>();

        Location loc = t.getTarget().getLocation();
        IHitbox hb = t.getTargetHitbox();
        if (hb != null)
            loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);

        lineFactory.updateLocation(loc, t.getBottom());
        lineFactory.updateLocation(loc, t.getTop());

        if (t.getTarget().isCustomNameVisible() || t.getTarget() instanceof Player) {
            String name = t.getTarget() instanceof Player ?
                    ((Player) t.getTarget()).getDisplayName() : t.getTarget().getCustomName();
            for (ITagController tc : t.getTagControllers(false)) {
                name = tc.getName(t.getTarget(), p, name);
                if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) name = name + ChatColor.RESET;
            }
            lineFactory.updateName(t.getTop(), name);
        } else lineFactory.updateName(t.getTop(), null);

        firstPhase.add(f.createEntitySpawnPacket(t.getBottom().getIdentifier()));
        Collections.addAll(firstPhase, f.createObjectSpawnPacket(t.getTop().getIdentifier()));

        if (mountPacket == null) {
            secondPhase.add(f.createMountPacket(packet.wrap(t.getTarget()), t.getBottom().getIdentifier()));
        } else {
            mountPacket.addToGroup(t.getBottom().getIdentifier());
        }

        processTransaction(new AddTransaction(
                t.getBottom().getIdentifier(),
                t.getTop().getIdentifier(),
                t.getLines(),
                p,
                t
        ), firstPhase, secondPhase);

        firstPhase.forEach(ep -> packet.dispatchPacket(ep, p, 0));
        Bukkit.getScheduler().runTask(
                parent,
                () -> secondPhase.forEach(ep -> packet.dispatchPacket(ep, p, 0))
        );
        visibleTags.put(p, t);
    }

    public void destroyTag(Tag t, Player player, IEntityDestroyPacket destroyPacket) {
        boolean event = destroyPacket != null;
        if (destroyPacket == null)
            destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        IEntityDestroyPacket finalDestroyPacket = destroyPacket;
        t.getLines().forEach(r -> r.getStack().forEach(e -> finalDestroyPacket.addToGroup(e.getIdentifier())));
        finalDestroyPacket.addToGroup(t.getBottom().getIdentifier());
        finalDestroyPacket.addToGroup(t.getTop().getIdentifier());
        if (!event) {
            packet.dispatchPacket(finalDestroyPacket, player, 0);
        }
        visibleTags.remove(player, t);
    }

    public void batchDestroyTags(Stream<Tag> tags, Player player) {
        IEntityDestroyPacket destroyPacket = (IEntityDestroyPacket) packet.getPacketFactory().createDestroyPacket();
        tags.forEach(tag -> destroyTag(tag, player, destroyPacket));
        packet.dispatchPacket(destroyPacket, player, 0);
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

    public Stream<Tag> getVisible(Player target) {
        return visibleTags.get(target).stream();
    }

    public void updateName(Tag tag, Player viewer) {
        String name;
        name = tag.getTarget() instanceof Player ? ((Player) tag.getTarget()).getDisplayName() : tag.getTarget()
                .getCustomName();
        for (ITagController tc : tag.getTagControllers(false)) {
            name = tc.getName(tag.getTarget(), viewer, name);
            if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) name = name + ChatColor.RESET;
        }
    }
}
