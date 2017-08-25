package net.blitzcube.mlapi.util.packet;

import net.blitzcube.mlapi.util.packet.entity.object.FakeObject;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by iso2013 on 8/24/2017.
 */
public enum ObjectType {
    BOAT(EntityType.BOAT, 1),
    DROPPED_ITEM(EntityType.DROPPED_ITEM, 2),
    AREA_EFFECT_CLOUD(EntityType.AREA_EFFECT_CLOUD, 3),
    MINECART(EntityType.MINECART, 10),
    PRIMED_TNT(EntityType.PRIMED_TNT, 50),
    ENDER_CRYSTAL(EntityType.ENDER_CRYSTAL, 51),
    TIPPED_ARROW(EntityType.TIPPED_ARROW, 60),
    SNOWBALL(EntityType.SNOWBALL, 61),
    EGG(EntityType.EGG, 62),
    FIREBALL(EntityType.FIREBALL, 63),
    SMALL_FIREBALL(EntityType.SMALL_FIREBALL, 64),
    ENDER_PEARL(EntityType.ENDER_PEARL, 65),
    WITHER_SKULL(EntityType.WITHER_SKULL, 66),
    SHULKER_BULLET(EntityType.SHULKER_BULLET, 67),
    FALLING_BLOCK(EntityType.FALLING_BLOCK, 70),
    ITEM_FRAME(EntityType.ITEM_FRAME, 71),
    ENDER_SIGNAL(EntityType.ENDER_SIGNAL, 72),
    SPLASH_POTION(EntityType.SPLASH_POTION, 73),
    THROWN_EXP_BOTTLE(EntityType.THROWN_EXP_BOTTLE, 75),
    FIREWORK(EntityType.FIREWORK, 76),
    LEASH_HITCH(EntityType.LEASH_HITCH, 77),
    ARMOR_STAND(EntityType.ARMOR_STAND, 78),
    FISHING_HOOK(EntityType.FISHING_HOOK, 90),
    SPECTRAL_ARROW(EntityType.SPECTRAL_ARROW, 91),
    DRAGON_FIREBALL(EntityType.DRAGON_FIREBALL, 93);

    static Map<EntityType, ObjectType> entityToObject;

    static {
        entityToObject = new HashMap<>();
        for (ObjectType t : values()) {
            entityToObject.put(t.entityType, t);
        }
    }

    EntityType entityType;
    int id;
    private Class<? extends FakeObject> impl;

    ObjectType(EntityType entityType, int id) {
        this.entityType = entityType;
        this.id = id;
        this.impl = FakeObject.class;
    }

    ObjectType(EntityType entityType, int id, Class<? extends FakeObject> impl) {
        this.entityType = entityType;
        this.id = id;
        this.impl = impl;
    }

    public static ObjectType getBy(EntityType type) {
        return entityToObject.get(type);
    }

    public Class<? extends FakeObject> getImpl() {
        return impl;
    }
}
