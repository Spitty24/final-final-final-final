package dev.freerun.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ethan James Patrick's plasma cannon: an instant cyan bolt that hits the first thing in its
 * path. Ethan pulls it out when someone hurts him; anyone who ends up holding it can fire it too.
 */
public class PlasmaCannonItem extends Item {
    private static final double RANGE = 32.0;

    public PlasmaCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) fire(level, player, player.getEyePosition(), player.getViewVector(1.0f), 8.0f);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    /** Fires one bolt from {@code from} along {@code dir}: a beam of particles and damage to whatever it hits. */
    public static void fire(Level level, LivingEntity shooter, Vec3 from, Vec3 dir, float damage) {
        if (!(level instanceof ServerLevel server)) return;
        Vec3 d = dir.normalize();
        Vec3 end = from.add(d.scale(RANGE));
        HitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, shooter, from, end,
                new AABB(from, end).inflate(1.0), e -> e != shooter && e.isAlive() && e.isPickable() && !e.isSpectator(), 0.3f);
        if (hit != null) end = hit.getLocation();

        double len = from.distanceTo(end);
        DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.3f, 0.95f, 1.0f), 1.4f);
        for (double t = 0.6; t < len; t += 0.35) {
            Vec3 p = from.add(d.scale(t));
            server.sendParticles(cyan, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 14, 0.2, 0.2, 0.2, 0.3);
        server.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.0f, 2.0f);
        level.playSound(null, end.x, end.y, end.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.0f, 1.6f);

        if (hit != null) {
            Entity target = hit.getEntity();
            target.hurt(level.damageSources().indirectMagic(shooter, shooter), damage);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§bRight-click: fire a plasma bolt"));
        tooltip.add(Component.literal("§8Property of Ethan James Patrick"));
    }
}
