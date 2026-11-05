package dev.freerun.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * The laser cutter's controller. The cutting bed is the 3 x 3 blocks centred on it. A job is a
 * drawn shape: the head pierces each piece, traces every outline with the beam, and when it's done
 * the finished part (plus the leftover skeleton as scrap) comes out on the unload side.
 */
public class LaserCutterBlockEntity extends BlockEntity {
    public static final int OXYGEN = 0, NITROGEN = 1;

    private long[] shape;
    private int gas;
    private long start;
    private int duration;
    private UUID owner;
    /** Cached on the client for the renderer. */
    private List<int[]> path;

    public LaserCutterBlockEntity(BlockPos pos, BlockState state) {
        super(LaserRegistry.LASER_CUTTER_BE, pos, state);
    }

    public boolean busy() {
        return shape != null;
    }

    public long[] shape() {
        return shape;
    }

    public int gas() {
        return gas;
    }

    public long start() {
        return start;
    }

    public int duration() {
        return duration;
    }

    public List<int[]> path() {
        if (shape == null) return List.of();
        if (path == null) path = LaserShape.path(shape);
        return path;
    }

    /** How long a cut takes: piercing each piece, then the outline at the gas's feed rate. */
    public static int cutTicks(long[] s, int gas) {
        double perEdge = gas == OXYGEN ? 0.55 : 0.8;   // oxygen burns through mild steel faster
        return (int) Math.min(900, 40 + LaserShape.pierces(s) * 12 + LaserShape.perimeter(s) * perEdge);
    }

    public void startJob(ServerPlayer by, long[] s, int gas) {
        this.shape = LaserShape.copy(s);
        this.gas = gas;
        this.start = level.getGameTime();
        this.duration = cutTicks(s, gas);
        this.owner = by.getUUID();
        this.path = null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition, LaserRegistry.LASER_START, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /** Where the head is (grid cell, fractional), t from 0 to 1. */
    public static double[] headAt(List<int[]> path, double t) {
        if (path.isEmpty()) return new double[]{LaserShape.N / 2.0, LaserShape.N / 2.0};
        double f = Math.max(0, Math.min(path.size() - 1, t * path.size()));
        int i = (int) f;
        int[] a = path.get(i), b = path.get(Math.min(path.size() - 1, i + 1));
        double k = f - i;
        return new double[]{a[0] + (b[0] - a[0]) * k + 0.5, a[1] + (b[1] - a[1]) * k + 0.5};
    }

    /** Grid cell to world position on the bed (the 3 x 3 blocks around the controller). */
    public double[] world(double gx, double gy) {
        return new double[]{worldPosition.getX() - 1 + gx * 3.0 / LaserShape.N, worldPosition.getY() + 0.8,
                worldPosition.getZ() - 1 + gy * 3.0 / LaserShape.N};
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaserCutterBlockEntity be) {
        if (be.shape == null || !(level instanceof ServerLevel sl)) return;
        try {
            tickJob(sl, pos, be);
        } catch (RuntimeException e) {
            // never take the server down over a cut: abandon the job and give the sheet back
            dev.freerun.Freerun.LOGGER.error("Laser cutter at {} failed", pos, e);
            be.shape = null;
            be.path = null;
            be.setChanged();
            drop(sl, pos.above(), new ItemStack(dev.freerun.factory.FactoryRegistry.STEEL_SHEET));
            sl.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        }
    }

    private static void tickJob(ServerLevel sl, BlockPos pos, LaserCutterBlockEntity be) {
        Level level = sl;
        long age = level.getGameTime() - be.start;
        if (age < 0 || age > be.duration + 200) age = be.duration;   // clock jumped (e.g. /time): just finish
        double t = age / (double) be.duration;
        if (age % 2 == 0 && age < be.duration) {
            double[] g = headAt(be.path(), t);
            double[] w = be.world(g[0], g[1]);
            sl.sendParticles(be.gas == OXYGEN ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, w[0], w[1] - 0.05, w[2],
                    3, 0.03, 0.0, 0.03, 0.05);
            if (age % 6 == 0) sl.sendParticles(ParticleTypes.SMOKE, w[0], w[1] + 0.1, w[2], 1, 0.02, 0.05, 0.02, 0.01);
            if (age % 20 == 0) level.playSound(null, pos, LaserRegistry.LASER_CUT, SoundSource.BLOCKS, 0.6f, 1.0f + level.random.nextFloat() * 0.1f);
        }
        if (age < be.duration) return;
        be.finish(sl);
    }

    private void finish(ServerLevel sl) {
        long[] s = shape;
        ItemStack part = LaserPartItem.make(s, gas);
        int waste = LaserShape.N * LaserShape.N - LaserShape.count(s);
        BlockPos out = worldPosition.above();
        drop(sl, out, part);
        int nuggets = waste / 160;
        if (nuggets > 0) drop(sl, out, new ItemStack(Items.IRON_NUGGET, nuggets));   // the skeleton, as scrap
        sl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35f, 1.6f);
        sl.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
        if (owner != null && sl.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer p) {
            dev.freerun.career.Careers.addXp(p, 4 + LaserShape.perimeter(s) / 12, "Laser-cut a part");
            if (p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 32 * 32) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("§6Cut complete. §7Your part is on the bed."), true);
            }
        }
        shape = null;
        path = null;
        owner = null;
        setChanged();
        sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        TarynEntity.onCutFinished(sl, worldPosition);
    }

    private static void drop(ServerLevel sl, BlockPos at, ItemStack stack) {
        ItemEntity e = new ItemEntity(sl, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, stack);
        e.setDeltaMovement(0, 0.15, 0);
        e.setDefaultPickUpDelay();
        sl.addFreshEntity(e);
    }

    // ------------------------------------------------------------------ save / sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (shape != null) {
            tag.putLongArray("Shape", shape);
            tag.putInt("Gas", gas);
            tag.putLong("Start", start);
            tag.putInt("Duration", duration);
            if (owner != null) tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Shape")) {
            shape = LaserShape.copy(tag.getLongArray("Shape"));
            gas = tag.getInt("Gas");
            start = tag.getLong("Start");
            duration = Math.max(1, tag.getInt("Duration"));
            owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        } else {
            shape = null;
        }
        path = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
