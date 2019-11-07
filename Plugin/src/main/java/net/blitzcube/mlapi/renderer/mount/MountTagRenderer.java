package net.blitzcube.mlapi.renderer.mount;

import net.blitzcube.mlapi.VisibilityStates;
import net.blitzcube.mlapi.api.tag.ITagController;
import net.blitzcube.mlapi.renderer.LineEntityFactory;
import net.blitzcube.mlapi.renderer.TagRenderer;
import net.blitzcube.mlapi.structure.transactions.*;
import net.blitzcube.mlapi.tag.RenderedTagLine;
import net.blitzcube.mlapi.tag.Tag;
import net.iso2013.peapi.api.PacketEntityAPI;
import net.iso2013.peapi.api.entity.EntityIdentifier;
import net.iso2013.peapi.api.entity.fake.FakeEntity;
import net.iso2013.peapi.api.entity.hitbox.Hitbox;
import net.iso2013.peapi.api.packet.EntityDestroyPacket;
import net.iso2013.peapi.api.packet.EntityMountPacket;
import net.iso2013.peapi.api.packet.EntityPacket;
import net.iso2013.peapi.api.packet.EntityPacketFactory;
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

    private final Map<FakeEntity, Entity> tagEntities = new HashMap<>();

    public MountTagRenderer(PacketEntityAPI packetAPI, LineEntityFactory lineFactory, VisibilityStates states,
                            JavaPlugin parent) {
        super(packetAPI, lineFactory, states, parent);
        packetAPI.addListener(new MountTagPacketListener(packetAPI, lineFactory, tagEntities));
    }

    @Override
    public void processTransactions(Collection<StructureTransaction> transactions, Tag tag, Player target) {
        List<EntityPacket> firstPhase = new LinkedList<>(), secondPhase = new LinkedList<>();

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

    private void processTransaction(StructureTransaction transaction, List<EntityPacket> firstPhase,
                                    List<EntityPacket> secondPhase, Tag tag, Player target, boolean spawn) {
        if (tag.getTarget() == target || (!state.isSpawned(target, tag) && !spawn)) return;

        EntityPacketFactory factory = packetAPI.getPacketFactory();
        if (transaction instanceof AddTransaction) {
            AddTransaction transactionAdd = (AddTransaction) transaction;
            if (target == tag.getTarget()) return;

            Location location = tag.getTarget().getLocation();
            Hitbox hitbox = tag.getTargetHitbox();
            if (hitbox != null) {
                location.add(0, (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT - LINE_HEIGHT, 0);
            }

            RenderedTagLine last = null;
            Set<EntityIdentifier> passengers = new HashSet<>();
            for (RenderedTagLine line : transactionAdd.getAdded()) {
                location.add(0, LINE_HEIGHT, 0);
                String value = line.get(target);

                this.lineFactory.updateName(line.getBottom(), value);
                this.lineFactory.updateLocation(location, line.getBottom());
                Collections.addAll(firstPhase, factory.createObjectSpawnPacket(line.getBottom()));

                if (value != null || !line.shouldRemoveSpaceWhenNull()) {
                    EntityIdentifier vehicle = (last == null) ? transactionAdd.getBelow() : last.getStack().getLast();

                    if (!passengers.isEmpty()) {
                        passengers.add(line.getBottom());
                        secondPhase.add(factory.createMountPacket(vehicle,
                                passengers.toArray(new EntityIdentifier[0])));
                        passengers.clear();
                    } else {
                        secondPhase.add(factory.createMountPacket(vehicle, line.getBottom()));
                    }
                    
                    last = line;
                } else {
                    passengers.add(line.getBottom());
                }

                EntityIdentifier previousIdentifier = null;
                for (FakeEntity fe : line.getStack()) {
                    if (previousIdentifier != null) {
                        this.lineFactory.updateLocation(location, fe);
                        firstPhase.add(factory.createEntitySpawnPacket(fe));
                        secondPhase.add(factory.createMountPacket(previousIdentifier, fe));
                    }

                    previousIdentifier = fe;
                }

                if (value != null || !line.shouldRemoveSpaceWhenNull())
                    this.state.addSpawnedLine(target, line);
            }

            EntityIdentifier vehicle = last == null ? transactionAdd.getBelow() : last.getStack().getLast();
            if (!passengers.isEmpty()) {
                passengers.add(transactionAdd.getAbove());
                secondPhase.add(factory.createMountPacket(vehicle, passengers.toArray(new EntityIdentifier[0])));
                passengers.clear();
            } else {
                secondPhase.add(factory.createMountPacket(vehicle, transactionAdd.getAbove()));
            }
        }

        else if (transaction instanceof MoveTransaction) {
            MoveTransaction transactionMove = (MoveTransaction) transaction;
            if (transactionMove.isToSameLevel()) {
                List<EntityIdentifier> identifiers = new LinkedList<>();
                transactionMove.getMoved().forEach(renderedTagLine -> {
                    identifiers.add(renderedTagLine.getBottom());
                    this.lineFactory.updateName(renderedTagLine.getBottom(), null);
                    firstPhase.add(factory.createDataPacket(renderedTagLine.getBottom()));
                });

                identifiers.add(transactionMove.getAbove());
                firstPhase.add(factory.createMountPacket(transactionMove.getBelow(), identifiers.toArray(new EntityIdentifier[0])));
                this.state.getSpawnedLines(target).removeAll(transactionMove.getMoved());
            }
            else {
                RenderedTagLine lastTagLine = null;
                for (RenderedTagLine line : transactionMove.getMoved()) {
                    firstPhase.add(factory.createMountPacket(
                            (lastTagLine != null) ? lastTagLine.getStack().getLast() : transactionMove.getBelow(),
                            line.getBottom()
                    ));

                    lastTagLine = line;
                    this.state.addSpawnedLine(target, line);
                }

                if (lastTagLine != null) {
                    firstPhase.add(factory.createMountPacket(lastTagLine.getStack().getLast(),
                            transactionMove.getAbove()));
                }
            }
        }

        else if (transaction instanceof NameTransaction) {
            ((NameTransaction) transaction).getQueuedNames()
                    .forEach((key, value) -> {
                        this.lineFactory.updateName(key.getBottom(), value);
                        secondPhase.add(factory.createDataPacket(key.getBottom()));
                    });
        }

        else if (transaction instanceof RemoveTransaction) {
            RemoveTransaction transactionRemove = (RemoveTransaction) transaction;
            EntityDestroyPacket destroyPacket = packetAPI.getPacketFactory().createDestroyPacket();
            transactionRemove.getRemoved().forEach(r -> r.getStack().forEach(destroyPacket::addToGroup));

            firstPhase.add(destroyPacket);
            this.state.getSpawnedLines(target).removeAll(transactionRemove.getRemoved());
            firstPhase.add(factory.createMountPacket(transactionRemove.getBelow(), transactionRemove.getAbove()));
        }
    }

    @Override
    public void spawnTag(Tag tag, Player player, EntityMountPacket mountPacket) {
        Entity target = tag.getTarget();
        if (target == player) return;

        Boolean visible = state.isVisible(tag, player);
        visible = (visible != null) ? visible : tag.getDefaultVisible();
        if (!visible) return;

        EntityPacketFactory factory = packetAPI.getPacketFactory();
        List<EntityPacket> firstPhase = new LinkedList<>(), secondPhase = new LinkedList<>();

        Location location = target.getLocation();
        Hitbox hitbox = tag.getTargetHitbox();
        if (hitbox != null) {
            location.add(0, (hitbox.getMax().getY() - hitbox.getMin().getY()) + BOTTOM_LINE_HEIGHT, 0);
        }

        FakeEntity top = tag.getTop(), bottom = tag.getBottom();
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

        firstPhase.add(factory.createEntitySpawnPacket(bottom));
        Collections.addAll(firstPhase, factory.createObjectSpawnPacket(top));

        if (mountPacket == null) {
            secondPhase.add(factory.createMountPacket(packetAPI.wrap(tag.getTarget()), bottom));
        } else {
            mountPacket.addToGroup(bottom);
        }

        this.processTransaction(new AddTransaction(bottom, top, tag.getLines()),
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
    public LinkedList<FakeEntity> createStack(Tag tag, int addIndex) {
        Location location = tag.getTarget().getLocation();
        Hitbox hitbox = tag.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) : (addIndex * LINE_HEIGHT);
        location.add(0, y, 0);

        LinkedList<FakeEntity> stack = new LinkedList<>();
        stack.add(lineFactory.createArmorStand(location));
        stack.add(lineFactory.createSlime(location));
        stack.add(lineFactory.createSilverfish(location));
        stack.add(lineFactory.createSilverfish(location));

        stack.forEach(e -> tagEntities.put(e, tag.getTarget()));
        return stack;
    }

    @Override
    public void purge(FakeEntity entity) {
        this.tagEntities.remove(entity);
    }

    @Override
    public FakeEntity createBottom(Tag target) {
        return createLineComponent(target, LineEntityFactory::createSilverfish);
    }

    @Override
    public FakeEntity createTop(Tag target) {
        return createLineComponent(target, LineEntityFactory::createArmorStand);
    }

    private FakeEntity createLineComponent(Tag target,
                                           BiFunction<LineEntityFactory, Location, FakeEntity> creationFunction) {
        Hitbox hitbox = target.getTargetHitbox();
        double y = (hitbox != null) ? (hitbox.getMax().getY() - hitbox.getMin().getY()) : 0;

        FakeEntity newEntity = creationFunction.apply(lineFactory, target.getTarget().getLocation().add(0, y + BOTTOM_LINE_HEIGHT, 0));
        this.tagEntities.put(newEntity, target.getTarget());
        return newEntity;
    }
}
