package net.blitzcube.mlapi.util.packet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.base.Preconditions;

import org.bukkit.entity.EntityType;

import net.blitzcube.mlapi.util.packet.entity.FakeEntity;

/**
 * Created by iso2013 on 8/24/2017.
 */
public final class FakeEntityFactory {

    protected FakeEntityFactory() {}

    public FakeEntity create(EntityType type, Map<Integer, Object> metadata) {
        Preconditions.checkArgument(ObjectType.getBy(type) == null, "The EntityType given must be created as an object!");

        Constructor<?> init = null;

        for (Constructor<?> c : FakeEntity.getImpl(type).getConstructors()) {
            if (c.getParameterCount() != 3) continue;
            Class<?>[] params;
            if ((params = c.getParameterTypes())[0] != EntityType.class) continue;
            if (params[1] != WrappedDataWatcher.class) continue;
            if (params[2] != int.class) continue;
            init = c;
        }

        try {
            if (init == null) {
                throw new IllegalArgumentException("Cannot create EntityType " + type.name() + " - No valid " +
                        "implementation found, no constructor with the parameters ProtocolManager, EntityType, " +
                        "WrappedDataWatcher, and int was found.");
            }

            return (FakeEntity) init.newInstance(type, metadata, PacketUtil.getNextEntityID());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public FakeEntity create(EntityType type) {
        return create(type, null);
    }

    public FakeEntity create(ObjectType type, int objectData, Map<Integer, Object> metadata) {
        Constructor<?> init = null;

        for (Constructor<?> c : type.getImpl().getConstructors()) {
            if (c.getParameterCount() != 5) continue;
            Class<?>[] params;
            if ((params = c.getParameterTypes())[0] != EntityType.class) continue;
            if (params[1] != int.class) continue;
            if (params[2] != WrappedDataWatcher.class) continue;
            if (params[3] != int.class) continue;
            init = c;
        }

        try {
            if (init == null) {
                throw new IllegalArgumentException("Cannot create EntityType " + type.name() + " - No valid " +
                        "implementation found, no constructor with the parameters ProtocolManager, EntityType, " +
                        "Integer, and WrappedDataWatcher was found.");
            }

            return (FakeEntity) init.newInstance(type, objectData, metadata, PacketUtil.getNextEntityID());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public FakeEntity create(ObjectType type, int objectData) {
        return create(type, objectData, null);
    }

    /**
     * Deprecated in favor of {@link #create(ObjectType, int, Map)}
     */
    @Deprecated
    public FakeEntity create(EntityType type, int objectData, Map<Integer, Object> metadata) {
        Preconditions.checkArgument(ObjectType.getBy(type) != null, "The given type must be created as an entity!");
        return create(ObjectType.getBy(type), objectData, metadata);
    }

    @Deprecated
    public FakeEntity create(EntityType type, int objectData) {
        Preconditions.checkArgument(ObjectType.getBy(type) != null, "The given type must be created as an entity!");
        return create(ObjectType.getBy(type), objectData);
    }

}
