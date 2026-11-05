package dev.freerun.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Friendly to the player; fights hostile mobs. */
public class UsSoldierEntity extends SoldierEntity {
    public UsSoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Faction getFaction() {
        return Faction.US;
    }
}
