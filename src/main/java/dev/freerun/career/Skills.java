package dev.freerun.career;

import dev.freerun.factory.FactoryRegistry;
import dev.freerun.food.FoodRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Every career's skill tree: seven skills each, two per tier for tiers 1-3 and a capstone at tier 4.
 *
 *   EMPLOYEE        hands-on: mining, toughness, forklifts, overtime, a packed lunch
 *   SUPERVISOR      running the floor: hitting hard, safety, speed, night vision, motivating others
 *   SUPERINTENDENT  the whole site: speed, heavy equipment, reach, mining ops, fire watch, step-up
 *   UPPER MGMT      perks: gold, health, luck, villager discounts, a golden parachute, bonuses
 */
public final class Skills {
    private static final Map<String, Skill> BY_ID = new LinkedHashMap<>();

    private Skills() {}

    public static Skill get(String id) {
        return BY_ID.get(id);
    }

    public static List<Skill> forCareer(Career career) {
        List<Skill> out = new ArrayList<>();
        for (Skill s : BY_ID.values()) if (s.career() == career) out.add(s);
        return out;
    }

    public static Iterable<Skill> all() {
        return BY_ID.values();
    }

    /** Extra XP from owned skills. */
    public static double xpMultiplier(CareerData d) {
        double m = 1.0;
        if (d.has("employee_of_the_month")) m *= 1.25;
        if (d.has("promotion_track")) m *= 1.4;
        if (d.has("quarterly_bonus")) m *= 1.5;
        return m;
    }

    // ------------------------------------------------------------------ definitions

    static {
        Career c = Career.EMPLOYEE;
        add("hard_worker", c, 1, "Hard Worker", "Mine 15% faster.", () -> Items.IRON_PICKAXE,
                mods(mod(Attributes.BLOCK_BREAK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)), null);
        add("steel_toes", c, 1, "Steel-Toed Boots", "+2 armor, and 25% less fall damage.", () -> Items.IRON_BOOTS,
                mods(mod(Attributes.ARMOR, 2, Operation.ADD_VALUE),
                        mod(Attributes.FALL_DAMAGE_MULTIPLIER, -0.25, Operation.ADD_MULTIPLIED_BASE)), null);
        add("forklift_certified", c, 2, "Forklift Certified", "Forklifts drive 35% faster and their forks lift twice as fast for you.",
                () -> FactoryRegistry.FORKLIFT_ITEM, List.of(), null);
        add("overtime", c, 2, "Overtime", "Earn double XP just for being on the clock.", () -> Items.CLOCK, List.of(), null);
        add("packed_lunch", c, 3, "Packed Lunch", "A fresh link of boudin turns up in your lunchbox every 8 minutes.",
                () -> FoodRegistry.BOUDIN, List.of(), every(480, p -> give(p, new ItemStack(FoodRegistry.BOUDIN), "Your packed lunch: one link of boudin.")));
        add("strong_back", c, 3, "Strong Back", "+2 hearts and 30% knockback resistance.", () -> Items.IRON_CHESTPLATE,
                mods(mod(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE),
                        mod(Attributes.KNOCKBACK_RESISTANCE, 0.3, Operation.ADD_VALUE)), null);
        add("employee_of_the_month", c, 4, "Employee of the Month", "Move 12% faster, mine another 15% faster, and earn 25% more XP.",
                () -> Items.GOLD_BLOCK, mods(mod(Attributes.MOVEMENT_SPEED, 0.12, Operation.ADD_MULTIPLIED_BASE),
                        mod(Attributes.BLOCK_BREAK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)), null);

        c = Career.SUPERVISOR;
        add("clipboard", c, 1, "Clipboard", "+1 attack damage. Writing people up hits hard.", () -> Items.WRITABLE_BOOK,
                mods(mod(Attributes.ATTACK_DAMAGE, 1, Operation.ADD_VALUE)), null);
        add("safety_first", c, 1, "Safety First", "40% less fall damage, and fall 2 blocks further before it hurts.",
                () -> Items.GOLDEN_HELMET, mods(mod(Attributes.FALL_DAMAGE_MULTIPLIER, -0.4, Operation.ADD_MULTIPLIED_BASE),
                        mod(Attributes.SAFE_FALL_DISTANCE, 2, Operation.ADD_VALUE)), null);
        add("walk_the_floor", c, 2, "Walking the Floor", "Move 8% faster.", () -> Items.LEATHER_BOOTS,
                mods(mod(Attributes.MOVEMENT_SPEED, 0.08, Operation.ADD_MULTIPLIED_BASE)), null);
        add("write_up", c, 2, "Write-Up", "Your hits knock things much further back.", () -> Items.PAPER,
                mods(mod(Attributes.ATTACK_KNOCKBACK, 1.0, Operation.ADD_VALUE)), null);
        add("eagle_eye", c, 3, "Eagle Eye", "Permanent night vision. Nothing on the floor gets past you.", () -> Items.SPYGLASS,
                List.of(), (p, s) -> keep(p, MobEffects.NIGHT_VISION, 0, 400));
        add("sick_and_tired", c, 3, "Sick and Tired", "+2 more attack damage and 20% faster attacks.", () -> Items.IRON_SWORD,
                mods(mod(Attributes.ATTACK_DAMAGE, 2, Operation.ADD_VALUE),
                        mod(Attributes.ATTACK_SPEED, 0.2, Operation.ADD_MULTIPLIED_BASE)), null);
        add("promotion_track", c, 4, "Promotion Track", "Earn 40% more XP, and players near you get Speed.", () -> Items.EXPERIENCE_BOTTLE,
                List.of(), (p, s) -> {
                    for (Player o : p.level().players()) {
                        if (o != p && o.distanceToSqr(p) < 12 * 12) keep(o, MobEffects.MOVEMENT_SPEED, 0, 60);
                    }
                });

        c = Career.SUPERINTENDENT;
        add("site_walk", c, 1, "Site Walk", "Move 10% faster.", () -> Items.COMPASS,
                mods(mod(Attributes.MOVEMENT_SPEED, 0.10, Operation.ADD_MULTIPLIED_BASE)), null);
        add("heavy_equipment", c, 1, "Heavy Equipment", "Overhead cranes drive 60% faster for you, and forklift forks lift twice as fast.",
                () -> FactoryRegistry.CRANE_PENDANT, List.of(), null);
        add("long_reach", c, 2, "Long Reach", "Reach blocks 2 further and entities 1 further.", () -> Items.BLAZE_ROD,
                mods(mod(Attributes.BLOCK_INTERACTION_RANGE, 2, Operation.ADD_VALUE),
                        mod(Attributes.ENTITY_INTERACTION_RANGE, 1, Operation.ADD_VALUE)), null);
        add("mining_ops", c, 2, "Mining Operations", "Mine 25% faster.", () -> Items.DIAMOND_PICKAXE,
                mods(mod(Attributes.BLOCK_BREAK_SPEED, 0.25, Operation.ADD_MULTIPLIED_BASE)), null);
        add("fire_watch", c, 3, "Fire Watch", "Permanent fire resistance.", () -> Items.MAGMA_CREAM,
                List.of(), (p, s) -> keep(p, MobEffects.FIRE_RESISTANCE, 0, 120));
        add("step_up", c, 3, "Step Up", "Walk straight up full blocks without jumping.", () -> Items.OAK_STAIRS,
                mods(mod(Attributes.STEP_HEIGHT, 0.5, Operation.ADD_VALUE)), null);
        add("shutdown_authority", c, 4, "Shutdown Authority", "Permanent Haste II and +2 hearts. The site runs on your say-so.",
                () -> Items.BELL, mods(mod(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE)),
                (p, s) -> keep(p, MobEffects.DIG_SPEED, 1, 120));

        c = Career.UPPER_MANAGEMENT;
        add("expense_account", c, 1, "Expense Account", "Collect 2 gold ingots every 10 minutes.", () -> Items.GOLD_INGOT,
                List.of(), every(600, p -> give(p, new ItemStack(Items.GOLD_INGOT, 2), "Expense account: 2 gold ingots reimbursed.")));
        add("corner_office", c, 1, "Corner Office", "+2 hearts. The view is great for your health.", () -> Items.PAINTING,
                mods(mod(Attributes.MAX_HEALTH, 4, Operation.ADD_VALUE)), null);
        add("synergy", c, 2, "Synergy", "+2 luck: better fishing and better loot.", () -> Items.RABBIT_FOOT,
                mods(mod(Attributes.LUCK, 2, Operation.ADD_VALUE)), null);
        add("executive_presence", c, 2, "Executive Presence", "Permanent Hero of the Village: villagers give you discounts.",
                () -> Items.EMERALD, List.of(), (p, s) -> keep(p, MobEffects.HERO_OF_THE_VILLAGE, 0, 120));
        add("golden_parachute", c, 3, "Golden Parachute", "Whenever you fall, you float gently down instead.", () -> Items.FEATHER,
                List.of(), null);   // checked every tick in Careers
        add("quarterly_bonus", c, 3, "Quarterly Bonus", "Earn 50% more XP, plus a diamond every 30 minutes.", () -> Items.DIAMOND,
                List.of(), every(1800, p -> give(p, new ItemStack(Items.DIAMOND), "Quarterly bonus: one diamond.")));
        add("hostile_takeover", c, 4, "Hostile Takeover", "+4 attack damage, +4 armor, and slow constant regeneration.",
                () -> Items.NETHERITE_SWORD, mods(mod(Attributes.ATTACK_DAMAGE, 4, Operation.ADD_VALUE),
                        mod(Attributes.ARMOR, 4, Operation.ADD_VALUE)),
                (p, s) -> keep(p, MobEffects.REGENERATION, 0, 120));
    }

    // ------------------------------------------------------------------ helpers

    private static void add(String id, Career career, int tier, String name, String description, Supplier<net.minecraft.world.item.Item> icon,
                            List<Skill.Mod> mods, BiConsumer<ServerPlayer, Integer> everySecond) {
        BY_ID.put(id, new Skill(id, career, tier, name, description, icon, mods, everySecond));
    }

    private static Skill.Mod mod(Holder<Attribute> attribute, double amount, Operation op) {
        return new Skill.Mod(attribute, amount, op);
    }

    @SafeVarargs
    private static <T> List<T> mods(T... mods) {
        return List.of(mods);
    }

    private static BiConsumer<ServerPlayer, Integer> every(int seconds, java.util.function.Consumer<ServerPlayer> action) {
        return (p, s) -> {
            if (s > 0 && s % seconds == 0) action.accept(p);
        };
    }

    private static void give(ServerPlayer p, ItemStack stack, String message) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        p.displayClientMessage(Component.literal("§6" + message), false);
    }

    /** Keeps an effect topped up without the flicker of letting it run low. */
    static void keep(Player p, Holder<MobEffect> effect, int amplifier, int duration) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur == null || cur.getAmplifier() < amplifier || cur.getDuration() < duration / 2) {
            p.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        }
    }
}
