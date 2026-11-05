package dev.freerun.career;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * One node in a career's skill tree. Tiers 1-5 need levels 1, 3, 6, 9 and 12, and each tier past the
 * first needs at least one skill from the tier before it.
 *
 * @param mods        attribute bonuses, applied while the skill is owned
 * @param everySecond extra effect run once a second (given the seconds online), or null
 */
public record Skill(String id, Career career, int tier, String name, String description, Supplier<Item> icon,
                   List<Mod> mods, BiConsumer<ServerPlayer, Integer> everySecond) {

    public record Mod(Holder<Attribute> attribute, double amount, AttributeModifier.Operation op) {}

    public static final int[] TIER_LEVEL = {1, 3, 6, 9, 12};

    public int requiredLevel() {
        return TIER_LEVEL[tier - 1];
    }
}
