package net.blitzcube.mlapi.renderer.mount;

import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class MountTagRenderer extends TagRenderer {
    private final Map<IFakeEntity, Entity> tagEntities = new HashMap<>();

    public MountTagRenderer(IPacketEntityAPI packetAPI, LineEntityFactory lineFactory, VisibilityStates states,
                            JavaPlugin parent) {
        super(packetAPI, lineFactory, states, parent);
        packetAPI.addListener(new MountTagPacketListener(packetAPI, lineFactory, tagEntities));
    }

    @Override
    public void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target) {
        List<IEntityPacket> firstPhase = new LinkedList<>(),
                secondPhase = new LinkedList<>();

        for (StructureTransaction t : transactions)
            processTransaction(t, firstPhase, secondPhase, tag, target);

        if (!firstPhase.isEmpty()) firstPhase.forEach(i -> packetAPI.dispatchPacket(i, target, 0));
        if (!secondPhase.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(
                    parent,
                    () -> secondPhase.forEach(i -> packetAPI.dispatchPacket(i, target, 0)),
                    1L
            );
        }
    }

    private void processTransaction(StructureTransaction t, List<IEntityPacket> firstPhase, List<IEntityPacket>
            secondPhase, Tag tag, Player target) {
        if (tag.getTarget() == target) return;

        IEntityPacketFactory f = packetAPI.getPacketFactory();
        if (t instanceof AddTransaction) {
            AddTransaction at = (AddTransaction) t;
            if (target == tag.getTarget()) return;

            Location loc = tag.getTarget().getLocation();
            IHitbox hb = tag.getTargetHitbox();
            if (hb != null)
                loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);

            RenderedTagLine last = null;

            Set<IEntityIdentifier> passengers = new HashSet<>();
            for (RenderedTagLine l : at.getAdded()) {
                loc.add(0, LINE_HEIGHT, 0);

                String value = l.get(target);

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
                        lineFactory.updateLocation(loc, fe);
                        firstPhase.add(f.createEntitySpawnPacket(fe.getIdentifier()));
                        secondPhase.add(f.createMountPacket(prev, fe.getIdentifier()));
                    }
                    prev = fe.getIdentifier();
                }

                state.addSpawnedLine(target, l);
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
                state.getSpawnedLines(target).removeAll(mt.getMoved());
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
                    state.addSpawnedLine(target, l);
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
            IEntityDestroyPacket destroyPacket = packetAPI.getPacketFactory().createDestroyPacket();
            rt.getRemoved().forEach(r -> r.getStack().forEach(e -> destroyPacket.addToGroup(e.getIdentifier())));
            firstPhase.add(destroyPacket);
            state.getSpawnedLines(target).removeAll(rt.getRemoved());
        }
    }

    @Override
    public void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket) {
        if (t.getTarget() == p) return;
        Boolean visible = state.isVisible(t, p);
        visible = visible != null ? visible : t.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory f = packetAPI.getPacketFactory();

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
            secondPhase.add(f.createMountPacket(packetAPI.wrap(t.getTarget()), t.getBottom().getIdentifier()));
        } else {
            mountPacket.addToGroup(t.getBottom().getIdentifier());
        }

        processTransaction(new AddTransaction(
                t.getBottom().getIdentifier(),
                t.getTop().getIdentifier(),
                t.getLines()
        ), firstPhase, secondPhase, t, p);

        firstPhase.forEach(ep -> packetAPI.dispatchPacket(ep, p, 0));
        Bukkit.getScheduler().runTask(
                parent,
                () -> secondPhase.forEach(ep -> packetAPI.dispatchPacket(ep, p, 0))
        );
        state.addSpawnedTag(p, t);
    }

    public void purge(Tag remove) {
        tagEntities.entrySet().removeIf(e -> e.getValue() == remove.getTarget());
    }

    @Override
    public LinkedList<IFakeEntity> createStack(Tag tag, int addIndex) {
        Location location = tag.getTarget().getLocation();
        IHitbox hb = tag.getTargetHitbox();
        double y = addIndex * LINE_HEIGHT;
        if (hb != null) y = hb.getMax().getY() - hb.getMin().getY();
        location.add(0, y, 0);
        LinkedList<IFakeEntity> stack = new LinkedList<>();
        stack.add(lineFactory.createArmorStand(location));
        stack.add(lineFactory.createSlime(location));
        stack.add(lineFactory.createSilverfish(location));
        stack.add(lineFactory.createSilverfish(location));
        for (IFakeEntity e : stack) tagEntities.put(e, tag.getTarget());
        return stack;
    }

    @Override
    public void purge(IFakeEntity entity) {
        tagEntities.remove(entity);
    }

    @Override
    public IFakeEntity createBottom(Tag target) {
        IHitbox hb = target.getTargetHitbox();
        double y = 0;
        if (hb != null) y = hb.getMax().getY() - hb.getMin().getY();
        IFakeEntity newEntity = lineFactory.createSilverfish(
                target.getTarget().getLocation().add(0, y + BOTTOM_LINE_HEIGHT, 0)
        );
        tagEntities.put(newEntity, target.getTarget());
        return newEntity;
    }

    @Override
    public IFakeEntity createTop(Tag target) {
        IHitbox hb = target.getTargetHitbox();
        double y = 0;
        if (hb != null) y = hb.getMax().getY() - hb.getMin().getY();
        IFakeEntity newEntity = lineFactory.createArmorStand(
                target.getTarget().getLocation().add(0, y + BOTTOM_LINE_HEIGHT, 0)
        );
        tagEntities.put(newEntity, target.getTarget());
        return newEntity;
    }
}
