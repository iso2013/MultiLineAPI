package net.blitzcube.mlapi.util.packet.entity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.blitzcube.mlapi.util.packet.PacketUtil;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by iso2013 on 8/24/2017.
 */
public class FakeEntity {
    private static final Map<EntityType, Class<? extends FakeEntity>> clazzMap =
            new HashMap<EntityType, Class<? extends FakeEntity>>() {{

            }};
    final Map<Integer, Object> pendingChanges;
    private final int entityID, typeID;
    private final UUID uniqueID;
    private final EntityType type;
    private WrappedDataWatcher metadata = new WrappedDataWatcher();

    public FakeEntity(EntityType type, WrappedDataWatcher metadata, int entityID) {
        this.entityID = entityID;
        this.typeID = type.getTypeId();
        this.uniqueID = UUID.randomUUID();
        this.type = type;
        this.pendingChanges = new HashMap<>();
        pendingChanges.putAll(metadata.asMap());
    }

    public static Class<? extends FakeEntity> getImpl(EntityType type) {
        return clazzMap.getOrDefault(type, FakeEntity.class);
    }

    public static void destroy(Set<FakeEntity> entities, Player... forWho) {
        int[] ids = new int[entities.size()];
        int idx = 0;
        for (FakeEntity e : entities) {
            ids[idx] = e.entityID;
            idx++;
        }
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getModifier().writeDefaults();
        packet.getIntegerArrays().write(0, ids);
        PacketUtil.sendFriendly(packet, forWho);
    }

    public static void setVehicle(int vehicle, Set<FakeEntity> passengers, Player... forWho) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, vehicle);
        int[] passengerIDs = new int[passengers.size()];
        int idx = 0;
        for (FakeEntity e : passengers) {
            passengerIDs[idx] = e.entityID;
        }
        packet.getIntegerArrays().write(0, passengerIDs);
        PacketUtil.sendFriendly(packet, forWho);
    }

    public void spawn(Location location, Player... forWho) {
        spawn(location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), forWho);
    }

    public void spawn(double x, double y, double z, float pitch, float yaw, Player... forWho) {
        PacketUtil.updateWatcher(metadata, pendingChanges);
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
        packet.getIntegers().write(0, entityID);
        packet.getModifier().writeDefaults();
        packet.getUUIDs().write(0, uniqueID);
        packet.getDoubles().write(0, x);
        packet.getDoubles().write(1, y);
        packet.getDoubles().write(2, z);
        packet.getIntegers().write(1, typeID);
        packet.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
        packet.getBytes().write(1, (byte) (pitch * 256.0F / 360.0F));
        packet.getBytes().write(2, (byte) (pitch * 256.0F / 360.0F));
        packet.getIntegers().write(2, 0);
        packet.getIntegers().write(3, 0);
        packet.getIntegers().write(4, 0);
        packet.getDataWatcherModifier().write(0, metadata);
        PacketUtil.sendFriendly(packet, forWho);
    }

    public void updateMetadata(Player... forWho) {
        PacketUtil.updateWatcher(metadata, pendingChanges);
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, entityID);
        packet.getWatchableCollectionModifier().write(0, metadata.getWatchableObjects());
        PacketUtil.sendFriendly(packet, forWho);
    }
}
