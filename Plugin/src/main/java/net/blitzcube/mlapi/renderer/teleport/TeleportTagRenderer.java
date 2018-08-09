package net.blitzcube.mlapi.renderer.teleport;

import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.transactions.*;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.blitzcube.peapi.api.IPacketEntityAPI;
import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.packet.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class TeleportTagRenderer extends TagRenderer {
    private final boolean animated;

    public TeleportTagRenderer(IPacketEntityAPI packet, LineEntityFactory lineFactory, VisibilityStates state,
                               MultiLineAPI parent, boolean animated) {
        super(packet, lineFactory, state, parent);
        this.animated = animated;
        packetAPI.addListener(new TeleportTagPacketListener(LINE_HEIGHT, BOTTOM_LINE_HEIGHT, parent, state));
    }

    @Override
    public void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target) {
        if (tag.getTarget() == target) return;
        if (!state.isSpawned(target, tag)) return;

        IEntityPacketFactory f = packetAPI.getPacketFactory();
        List<IEntityPacket> firstPhase = null,
                secondPhase = null,
                thirdPhase = null;

        Location loc = null;
        IHitbox hb;

        for (StructureTransaction t : transactions) {
            if (t instanceof MoveTransaction) {
                MoveTransaction mt = (MoveTransaction) t;
                if (mt.isToSameLevel()) {
                    t = new RemoveTransaction(mt.getBelow(), mt.getAbove(), mt.getMoved());
                } else {
                    t = new AddTransaction(mt.getBelow(), mt.getAbove(), mt.getMoved());
                }
            }
            if (t instanceof AddTransaction || t instanceof RemoveTransaction) {
                boolean add = t instanceof AddTransaction;
                Collection<RenderedTagLine> changed =
                        add ? ((AddTransaction) t).getAdded() : ((RemoveTransaction) t).getRemoved();

                if (loc == null) {
                    loc = tag.getTarget().getLocation();
                    hb = tag.getTargetHitbox();
                    if (hb != null)
                        loc.setY(loc.getY() + (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT);
                }

                int idx = 0;
                IEntityDestroyPacket destroyPacket = null;
                for (RenderedTagLine line : tag.getLines()) {
                    String value = line.get(target);
                    if (changed.contains(line)) {
                        if (add) {
                            if (value != null) {
                                if (thirdPhase == null) thirdPhase = new LinkedList<>();
                                lineFactory.updateName(line.getBottom(), value);
                                lineFactory.updateLocation(loc.clone().add(0, LINE_HEIGHT * idx++, 0), line.getBottom
                                        ());

                                Collections.addAll(thirdPhase, f.createObjectSpawnPacket(line.getBottom()
                                        .getIdentifier()));
                                state.addSpawnedLine(target, line);
                            } else {
                                idx++;
                            }
                        } else {
                            if (destroyPacket == null) destroyPacket = f.createDestroyPacket();
                            destroyPacket.addToGroup(line.getBottom().getIdentifier());
                            if (!line.shouldRemoveSpaceWhenNull()) idx++;
                        }
                    } else if (!state.isLineSpawned(target, line)) {
                        if (!line.shouldRemoveSpaceWhenNull()) {
                            idx++;
                        }
                    } else idx++;
                }
                if (destroyPacket != null) {
                    if (firstPhase == null) firstPhase = new LinkedList<>();
                    firstPhase.add(destroyPacket);
                    state.getSpawnedLines(target).removeAll(changed);
                }
            } else if (t instanceof NameTransaction) {
                if (firstPhase == null) firstPhase = new LinkedList<>();
                List<IEntityPacket> finalFirstPhase = firstPhase;
                ((NameTransaction) t).getQueuedNames()
                        .forEach((key, value) -> {
                            lineFactory.updateName(key.getBottom(), value);
                            finalFirstPhase.add(f.createDataPacket(key.getBottom().getIdentifier()));
                        });
            }
        }

        if (loc != null) {
            secondPhase = new LinkedList<>();
            for (RenderedTagLine l : tag.getLines()) {
                if (!state.isLineSpawned(target, l)) {
                    if (!l.shouldRemoveSpaceWhenNull()) {
                        loc.setY(loc.getY() + LINE_HEIGHT);
                    }
                    continue;
                }
                secondPhase.add(f.createMovePacket(
                        l.getBottom().getIdentifier(),
                        loc.toVector(),
                        null,
                        false,
                        IEntityMovePacket.MoveType.TELEPORT
                ));
                loc.setY(loc.getY() + LINE_HEIGHT);
            }
            secondPhase.add(f.createMovePacket(
                    tag.getTop().getIdentifier(),
                    loc.toVector(),
                    null,
                    false,
                    IEntityMovePacket.MoveType.TELEPORT
            ));
        }

        if (firstPhase != null && !firstPhase.isEmpty())
            firstPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0));
        if (!animated) {
            if (secondPhase != null && !secondPhase.isEmpty()) {
                secondPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0));
            }
            if (thirdPhase != null && !thirdPhase.isEmpty()) {
                thirdPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0));
            }
        } else {
            if (secondPhase != null && !secondPhase.isEmpty()) {
                List<IEntityPacket> finalSecondPhase = secondPhase;
                Bukkit.getScheduler().runTaskLater(parent, () -> finalSecondPhase.forEach(packet -> packetAPI
                        .dispatchPacket(packet, target, 0)), 1L);
            }
            if (thirdPhase != null && !thirdPhase.isEmpty()) {
                List<IEntityPacket> finalThirdPhase = thirdPhase;
                Bukkit.getScheduler().runTaskLater(parent, () -> finalThirdPhase.forEach(packet -> packetAPI
                        .dispatchPacket(packet, target, 0)), 2L);
            }
        }
    }

    @Override
    public void spawnTag(Tag t, Player p, IEntityMountPacket mountPacket) {
        if (t.getTarget() == p) return;
        Boolean visible = state.isVisible(t, p);
        visible = visible != null ? visible : t.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory f = packetAPI.getPacketFactory();

        List<IEntityPacket> packets = new LinkedList<>();

        Location loc = t.getTarget().getLocation();
        IHitbox hb = t.getTargetHitbox();
        if (hb != null)
            loc.add(0, (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT, 0);

        lineFactory.updateLocation(loc, t.getBottom());

        if (t.getTarget().isCustomNameVisible() || t.getTarget() instanceof Player) {
            String name = t.getTarget() instanceof Player ?
                    ((Player) t.getTarget()).getDisplayName() : t.getTarget().getCustomName();
            for (ITagController tc : t.getTagControllers(false)) {
                name = tc.getName(t.getTarget(), p, name);
                if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) name = name + ChatColor.RESET;
            }
            lineFactory.updateName(t.getTop(), name);
        } else lineFactory.updateName(t.getTop(), null);

        for (RenderedTagLine l : t.getLines()) {
            String value = l.get(p);

            if (value == null) {
                if (!l.shouldRemoveSpaceWhenNull()) {
                    loc.add(0, LINE_HEIGHT, 0);
                }
                continue;
            }

            lineFactory.updateName(l.getBottom(), value);
            lineFactory.updateLocation(loc, l.getBottom());

            Collections.addAll(packets, f.createObjectSpawnPacket(l.getBottom().getIdentifier()));

            loc.add(0, LINE_HEIGHT, 0);

            state.addSpawnedLine(p, l);
        }

        lineFactory.updateLocation(loc, t.getTop());
        Collections.addAll(packets, f.createObjectSpawnPacket(t.getTop().getIdentifier()));
        Collections.addAll(packets, f.createObjectSpawnPacket(t.getBottom().getIdentifier()));

        packets.forEach(eP -> packetAPI.dispatchPacket(eP, p, 0));
        packetAPI.dispatchPacket(f.createMountPacket(packetAPI.wrap(t.getTarget()), t.getBottom().getIdentifier()),
                p, 1);
        state.addSpawnedTag(p, t);
    }

    @Override
    public IFakeEntity createBottom(Tag tag) {
        return createTop(tag);
    }

    @Override
    public IFakeEntity createTop(Tag tag) {
        double y = 0;
        IHitbox hb = tag.getTargetHitbox();
        if (hb != null)
            y = (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT;
        y += tag.getLines().size() * LINE_HEIGHT;
        return lineFactory.createArmorStand(tag.getTarget().getLocation().add(0, y, 0));
    }

    @Override
    public LinkedList<IFakeEntity> createStack(Tag tag, int addIndex) {
        LinkedList<IFakeEntity> stack = new LinkedList<>();
        double y = 0;
        IHitbox hb = tag.getTargetHitbox();
        if (hb != null)
            y = (hb.getMax().getY() - hb.getMin().getY()) + BOTTOM_LINE_HEIGHT;
        y += addIndex * LINE_HEIGHT;
        stack.add(lineFactory.createArmorStand(tag.getTarget().getLocation().add(0, y, 0)));
        return stack;
    }

    @Override
    public void purge(Tag t) {}

    @Override
    public void purge(IFakeEntity entity) {}
}
