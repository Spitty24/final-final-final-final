package dev.freerun.evil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A shadowy runner that hunts the player. It is faster than a normal mob and its speed
 * ramps up over time (set each tick from {@link PursuerManager}). "Catching" the player is
 * handled by {@link PursuerManager}, not by vanilla melee damage, so the caught behaviour
 * (fake-out / scatter / cursed respawn) is configurable.
 */
public class PursuerEntity extends PathfinderMob {
    public float trailPhase;

    public PursuerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 128.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setRampedSpeed(double speed) {
        var attr = getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(speed);
    }

    private int griefCooldown;

    @Override
    public void aiStep() {
        super.aiStep();
        trailPhase += 0.3f;
        if (level() instanceof ServerLevel server && getTarget() != null) {
            griefAndParkour(server, getTarget());
        }
    }

    /**
     * Lets the Pursuer get through parkour terrain the way a player would, by force:
     * it breaks blocks in its path, bridges gaps, and pillars up walls toward its target.
     * Deliberately short-range so it only reshapes terrain right around itself.
     */
    private void griefAndParkour(ServerLevel level, LivingEntity target) {
        EvilConfig cfg = EvilConfig.get();
        if (!cfg.pursuerGriefs) return;
        if (griefCooldown > 0) {
            griefCooldown--;
            return;
        }

        double dx = target.getX() - getX();
        double dy = target.getY() - getY();
        double dz = target.getZ() - getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz > cfg.pursuerGriefRange) return;

        Direction face = Direction.fromYRot(Mth.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
        BlockPos self = blockPosition();
        BlockPos ahead = self.relative(face);
        boolean acted = false;

        // Target is above and we're close: pillar straight up toward it.
        if (dy > 1.2 && horiz < 2.0) {
            getJumpControl().jump();
            setDeltaMovement(getDeltaMovement().add(0, 0.25, 0));
            placeIfAir(level, self);               // block fills our feet-space as we rise
            acted = true;
        } else {
            // Break a wall in front (feet + head height).
            if (horiz > 1.0) {
                acted = breakIfBreakable(level, ahead) | breakIfBreakable(level, ahead.above());
            }
            // Bridge a gap in front when the target isn't below us.
            BlockPos aheadFloor = ahead.below();
            if (dy > -1.0 && level.getBlockState(aheadFloor).isAir() && level.getBlockState(ahead).isAir()) {
                acted |= placeIfAir(level, aheadFloor);
            }
            // Big horizontal gap and roughly level: leap across.
            if (horiz > 2.5 && Math.abs(dy) < 1.5 && onGround()) {
                Vec3 toward = new Vec3(dx, 0, dz).normalize().scale(0.5);
                setDeltaMovement(toward.x, 0.42, toward.z);
                acted = true;
            }
        }

        if (acted) griefCooldown = 4;
    }

    private boolean breakIfBreakable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || state.is(Blocks.BEDROCK)) return false;   // skip unbreakable
        level.destroyBlock(pos, true);
        return true;
    }

    private boolean placeIfAir(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Immune to normal harm — it is a scripted threat, not a fair fight. Only a creative-mode player can remove it. */
    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !source.isCreativePlayer() || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return source.isCreativePlayer() && super.hurt(source, amount);
    }

    public LivingEntity quarry() {
        return getTarget();
    }
}
