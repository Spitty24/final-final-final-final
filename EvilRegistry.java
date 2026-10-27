package dev.freerun.evil;

import dev.freerun.Freerun;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registers the Pursuer entity type. */
public final class EvilRegistry {
    public static EntityType<PursuerEntity> PURSUER;

    private EvilRegistry() {}

    public static void register() {
        PURSUER = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                Freerun.id("pursuer"),
                EntityType.Builder.of(PursuerEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f)
                        .clientTrackingRange(16)
                        .build("pursuer"));
    }
}
