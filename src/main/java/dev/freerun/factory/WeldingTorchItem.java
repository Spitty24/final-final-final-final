package dev.freerun.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MIG welding gun. Needs a Welder machine within 12 blocks.
 *   click a part            select it (it glows)
 *   click a touching part   weld the two into one rigid assembly
 *   sneak + click a part    cut all of its welds apart
 *   click the air           drop your selection
 */
public class WeldingTorchItem extends Item {
    private static final int WELDER_RANGE = 12;
    /** How close two parts must be to count as touching. */
    private static final double TOUCH = 0.3;
    private static final Map<UUID, Integer> SELECTED = new ConcurrentHashMap<>();

    public WeldingTorchItem(Properties properties) {
        super(properties);
    }

    public static void forget(UUID player) {
        SELECTED.remove(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            SteelPartEntity sel = selected(level, player);
            if (sel != null) {
                sel.setGlowingTag(false);
                SELECTED.remove(player.getUUID());
                player.displayClientMessage(Component.literal("§7Selection cleared."), true);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private static SteelPartEntity selected(Level level, Player player) {
        Integer id = SELECTED.get(player.getUUID());
        if (id == null) return null;
        Entity e = level.getEntity(id);
        return e instanceof SteelPartEntity part && part.isAlive() ? part : null;
    }

    static boolean welderNearby(Level level, BlockPos center) {
        return BlockPos.findClosestMatch(center, WELDER_RANGE, WELDER_RANGE / 2,
                p -> level.getBlockState(p).is(FactoryRegistry.WELDER)).isPresent();
    }

    /** Server side: the player clicked {@code part} with the torch. */
    public static void useOnPart(Player player, SteelPartEntity part) {
        Level level = part.level();
        if (!welderNearby(level, player.blockPosition())) {
            player.displayClientMessage(Component.literal(
                    "§cNo power. Stay within 12 blocks of a Welder machine."), true);
            level.playSound(null, part.getX(), part.getY(), part.getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.3f, 2.0f);
            return;
        }

        if (player.isShiftKeyDown()) {
            if (part.partCount() <= 1) {
                player.displayClientMessage(Component.literal("§7Nothing welded there to cut."), true);
                return;
            }
            int n = part.partCount();
            part.setGlowingTag(false);
            sparks(level, part.getBoundingBox().getCenter(), 30);
            part.split();
            player.displayClientMessage(Component.literal("§6Cut the welds: " + n + " loose parts."), true);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) dev.freerun.career.Careers.addXp(sp, 2, "cutting");
            return;
        }

        SteelPartEntity first = selected(level, player);
        if (first == null) {
            select(player, part);
            return;
        }
        first.setGlowingTag(false);
        SELECTED.remove(player.getUUID());
        if (first == part) {
            player.displayClientMessage(Component.literal("§7Selection cleared."), true);
            return;
        }
        if (!first.getBoundingBox().inflate(TOUCH).intersects(part.getBoundingBox())) {
            player.displayClientMessage(Component.literal(
                    "§eThose aren't touching. Line them up (the Gravity Gun helps) and try again."), true);
            select(player, part);
            return;
        }
        if (first.partCount() + part.partCount() > SteelPartEntity.MAX_PARTS) {
            player.displayClientMessage(Component.literal(
                    "§cToo big: an assembly can have at most " + SteelPartEntity.MAX_PARTS + " parts."), true);
            return;
        }

        Vec3 mid = first.getBoundingBox().getCenter().add(part.getBoundingBox().getCenter()).scale(0.5);
        first.weldWith(part);
        sparks(level, mid, 40);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.6f);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 2.0f, 1.8f);
        player.displayClientMessage(Component.literal(
                "§aWelded! §7The assembly has " + first.partCount() + " parts."), true);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) dev.freerun.career.Careers.addXp(sp, 8, "welding");
    }

    private static void select(Player player, SteelPartEntity part) {
        part.setGlowingTag(true);
        SELECTED.put(player.getUUID(), part.getId());
        player.displayClientMessage(Component.literal(
                "§bSelected a " + (part.partCount() > 1 ? "welded assembly" : part.getParts().get(0).type().displayName())
                        + ". §7Now click a part touching it."), true);
        part.level().playSound(null, part.getX(), part.getY(), part.getZ(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    static void sparks(Level level, Vec3 at, int count) {
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count, 0.25, 0.15, 0.25, 0.15);
        server.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, Math.max(2, count / 10), 0.2, 0.05, 0.2, 0.0);
        server.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.2, at.z, count / 4, 0.2, 0.1, 0.2, 0.02);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Click a part, then one touching it, to weld."));
        tooltip.add(Component.literal("§7Sneak-click to cut welds apart."));
        tooltip.add(Component.literal("§8Needs a Welder within 12 blocks."));
    }
}
