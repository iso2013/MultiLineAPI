package net.blitzcube.mlapi.renderer;

import net.blitzcube.peapi.api.entity.fake.IFakeEntity;
import net.blitzcube.peapi.api.entity.fake.IFakeEntityFactory;
import net.blitzcube.peapi.api.entity.hitbox.IHitbox;
import net.blitzcube.peapi.api.entity.modifier.IEntityModifier;
import net.blitzcube.peapi.api.entity.modifier.IEntityModifierRegistry;
import net.blitzcube.peapi.api.entity.modifier.IModifiableEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Created by iso2013 on 6/4/2018.
 */
public class LineEntityFactory {
    //Armor stand modifiers
    private final IEntityModifier<Boolean> armorStandInvisible;
    private final IEntityModifier<String> name;
    private final IEntityModifier<Boolean> nameVisible;
    private final IEntityModifier<Boolean> marker;

    //Slime modifiers
    private final IEntityModifier<Integer> size;

    //Silverfish modifiers
    private final IEntityModifier<Boolean> silent;
    private final IEntityModifier<Boolean> noAI;

    //Entity modifiers
    private final IEntityModifier<Boolean> entityInvisible;

    private final IFakeEntityFactory factory;

    public LineEntityFactory(IEntityModifierRegistry reg, IFakeEntityFactory factory) {
        this.factory = factory;
        this.name = reg.lookup(EntityType.ARMOR_STAND, "CUSTOM_NAME", String.class);
        this.nameVisible = reg.lookup(EntityType.ARMOR_STAND, "CUSTOM_NAME_VISIBLE", Boolean.class);
        this.size = reg.lookup(EntityType.SLIME, "SIZE", Integer.class);
        this.marker = reg.lookup(EntityType.ARMOR_STAND, "MARKER", Boolean.class);
        this.entityInvisible = reg.lookup(EntityType.SILVERFISH, "INVISIBLE", Boolean.class);
        this.armorStandInvisible = reg.lookup(EntityType.ARMOR_STAND, "INVISIBLE", Boolean.class);
        this.noAI = reg.lookup(EntityType.SILVERFISH, "NO_AI", Boolean.class);
        this.silent = reg.lookup(EntityType.SILVERFISH, "SILENT", Boolean.class);
    }

    public IFakeEntity createArmorStand(Location l) {
        IFakeEntity e = factory.createFakeEntity(EntityType.ARMOR_STAND);
        e.setLocation(l);
        IModifiableEntity m = e.getModifiableEntity();
        armorStandInvisible.setValue(m, true);
        marker.setValue(m, true);
        return e;
    }

    public IFakeEntity createSlime(Location l) {
        IFakeEntity e = factory.createFakeEntity(EntityType.SLIME);
        e.setLocation(l);
        IModifiableEntity m = e.getModifiableEntity();
        entityInvisible.setValue(m, true);
        size.setValue(m, -1);
        return e;
    }

    public IFakeEntity createSilverfish(Location l) {
        IFakeEntity e = factory.createFakeEntity(EntityType.SILVERFISH);
        e.setLocation(l);
        IModifiableEntity m = e.getModifiableEntity();
        entityInvisible.setValue(m, true);
        silent.setValue(m, true);
        noAI.setValue(m, true);
        return e;
    }

    public void updateName(IFakeEntity entity, String newName) {
        if (newName != null) {
            nameVisible.setValue(entity.getModifiableEntity(), true);
            name.setValue(entity.getModifiableEntity(), newName);
        } else {
            name.setValue(entity.getModifiableEntity(), ":D");
            nameVisible.setValue(entity.getModifiableEntity(), false);
        }
    }

    public void updateLocation(Location l, IFakeEntity e) {
        e.setLocation(l);
    }

    public IHitbox getHitbox(Entity newEntity) {
        return factory.createHitboxFromEntity(newEntity);
    }
}
