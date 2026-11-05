package dev.freerun.garage;

import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The HawkSight aligner's brain. Like the real camera aligners it's modelled on, there's nothing
 * electronic on the wheels: passive targets clamp to the tyres, the camera beam finds them, a short
 * roll of the car ("rolling compensation") cancels out any wobble in the clamps, and from then on
 * the cameras read camber, caster and toe live while you turn the adjusters.
 *
 *   MOUNT    targets clamp on, wheel by wheel
 *   COMP     the car rolls back half a block and forward again
 *   MEASURE  the camera beam rides up to the targets' height and locks on
 *   LIVE     readings on screen; adjust until everything's green, then press Finish
 *   REMOVE   targets come off
 */
public class AlignerBlockEntity extends BlockEntity {
    public static final int IDLE = 0, MOUNT = 1, COMP = 2, MEASURE = 3, LIVE = 4, REMOVE = 5;
    public static final int[] LENGTH = {0, 60, 80, 40, 0, 40};

    private int phase = IDLE;
    private long phaseStart;
    private int carId = -1;
    private UUID carUuid;
    private UUID operator;
    private Vec3 compFrom;
    private float beam = 1.6f;

    public AlignerBlockEntity(BlockPos pos, BlockState state) {
        super(GarageRegistry.ALIGNER_BE, pos, state);
    }

    public int phase() {
        return phase;
    }

    public long phaseStart() {
        return phaseStart;
    }

    public int carId() {
        return carId;
    }

    /** Camera beam height above the console's base, where it's parked or locked on. */
    public float beam() {
        return beam;
    }

    public Direction facing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** The car in front of the console: within 9 blocks ahead, 4 to either side. */
    public static CarEntity findCar(Level level, BlockPos pos, Direction facing) {
        Vec3 c = Vec3.atCenterOf(pos);
        Vec3 f = Vec3.atLowerCornerOf(facing.getNormal());
        AABB box = new AABB(c, c.add(f.scale(9))).inflate(Math.abs(f.x) > 0 ? 0.5 : 4, 3, Math.abs(f.z) > 0 ? 0.5 : 4)
                .inflate(Math.abs(f.x) > 0 ? 0 : 0, 0, 0);
        if (Math.abs(f.x) > 0) box = box.inflate(0, 0, 3.5);
        else box = box.inflate(3.5, 0, 0);
        return level.getEntitiesOfClass(CarEntity.class, box, e -> e.isAlive())
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
    }

    private CarEntity car() {
        if (!(level instanceof ServerLevel sl) || carUuid == null) return null;
        Entity e = sl.getEntity(carUuid);
        return e instanceof CarEntity car && car.isAlive() && car.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 14 * 14 ? car : null;
    }

    /** Right-click on the console. */
    public void use(ServerPlayer player) {
        if (phase == LIVE) {
            CarEntity car = car();
            if (car == null) {
                reset();
                player.displayClientMessage(Component.literal("§cLost the car. Start again."), true);
                return;
            }
            openScreen(player, car);
            return;
        }
        if (phase != IDLE) {
            player.displayClientMessage(Component.literal("§7Hold on, it's still working..."), true);
            return;
        }
        CarEntity car = findCar(level, worldPosition, facing());
        if (car == null) {
            player.displayClientMessage(Component.literal("§cNo vehicle in front of the cameras. §7Drive one onto the lift in front of the console."), true);
            return;
        }
        if (car.getControllingPassenger() != null) {
            player.displayClientMessage(Component.literal("§cAsk the driver to step out first."), true);
            return;
        }
        if ((car.build() & CarPart.WHEELS) != CarPart.WHEELS) {
            player.displayClientMessage(Component.literal("§cIt needs all four wheels on to align it."), true);
            return;
        }
        carUuid = car.getUUID();
        carId = car.getId();
        operator = player.getUUID();
        compFrom = car.position();
        setPhase(MOUNT);
        player.displayClientMessage(Component.literal("§bHawkSight: §fmounting targets..."), true);
    }

    private void setPhase(int p) {
        phase = p;
        phaseStart = level.getGameTime();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void reset() {
        carUuid = null;
        carId = -1;
        operator = null;
        beam = 1.6f;
        setPhase(IDLE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlignerBlockEntity be) {
        if (be.phase == IDLE || be.phase == LIVE && level.getGameTime() % 20 != 0) return;
        CarEntity car = be.car();
        if (car == null) {
            be.reset();
            return;
        }
        long t = level.getGameTime() - be.phaseStart;
        switch (be.phase) {
            case MOUNT -> {
                if (t % 15 == 1) level.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7f, 1.5f);
                if (t >= LENGTH[MOUNT]) {
                    be.compFrom = car.position();
                    be.setPhase(COMP);
                    be.tell("§bHawkSight: §frolling compensation: rolling the vehicle back and forward...");
                }
            }
            case COMP -> {
                // half a block back and forward again, smoothly
                double k = Math.sin(Math.PI * t / LENGTH[COMP]) * 0.5;
                Vec3 f = car.forwardVec();
                car.suspend();
                car.setPos(be.compFrom.x - f.x * k, be.compFrom.y, be.compFrom.z - f.z * k);
                if (t >= LENGTH[COMP]) {
                    car.setPos(be.compFrom.x, be.compFrom.y, be.compFrom.z);
                    be.setPhase(MEASURE);
                }
            }
            case MEASURE -> {
                // ride the beam up (or down) to the targets
                float want = (float) (car.getY() + 0.375 - pos.getY());
                be.beam += (want - be.beam) * 0.15f;
                if (t % 10 == 0) level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.8f);
                if (t >= LENGTH[MEASURE]) {
                    be.beam = want;
                    be.setPhase(LIVE);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.5f);
                    if (be.operator != null && level.getPlayerByUUID(be.operator) instanceof ServerPlayer sp) be.openScreen(sp, car);
                }
            }
            case LIVE -> {
                if (car.getControllingPassenger() != null) {
                    be.tell("§cSomeone drove off with the targets on. Measurement cancelled.");
                    be.reset();
                }
            }
            case REMOVE -> {
                float want = 1.6f;
                be.beam += (want - be.beam) * 0.1f;
                if (t % 15 == 1) level.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.6f, 1.5f);
                if (t >= LENGTH[REMOVE]) be.reset();
            }
            default -> {}
        }
    }

    private void tell(String text) {
        if (operator != null && level.getPlayerByUUID(operator) instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal(text), true);
    }

    private void openScreen(ServerPlayer p, CarEntity car) {
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", worldPosition.asLong());
        t.putInt("Car", car.getId());
        dev.freerun.life.LifePayloads.open(p, dev.freerun.life.LifePayloads.ALIGN, t);
    }

    /** From the screen: nudge one reading, or finish. */
    public static void action(ServerPlayer p, CompoundTag data) {
        BlockPos pos = BlockPos.of(data.getLong("Pos"));
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 16 * 16) return;
        if (!(p.level().getBlockEntity(pos) instanceof AlignerBlockEntity be) || be.phase != LIVE) return;
        CarEntity car = be.car();
        if (car == null) return;
        if (data.getBoolean("Finish")) {
            boolean good = car.alignment().inSpec();
            be.setPhase(REMOVE);
            p.displayClientMessage(Component.literal(good ? "§aAlignment in spec. §7Drives straight as an arrow." : "§eSaved as-is. §7It's still out of spec, so expect it to pull."), false);
            if (good) dev.freerun.career.Careers.addXp(p, 25, "wheel alignment");
            return;
        }
        boolean hasWrench = p.getInventory().contains(new net.minecraft.world.item.ItemStack(GarageRegistry.WRENCH)) || p.getAbilities().instabuild;
        if (!hasWrench) {
            p.displayClientMessage(Component.literal("§cYou need a torque wrench to turn the adjusters."), true);
            return;
        }
        Alignment a = car.alignment();
        Alignment n = Alignment.load(a.save());
        int wheel = Math.max(0, Math.min(3, data.getInt("Wheel")));
        double d = Math.max(-1, Math.min(1, data.getDouble("Delta")));
        switch (data.getString("What")) {
            case "camber" -> n.camber[wheel] += d * 0.1;
            case "caster" -> {
                if (wheel < 2) n.caster[wheel] += d * 0.1;
            }
            case "toe" -> n.toe[wheel] += d * 0.02;
            default -> {}
        }
        n.round();
        car.setAlignment(n);
        p.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
    }

    // ------------------------------------------------------------------ save / sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Phase", phase);
        tag.putLong("Start", phaseStart);
        tag.putInt("CarId", carId);
        tag.putFloat("Beam", beam);
        if (carUuid != null) tag.putUUID("CarUuid", carUuid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = tag.getInt("Phase");
        phaseStart = tag.getLong("Start");
        carId = tag.getInt("CarId");
        beam = tag.contains("Beam") ? tag.getFloat("Beam") : 1.6f;
        carUuid = tag.hasUUID("CarUuid") ? tag.getUUID("CarUuid") : null;
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
