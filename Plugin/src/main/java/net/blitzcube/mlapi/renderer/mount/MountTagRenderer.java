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
import java.util.function.BiFunction;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class MountTagRenderer extends TagRenderer {

    private final Map<IFakeEntity, Entity> tagEntities = new HashMap<>();

    public MountTagRenderer(IPacketEntityAPI packetAPI, LineEntityFactory lineFactory, VisibilityStates states, JavaPlugin parent) {
        super(packetAPI, lineFactory, states, parent);
        packetAPI.addListener(new MountTagPacketListener(packetAPI, lineFactory, tagEntities));
    }

    @Override
    public void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target) {
        List<IEntityPacket> firstPhase = new LinkedList<>(), secondPhase = new LinkedList<>();

        for (StructureTransaction transaction : transactions) {
            this.processTransaction(transaction, firstPhase, secondPhase, tag, target, false);
        }

        if (!firstPhase.isEmpty()) {
            firstPhase.forEach(i -> packetAPI.dispatchPacket(i, target, 0));
        }
        if (!secondPhase.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(parent, () -> secondPhase.forEach(i -> packetAPI.dispatchPacket(i, target, 0)), 1L);
        }
    }

    private void processTransaction(StructureTransaction transaction, List<IEntityPacket> firstPhase,
                                    List<IEntityPacket> secondPhase, Tag tag, Player target, boolean spawn) {
        if (tag.getTarget() == target || (!state.isSpawned(target, tag) && !spawn)) return;

        IEntityPacketFactory factory = packetAPI.getPacketFactory();
        if (transaction instanceof AddTransaction) {
            AddTransaction transactionAdd = (AddTransaction) transaction;
            if (target == tag.getTarget()) return;

            Location location = tag.getTarget().getLocation();
            IHitbox hitbox = tag.getTargetHitbox();
            if (hitbox != null) {
                location.add(0, (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);
            }

            RenderedTagLine last = null;
            Set<IEntityIdentifier> passengers = new HashSet<>();
            for (RenderedTagLine line : transactionAdd.getAdded()) {
                location.add(0, LINE_HEIGHT, 0);
                String value = line.get(target);

                this.lineFactory.updateName(line.getBottom(), value);
                this.lineFactory.updateLocation(location, line.getBottom());
                Collections.addAll(firstPhase, factory.createObjectSpawnPacket(line.getBottom().getIdentifier()));

                if (value != null || !line.shouldRemoveSpaceWhenNull()) {
                    IEntityIdentifier vehicle = (last == null) ? transactionAdd.getBelow() : last.getStack().getLast().getIdentifier();

                    if (!passengers.isEmpty()) {
                        passengers.add(line.getBottom().getIdentifier());
                        secondPhase.add(factory.createMountPacket(vehicle, passengers.toArray(new IEntityIdentifier[0])));
                        passengers.clear();
                    } else {
                        secondPhase.add(factory.createMountPacket(vehicle, line.getBottom().getIdentifier()));
                    }
                    
                    last = line;
                } else {
                    passengers.add(line.getBottom().getIdentifier());
                }

                IEntityIdentifier previousIdentifier = null;
                for (IFakeEntity fe : line.getStack()) {
                    if (previousIdentifier != null) {
                        this.lineFactory.updateLocation(location, fe);
                        firstPhase.add(factory.createEntitySpawnPacket(fe.getIdentifier()));
                        secondPhase.add(factory.createMountPacket(previousIdentifier, fe.getIdentifier()));
                    }

                    previousIdentifier = fe.getIdentifier();
                }

                this.state.addSpawnedLine(target, line);
            }

            IEntityIdentifier vehicle = last == null ? transactionAdd.getBelow() : last.getStack().getLast().getIdentifier();
            if (!passengers.isEmpty()) {
                passengers.add(transactionAdd.getAbove());
                secondPhase.add(factory.createMountPacket(vehicle, passengers.toArray(new IEntityIdentifier[0])));
                passengers.clear();
            } else {
                secondPhase.add(factory.createMountPacket(vehicle, transactionAdd.getAbove()));
            }
        }

        else if (transaction instanceof MoveTransaction) {
            MoveTransaction transactionMove = (MoveTransaction) transaction;
            if (transactionMove.isToSameLevel()) {
                List<IEntityIdentifier> identifiers = new LinkedList<>();
                transactionMove.getMoved().forEach(renderedTagLine -> {
                    identifiers.add(renderedTagLine.getBottom().getIdentifier());
                    this.lineFactory.updateName(renderedTagLine.getBottom(), null);
                    firstPhase.add(factory.createDataPacket(renderedTagLine.getBottom().getIdentifier()));
                });

                identifiers.add(transactionMove.getAbove());
                firstPhase.add(factory.createMountPacket(transactionMove.getBelow(), identifiers.toArray(new IEntityIdentifier[0])));
                this.state.getSpawnedLines(target).removeAll(transactionMove.getMoved());
            }
            else {
                RenderedTagLine lastTagLine = null;
                for (RenderedTagLine line : transactionMove.getMoved()) {
                    firstPhase.add(factory.createMountPacket(
                            (lastTagLine != null) ? lastTagLine.getStack().getLast().getIdentifier() : transactionMove.getBelow(),
                            line.getBottom().getIdentifier()
                    ));

                    lastTagLine = line;
                    this.state.addSpawnedLine(target, line);
                }

                if (lastTagLine != null) {
                    firstPhase.add(factory.createMountPacket(lastTagLine.getStack().getLast().getIdentifier(), transactionMove.getAbove()));
                }
            }
        }

        else if (transaction instanceof NameTransaction) {
            ((NameTransaction) transaction).getQueuedNames()
                    .forEach((key, value) -> {
                        this.lineFactory.updateName(key.getBottom(), value);
                        secondPhase.add(factory.createDataPacket(key.getBottom().getIdentifier()));
                    });
        }

        else if (transaction instanceof RemoveTransaction) {
            RemoveTransaction transactionRemove = (RemoveTransaction) transaction;
            IEntityDestroyPacket destroyPacket = packetAPI.getPacketFactory().createDestroyPacket();
            transactionRemove.getRemoved().forEach(r -> r.getStack().forEach(e -> destroyPacket.addToGroup(e.getIdentifier())));

            firstPhase.add(destroyPacket);
            this.state.getSpawnedLines(target).removeAll(transactionRemove.getRemoved());
            firstPhase.add(factory.createMountPacket(transactionRemove.getBelow(), transactionRemove.getAbove()));
        }
    }

    @Override
    public void spawnTag(Tag tag, Player player, IEntityMountPacket mountPacket) {
        Entity target = tag.getTarget();
        if (target == player) return;

        Boolean visible = state.isVisible(tag, player);
        visible = (visible != null) ? visible : tag.getDefaultVisible();
        if (!visible) return;

        IEntityPacketFactory factory = packetAPI.getPacketFactory();
        List<IEntityPacket> firstPhase = new LinkedList<>(), secondPhase = new LinkedList<>();

        Location location = target.getLocation();
        IHitbox hitbox = tag.getTargetHitbox();
        if (hitbox != null) {
            location.add(0, (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT, 0);
        }

        IFakeEntity top = tag.getTop(), bottom = tag.getBottom();
        this.lineFactory.updateLocation(location, top);
        this.lineFactory.updateLocation(location, bottom);

        if (target.isCustomNameVisible() || target instanceof Player) {
            String name = (target instanceof Player) ? ((Player) target).getDisplayName() : target.getCustomName();
            for (ITagController controller : tag.getTagControllers(false)) {
                name = controller.getName(tag.getTarget(), player, name);
                if (name != null && name.contains(ChatColor.COLOR_CHAR + "")) name += ChatColor.RESET;
            }

            this.lineFactory.updateName(top, name);
        } else {
            this.lineFactory.updateName(top, null);
        }

        firstPhase.add(factory.createEntitySpawnPacket(bottom.getIdentifier()));
        Collections.addAll(firstPhase, factory.createObjectSpawnPacket(top.getIdentifier()));

        if (mountPacket == null) {
            secondPhase.add(factory.createMountPacket(packetAPI.wrap(tag.getTarget()), bottom.getIdentifier()));
        } else {
            mountPacket.addToGroup(bottom.getIdentifier());
        }

        this.processTransaction(new AddTransaction(bottom.getIdentifier(), top.getIdentifier(), tag.getLines()),
                firstPhase, secondPhase, tag, player, true);

        firstPhase.forEach(ep -> packetAPI.dispatchPacket(ep, player, 0));
        Bukkit.getScheduler().runTask(parent, () -> secondPhase.forEach(ep -> packetAPI.dispatchPacket(ep, player, 0)));
        this.state.addSpawnedTag(player, tag);
    }

    @Override
    public void purge(Tag remove) {
        this.tagEntities.entrySet().removeIf(e -> e.getValue() == remove.getTarget());
    }

    @Override
    public LinkedList<IFakeEntity> createStack(Tag tag, int addIndex) {
        Location location = tag.getTarget().getLocation();
        IHitbox hitbox = tag.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) : (addIndex * LINE_HEIGHT);
        location.add(0, y, 0);

        LinkedList<IFakeEntity> stack = new LinkedList<>();
        stack.add(lineFactory.createArmorStand(location));
        stack.add(lineFactory.createSlime(location));
        stack.add(lineFactory.createSilverfish(location));
        stack.add(lineFactory.createSilverfish(location));

        stack.forEach(e -> tagEntities.put(e, tag.getTarget()));
        return stack;
    }

    @Override
    public void purge(IFakeEntity entity) {
        this.tagEntities.remove(entity);
    }

    @Override
    public IFakeEntity createBottom(Tag target) {
        return createLineComponent(target, LineEntityFactory::createSilverfish);
    }

    @Override
    public IFakeEntity createTop(Tag target) {
        return createLineComponent(target, LineEntityFactory::createArmorStand);
    }

    private IFakeEntity createLineComponent(Tag target, BiFunction<LineEntityFactory, Location, IFakeEntity> creationFunction) {
        IHitbox hitbox = target.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) : 0;

        IFakeEntity newEntity = creationFunction.apply(lineFactory, target.getTarget().getLocation().add(0, y + BOTTOM_LINE_HEIGHT, 0));
        this.tagEntities.put(newEntity, target.getTarget());
        return newEntity;
    }
}
