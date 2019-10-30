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
import org.bukkit.entity.EntityType;
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
        this.packetAPI.addListener(new TeleportTagPacketListener(LINE_HEIGHT, BOTTOM_LINE_HEIGHT, parent, state));
    }

    @Override
    public void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target) {
        if (tag.getTarget() == target || !state.isSpawned(target, tag)) return;

        IEntityPacketFactory factory = packetAPI.getPacketFactory();
        List<IEntityPacket> firstPhase = null, secondPhase = null, thirdPhase = null;

        Location location = null;
        IHitbox hitbox;

        for (StructureTransaction transaction : transactions) {
            if (transaction instanceof MoveTransaction) {
                MoveTransaction transactionMove = (MoveTransaction) transaction;

                if (transactionMove.isToSameLevel()) {
                    transaction = new RemoveTransaction(transactionMove.getBelow(), transactionMove.getAbove(),
                            transactionMove.getMoved());
                } else {
                    transaction = new AddTransaction(transactionMove.getBelow(), transactionMove.getAbove(),
                            transactionMove.getMoved());
                }
            }

            if (transaction instanceof AddTransaction || transaction instanceof RemoveTransaction) {
                boolean add = transaction instanceof AddTransaction;
                Collection<RenderedTagLine> changed = add
                        ? ((AddTransaction) transaction).getAdded()
                        : ((RemoveTransaction) transaction).getRemoved();

                if (location == null) {
                    location = tag.getTarget().getLocation();
                    hitbox = tag.getTargetHitbox();

                    if (hitbox != null) {
                        location.setY(location.getY() + (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT);
                    }
                }

                int index = 0;
                IEntityDestroyPacket destroyPacket = null;
                for (RenderedTagLine line : tag.getLines()) {
                    String value = line.get(target);

                    if (changed.contains(line)) {
                        if (add) {
                            if (value == null) {
                                index++;
                                continue;
                            }

                            if (thirdPhase == null) {
                                thirdPhase = new LinkedList<>();
                            }

                            this.lineFactory.updateName(line.getBottom(), value);
                            this.lineFactory.updateLocation(location.clone().add(0, LINE_HEIGHT * index, 0),
                                    line.getBottom());
                            index++;

                            Collections.addAll(thirdPhase,
                                    factory.createObjectSpawnPacket(line.getBottom()));
                            this.state.addSpawnedLine(target, line);
                        } else {
                            if (destroyPacket == null) {
                                destroyPacket = factory.createDestroyPacket();
                            }

                            destroyPacket.addToGroup(line.getBottom());
                            if (!line.shouldRemoveSpaceWhenNull()) {
                                index++;
                            }
                        }
                    } else if (!state.isLineSpawned(target, line) && !line.shouldRemoveSpaceWhenNull()) {
                        index++;
                    } else {
                        index++;
                    }
                }

                if (destroyPacket != null) {
                    if (firstPhase == null) {
                        firstPhase = new LinkedList<>();
                    }

                    firstPhase.add(destroyPacket);
                    this.state.getSpawnedLines(target).removeAll(changed);
                }
            } else if (transaction instanceof NameTransaction) {
                if (firstPhase == null) {
                    firstPhase = new LinkedList<>();
                }

                List<IEntityPacket> finalFirstPhase = firstPhase;
                ((NameTransaction) transaction).getQueuedNames()
                        .forEach((key, value) -> {
                            this.lineFactory.updateName(key.getBottom(), value);
                            finalFirstPhase.add(factory.createDataPacket(key.getBottom()));
                        });
            }
        }

        if (location != null) {
            secondPhase = new LinkedList<>();

            for (RenderedTagLine line : tag.getLines()) {
                if (!state.isLineSpawned(target, line)) {
                    if (!line.shouldRemoveSpaceWhenNull()) {
                        location.setY(location.getY() + LINE_HEIGHT);
                    }

                    continue;
                }

                secondPhase.add(factory.createMovePacket(line.getBottom(), location.toVector(),
                        null, false, IEntityMovePacket.MoveType.TELEPORT
                ));
                location.setY(location.getY() + LINE_HEIGHT);
            }

            secondPhase.add(factory.createMovePacket(tag.getTop(), location.toVector(),
                    null, false, IEntityMovePacket.MoveType.TELEPORT
            ));
        }

        if (firstPhase != null && !firstPhase.isEmpty()) {
            firstPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0));
        }

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
                Bukkit.getScheduler().runTaskLater(parent,
                        () -> finalSecondPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0)), 1L);
            }

            if (thirdPhase != null && !thirdPhase.isEmpty()) {
                List<IEntityPacket> finalThirdPhase = thirdPhase;
                Bukkit.getScheduler().runTaskLater(parent,
                        () -> finalThirdPhase.forEach(packet -> packetAPI.dispatchPacket(packet, target, 0)), 2L);
            }
        }
    }

    @Override
    public void spawnTag(Tag tag, Player player, IEntityMountPacket mountPacket) {
        if (tag.getTarget() == player) return;
        Boolean visible = state.isVisible(tag, player);
        visible = (visible != null) ? visible : tag.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory factory = packetAPI.getPacketFactory();
        List<IEntityPacket> packets = new LinkedList<>();

        Location location = tag.getTarget().getLocation();
        IHitbox hitbox = tag.getTargetHitbox();
        if (hitbox != null) {
            location.add(0, (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT, 0);
        }

        if (tag.getTarget().isCustomNameVisible() || tag.getTarget() instanceof Player) {
            String name = (tag.getTarget() instanceof Player)
                    ? ((Player) tag.getTarget()).getDisplayName()
                    : tag.getTarget().getCustomName();

            for (ITagController controller : tag.getTagControllers(false)) {
                name = controller.getName(tag.getTarget(), player, name);

                if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) {
                    name += ChatColor.RESET;
                }
            }

            this.lineFactory.updateName(tag.getTop(), name);
        } else {
            this.lineFactory.updateName(tag.getTop(), null);
        }

        if (tag.getBottom() != null) {
            this.lineFactory.updateLocation(location, tag.getBottom());
            Collections.addAll(packets, factory.createEntitySpawnPacket(tag.getBottom()));
            packetAPI.dispatchPacket(
                    factory.createMountPacket(packetAPI.wrap(tag.getTarget()), tag.getBottom()),
                    player,
                    1
            );
        }

        for (RenderedTagLine line : tag.getLines()) {
            String value = line.get(player);

            if (value == null) {
                if (!line.shouldRemoveSpaceWhenNull()) {
                    location.add(0, LINE_HEIGHT, 0);
                }

                continue;
            }

            this.lineFactory.updateName(line.getBottom(), value);
            this.lineFactory.updateLocation(location, line.getBottom());

            Collections.addAll(packets, factory.createObjectSpawnPacket(line.getBottom()));
            location.add(0, LINE_HEIGHT, 0);
            this.state.addSpawnedLine(player, line);
        }

        this.lineFactory.updateLocation(location, tag.getTop());
        Collections.addAll(packets, factory.createObjectSpawnPacket(tag.getTop()));

        packets.forEach(p -> packetAPI.dispatchPacket(p, player, 0));
        this.state.addSpawnedTag(player, tag);
    }

    @Override
    public IFakeEntity createBottom(Tag tag) {
        if (tag.getTarget().getType() == EntityType.PLAYER) {
            IHitbox hitbox = tag.getTargetHitbox();
            double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT : 0;
            return lineFactory.createSilverfish(tag.getTarget().getLocation().add(0, y, 0));
        }
        return null;
    }

    @Override
    public IFakeEntity createTop(Tag tag) {
        IHitbox hitbox = tag.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT : 0;

        y += tag.getLines().size() * LINE_HEIGHT;
        return lineFactory.createArmorStand(tag.getTarget().getLocation().add(0, y, 0));
    }

    @Override
    public LinkedList<IFakeEntity> createStack(Tag tag, int addIndex) {
        LinkedList<IFakeEntity> stack = new LinkedList<>();
        IHitbox hitbox = tag.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT : 0;

        y += addIndex * LINE_HEIGHT;
        stack.add(lineFactory.createArmorStand(tag.getTarget().getLocation().add(0, y, 0)));
        return stack;
    }

    @Override
    public void purge(Tag tag) {}

    @Override
    public void purge(IFakeEntity entity) {}
}
