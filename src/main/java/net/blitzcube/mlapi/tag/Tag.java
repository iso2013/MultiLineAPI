package net.blitzcube.mlapi.tag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.blitzcube.mlapi.listener.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Tag {
    private final List<Entity> baseEntities;
    private final List<Entity> stack;

    private final TagLine name;
    private final Map<TagController, List<TagLine>> lines;

    private final Entity whoOwns;
    private final Protocol protocol;
    private final Event event;
    private Location entityLoc;

    public Tag(Entity owner, List<TagController> owners) {
        baseEntities = Lists.newArrayList();
        stack = Lists.newArrayList();
        lines = Maps.newHashMap();
        protocol = new Protocol(this);
        event = new Event(this);

        for (TagController r : owners) {
            lines.put(r, Lists.newArrayList());
        }
        whoOwns = owner;

        updateEntityLoc();
        name = new TagLine(this, null);
        if (!(owner instanceof Player)) {
            name.setKeepSpaceWhenNull(false);
            name.setText(null);
        } else {
            name.setText(owner.getName());
        }
        genBase();
        getEntityPairings();
    }

    public TagLine getName() {
        return name;
    }

    public TagLine addLine(TagController owner) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        TagLine newLine = new TagLine(this, owner);
        lines.get(owner).add(newLine);
        return newLine;
    }

    public TagLine addLine(TagController owner, TagLine newLine) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        Preconditions.checkArgument(lines.get(owner).contains(newLine), "Cannot add an instance of TagLine to a Tag " +
                "more than once");
        lines.get(owner).add(newLine);
        return newLine;
    }

    public TagLine getLine(TagController owner, int index) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        Preconditions.checkArgument(index >= 0 && index < lines.get(owner).size(), "Index " + index + " was not found" +
                " in list of size " + lines.get(owner).size());
        return lines.get(owner).get(index);
    }

    public void clear() {
        Set<TagController> owners = Sets.newHashSet(lines.keySet());
        owners.forEach(this::clear);
        lines.clear();
        for (TagController t : owners) {
            lines.put(t, Lists.newArrayList());
        }
    }

    public void clear(TagController owner) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        lines.get(owner).forEach(TagLine::remove);
        lines.get(owner).clear();
    }

    public int getNumLines() {
        int num = 0;
        for (TagController c : lines.keySet()) {
            num += lines.get(c).size();
        }
        return num;
    }

    public int getNumLines(TagController owner) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        return lines.get(owner).size();
    }

    /**
     * Remove a line from this tag object.
     *
     * @param owner The TagController to remove a line of
     * @param line  The TagLine to remove from the tag
     */
    public void removeLine(TagController owner, TagLine line) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with MultiLineAPI" +
                "!");
        lines.get(owner).remove(line);
    }

    /**
     * Remove a line from this tag object based on its index.
     *
     * @param owner The TagController to remove a line of
     * @param index The index of the TagLine that should be removed
     */
    public void removeLine(TagController owner, int index) {
        Preconditions.checkArgument(lines.containsKey(owner), "Controller is not registered for use with " +
                "MultiLineAPI!");
        Preconditions.checkArgument(index >= 0 && index < lines.get(owner).size(), "Index " + index + " was not found" +
                " in list of size " + lines.get(owner).size());
        lines.get(owner).remove(index);
    }

    /**
     * Get an array of entity IDs that the stack is comprised of.
     *
     * @return An array of entity IDs
     */
    public int[] getEntities() {
        return stack.stream().mapToInt(Entity::getEntityId).toArray();
    }

    private void genBase() {
        baseEntities.add(createGenericEntity(EntityType.SILVERFISH));
    }

    public int[][] getEntityPairings() {
        stack.clear();
        stack.add(whoOwns);
        stack.addAll(baseEntities);
        List<TagLine> tagLines = Lists.newArrayList();
        this.lines.entrySet().stream().sorted((o1, o2) -> Integer.compare(o2.getKey()
                .getPriority(), o1.getKey().getPriority())).forEach(v -> tagLines.addAll(v.getValue()));
        Collections.reverse(tagLines);
        for (TagLine line : tagLines) {
            if (line.getText() != null) {
                stack.add(line.getLineEntity());
            }
            if (line.keepSpaceWhenNull() || line.getText() != null) {
                stack.addAll(line.getSpaceEntities());
            }
        }
        stack.add(name.getLineEntity());
        int[] keys = new int[stack.size() - 1];
        int[] values = new int[stack.size() - 1];
        for (int i = 0; i < stack.size(); i++) {
            if (i + 1 < stack.size()) {
                keys[i] = stack.get(i).getEntityId();
                values[i] = stack.get(i + 1).getEntityId();
            }
        }
        return new int[][]{keys, values};
    }

    private LivingEntity createGenericEntity(EntityType type) {
        LivingEntity e = (LivingEntity) entityLoc.getWorld().spawnEntity(entityLoc, type);
        e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1, true, false));
        e.setAI(false);
        e.setCanPickupItems(false);
        e.setCollidable(false);
        e.setGravity(false);
        e.setInvulnerable(true);
        e.setSilent(true);
        e.setMetadata("STACK_ENTITY", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("MultiLineAPI"),
                whoOwns.getUniqueId()));
        return e;
    }

    private LivingEntity createSlime() {
        Slime s = (Slime) createGenericEntity(EntityType.SLIME);
        s.setSize(-1);
        return s;
    }

    LivingEntity createArmorStand() {
        ArmorStand as = (ArmorStand) createGenericEntity(EntityType.ARMOR_STAND);
        as.setMarker(true);
        as.setVisible(false);
        as.setCustomNameVisible(true);
        return as;
    }

    List<Entity> createSpace() {
        List<Entity> space = new ArrayList<>();
        space.add(createSlime());
        space.add(createGenericEntity(EntityType.SILVERFISH));
        space.add(createGenericEntity(EntityType.SILVERFISH));
        return space;
    }

    public void updateEntityLoc() {
        Location l = whoOwns.getLocation();
        l.setY(-10.0D);
        this.entityLoc = l;

        for (Entity e : baseEntities) {
            e.teleport(entityLoc);
        }
        for (List<TagLine> t : lines.values()) {
            for (TagLine t2 : t) {
                t2.teleport(entityLoc);
            }
        }
    }

    /**
     * Get the owner of the Tag
     *
     * @return The owner of the Tag
     */
    public Entity getOwner() {
        return whoOwns;
    }

    /**
     * Remove the tag from the player. This includes removal of the name, lines and
     * the base entities. Clears all localized data
     */
    public void remove() {
        name.remove();
        for (List<TagLine> t : lines.values()) {
            t.forEach(TagLine::remove);
        }
        baseEntities.forEach(Entity::remove);
    }

    public void despawn() {
        if (baseEntities.size() <= 0) return;
        name.despawn();
        for (List<TagLine> t : lines.values()) {
            t.forEach(TagLine::despawn);
        }
        baseEntities.forEach(Entity::remove);

        baseEntities.clear();
        stack.clear();
    }

    public void respawn() {
        if (baseEntities.size() > 0) return;
        updateEntityLoc();

        name.respawn();
        for (List<TagLine> t : lines.values()) {
            t.forEach(TagLine::respawn);
        }
        genBase();

        getEntityPairings();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

        if (name != null ? !name.equals(tag.name) : tag.name != null) return false;
        return whoOwns.equals(tag.whoOwns);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + whoOwns.hashCode();
        return result;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Event getEvent() {
        return event;
    }

    public int[] getEntitiesExceptOwner() {
        return stack.stream().filter(e -> e != whoOwns).mapToInt(Entity::getEntityId).toArray();
    }

    public class Protocol {
        Tag forWhat;
        int[][] pairCache;

        public Protocol(Tag forWhat) {
            this.forWhat = forWhat;
            pairCache = new int[][]{};
        }

        public void sendPairs(Player forWho, PacketListener manager, boolean update) {
            forWhat.updateEntityLoc();
            if (forWho == null) return;
            if (pairCache.length == 0 || update || pairCache.length != (forWhat.getEntities().length - 1)) {
                pairCache = forWhat.getEntityPairings();
            }
            for (int i = 0; i < pairCache[0].length; i++) {
                PacketContainer packet = manager.protocol.createPacket(PacketType.Play.Server.MOUNT);
                packet.getIntegers().write(0, pairCache[0][i]);
                packet.getIntegerArrays().write(0, new int[]{pairCache[1][i]});

                try {
                    manager.protocol.sendServerPacket(forWho, packet);
                } catch (InvocationTargetException e) {
                    manager.inst.getLogger().info("Failed to send hide packet to " + forWho.getName() + "!");
                }
            }
        }

        public void hide(PacketListener manager) {
            int[] entities = forWhat.getEntitiesExceptOwner();
            int dist = manager.getMaxTrackingRange(forWhat.getOwner().getWorld().getName());
            forWhat.getOwner().getNearbyEntities(dist, dist, 250).stream()
                    .filter(e -> e instanceof Player && Bukkit.getPlayer(e.getUniqueId()) != null)
                    .filter(e -> !manager.vnsh.canSee(forWhat.getOwner(), (Player) e))
                    .forEach(e -> hide((Player) e, entities, manager));
            if (forWhat.getOwner() instanceof Player && Bukkit.getPlayer(forWhat.getOwner().getUniqueId()) != null) {
                hide((Player) forWhat.getOwner(), entities, manager);
            }
        }

        private void hide(Player forWho, int[] entities, PacketListener manager) {
            PacketContainer packet = manager.protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntegerArrays().write(0, entities);
            try {
                manager.protocol.sendServerPacket(forWho, packet);
            } catch (InvocationTargetException e) {
                manager.inst.getLogger().info("Failed to send hide packet to " + forWho.getName() + "!");
            }
        }

        public void unhideTags(Player forWho, PacketListener pckt) {
            for (Entity e : forWhat.stack) {
                PacketContainer hidePacket = pckt.protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                WrappedDataWatcher watcher = new WrappedDataWatcher();
                WrappedDataWatcher.WrappedDataWatcherObject object = new WrappedDataWatcher.WrappedDataWatcherObject
                        (3, WrappedDataWatcher.Registry.get(Boolean.class));
                watcher.setObject(object, new WrappedWatchableObject(object, e.isCustomNameVisible()));
                hidePacket.getIntegers().write(0, e.getEntityId());
                hidePacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                try {
                    pckt.protocol.sendServerPacket(forWho, hidePacket);
                } catch (InvocationTargetException e1) {
                    Bukkit.getLogger().info("Failed to send a hide packet to " + forWho.getName());
                    e1.printStackTrace();
                }
            }
        }
    }

    public class Event {
        Tag forWhat;

        public Event(Tag forWhat) {
            this.forWhat = forWhat;
        }

        public void create(PacketListener manager) {
            Bukkit.getScheduler().runTaskLater(manager.inst, () -> resend(manager, true), 1L);
            Bukkit.getScheduler().runTaskLater(manager.inst, () -> forWhat.protocol.hide(manager), 2L);
        }

        public void respawn(PacketListener manager) {
            Bukkit.getScheduler().runTaskLater(manager.inst, () -> {
                forWhat.respawn();
                resend(manager, true);
            }, 2L);
        }

        public void partKilled(PacketListener manager) {
            forWhat.despawn();
            Bukkit.getScheduler().runTaskLater(manager.inst, () -> {
                forWhat.respawn();
                resend(manager, true);
            }, 2L);
        }

        private void resend(PacketListener manager, boolean update) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                forWhat.protocol.sendPairs(p, manager, update);
            }
        }

        public void teleportOrWorldChange(PacketListener manager) {
            forWhat.updateEntityLoc();
            forWhat.despawn();
            forWhat.respawn();
            Bukkit.getScheduler().runTaskLater(manager.inst, () -> forWhat.protocol.hide(manager), 2L);
            for (Player p : Bukkit.getOnlinePlayers()) {
                forWhat.protocol.sendPairs(p, manager, true);
            }
        }
    }
}