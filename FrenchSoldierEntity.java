package dev.freerun.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Hostile to the player; fights US soldiers. */
public class FrenchSoldierEntity extends SoldierEntity {
    public FrenchSoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Faction getFaction() {
        return Faction.FRENCH;
    }
}
