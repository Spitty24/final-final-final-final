package dev.freerun.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A free-moving piece of steel, or a rigid assembly of several welded together.
 *
 * Physics: gravity, bouncing, sliding friction, and solid collision with blocks and with other
 * parts (so parts stack and you can stand on them). Parts stay upright and rotate in 90 degree
 * steps; there is no tumbling.
 *
 * The parts list is stored relative to the entity's position (centre of the bottom of the
 * assembly) and synced to clients so they can render and collide with the exact shape bounds.
 *
 * Interactions:
 *   empty hand            rotate the whole assembly 90 degrees
 *   sneak + empty hand    pick it up (you get every part back as items)
 *   punch                 shove it
 *   welding torch         select / weld (see {@link WeldingTorchItem})
 *   part item             place another part on top, snapped to this assembly's grid
 *   gravity gun           handled by the gun (see {@link GravityGunManager})
 */
public class SteelPartEntity extends Entity {
    public static final int MAX_PARTS = 32;

    /** One piece of the assembly: its shape, rotation (0..3, quarter turns) and offset from the origin. */
    public record Part(PartType type, int rot, double dx, double dy, double dz) {
        public AABB localBox() {
            double w = type.w;
            double d = type.d;
            if ((rot & 1) == 1) {
                double t = w;
                w = d;
                d = t;
            }
            return new AABB(dx - w / 2, dy, dz - d / 2, dx + w / 2, dy + type.h, dz + d / 2);
        }
    }

    private static final EntityDataAccessor<CompoundTag> DATA_PARTS =
            SynchedEntityData.defineId(SteelPartEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private List<Part> partsCache;
    /** Set each tick by the gravity gun while it holds us; we skip gravity while it's above 0. */
    private int heldTicks;

    public SteelPartEntity(EntityType<? extends SteelPartEntity> type, Level level) {
        super(type, level);
    }

    /** A brand new single part at the given position. Not yet added to the level. */
    public static SteelPartEntity single(Level level, PartType type, double x, double y, double z, int rot) {
        SteelPartEntity e = new SteelPartEntity(FactoryRegistry.STEEL_PART, level);
        e.setParts(List.of(new Part(type, rot & 3, 0, 0, 0)));
        e.moveTo(x, y, z, 0.0f, 0.0f);
        return e;
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PARTS, new CompoundTag());
    }

    public List<Part> getParts() {
        if (partsCache == null) partsCache = decode(entityData.get(DATA_PARTS));
        return partsCache;
    }

    public int partCount() {
        return getParts().size();
    }

    public void setParts(List<Part> parts) {
        entityData.set(DATA_PARTS, encode(parts));
        partsCache = null;
        refreshShape();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PARTS.equals(key)) {
            partsCache = null;
            refreshShape();
        }
    }

    /** Recomputes our size and box. Physics is paused so vanilla doesn't nudge the new box around. */
    private void refreshShape() {
        boolean np = noPhysics;
        noPhysics = true;
        refreshDimensions();
        noPhysics = np;
    }

    private static CompoundTag encode(List<Part> parts) {
        ListTag list = new ListTag();
        for (Part p : parts) {
            CompoundTag t = new CompoundTag();
            t.putInt("t", p.type().ordinal());
            t.putInt("r", p.rot() & 3);
            t.putDouble("x", p.dx());
            t.putDouble("y", p.dy());
            t.putDouble("z", p.dz());
            list.add(t);
        }
        CompoundTag root = new CompoundTag();
        root.put("L", list);
        return root;
    }

    private static List<Part> decode(CompoundTag root) {
        List<Part> out = new ArrayList<>();
        if (root != null && root.contains("L", Tag.TAG_LIST)) {
            ListTag list = root.getList("L", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < MAX_PARTS; i++) {
                CompoundTag t = list.getCompound(i);
                out.add(new Part(PartType.byId(t.getInt("t")), t.getInt("r") & 3,
                        t.getDouble("x"), t.getDouble("y"), t.getDouble("z")));
            }
        }
        // A part summoned with no data is a plain sheet.
        if (out.isEmpty()) out.add(new Part(PartType.SHEET, 0, 0, 0, 0));
        return List.copyOf(out);
    }

    // ------------------------------------------------------------------ shape

    /** Union of all part boxes, relative to our position. */
    public static AABB localBounds(List<Part> parts) {
        AABB box = null;
        for (Part p : parts) {
            AABB b = p.localBox();
            box = box == null ? b : new AABB(
                    Math.min(box.minX, b.minX), Math.min(box.minY, b.minY), Math.min(box.minZ, b.minZ),
                    Math.max(box.maxX, b.maxX), Math.max(box.maxY, b.maxY), Math.max(box.maxZ, b.maxZ));
        }
        return box;
    }

    @Override
    protected AABB makeBoundingBox() {
        if (entityData == null) return super.makeBoundingBox();
        AABB local = localBounds(getParts());
        if (local == null) return super.makeBoundingBox();
        return local.move(getX(), getY(), getZ());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (entityData == null) return super.getDimensions(pose);
        AABB b = localBounds(getParts());
        if (b == null) return super.getDimensions(pose);
        float w = (float) Math.max(b.getXsize(), b.getZsize());
        float h = (float) Math.max(0.0625, b.maxY);
        return EntityDimensions.fixed(w, h);
    }

    // ------------------------------------------------------------------ physics

    /** Called by the gravity gun every tick it holds this part. */
    public void hold() {
        heldTicks = 2;
    }

    public boolean isHeld() {
        return heldTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;   // the server simulates; clients get positions each tick

        boolean held = heldTicks > 0;
        if (held) heldTicks--;

        Vec3 v = getDeltaMovement();
        // Resting parts only re-check every few ticks (like dropped items) to save work.
        boolean resting = onGround() && !held && v.horizontalDistanceSqr() < 1.0E-5 && Math.abs(v.y) < 0.08;
        if (resting && (tickCount + getId()) % 4 != 0) return;

        if (!held && !isNoGravity()) v = v.add(0.0, -0.06, 0.0);
        double preVy = v.y;
        setDeltaMovement(v);
        move(MoverType.SELF, v);

        Vec3 nv = getDeltaMovement();
        if (verticalCollision && preVy < -0.25) {
            // Clang: a small bounce and a metallic hit, louder for harder landings.
            nv = new Vec3(nv.x, -preVy * 0.25, nv.z);
            float vol = (float) Math.min(0.6, 0.1 + -preVy * 0.35);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS,
                    vol, 1.6f + random.nextFloat() * 0.4f);
        }
        if (onGround()) {
            nv = nv.multiply(0.6, 1.0, 0.6);
        } else {
            nv = nv.scale(held ? 0.9 : 0.98);
        }
        if (nv.horizontalDistanceSqr() < 1.0E-6 && onGround()) nv = new Vec3(0.0, nv.y, 0.0);
        setDeltaMovement(nv);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;   // solid: stack them, stand on them
    }

    @Override
    public boolean isPushable() {
        return false;  // move them with your fists or the gravity gun instead
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return false;   // the blast knocks it around anyway
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            Vec3 look = player.getLookAngle();
            double mass = Math.sqrt(partCount());
            setDeltaMovement(getDeltaMovement().add(look.x * 0.45 / mass, 0.18 / mass, look.z * 0.45 / mass));
            hurtMarked = true;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS,
                    0.25f, 1.9f);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof GravityGunItem) return InteractionResult.PASS;   // the gun does its own thing
        if (stack.getItem() instanceof WeldingTorchItem) {
            if (!level().isClientSide) WeldingTorchItem.useOnPart(player, this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (hand != InteractionHand.MAIN_HAND || !stack.isEmpty()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            pickUp(player);
        } else {
            rotate(player);
        }
        return InteractionResult.CONSUME;
    }

    /** Clicking an assembly with a part item stacks a new part on top of it at the clicked spot. */
    @Override
    public InteractionResult interactAt(Player player, Vec3 hit, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof PartItem partItem)) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        double x = getX() + Math.round(hit.x * 2.0) / 2.0;    // snap to this assembly's half-block grid
        double z = getZ() + Math.round(hit.z * 2.0) / 2.0;
        double y = getBoundingBox().maxY;
        SteelPartEntity part = single(level(), partItem.type(), x, y, z, PartItem.rotationFor(player));
        if (!level().noCollision(part, part.getBoundingBox().deflate(1.0E-3))) {
            player.displayClientMessage(Component.literal("§cNo room for that part there."), true);
            return InteractionResult.CONSUME;
        }
        level().addFreshEntity(part);
        stack.consume(1, player);
        level().playSound(null, x, y, z, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        return InteractionResult.CONSUME;
    }

    private void rotate(Player player) {
        List<Part> rotated = new ArrayList<>();
        for (Part p : getParts()) {
            // A quarter turn about our origin: +X goes to +Z.
            rotated.add(new Part(p.type(), (p.rot() + 1) & 3, -p.dz(), p.dy(), p.dx()));
        }
        AABB box = localBounds(rotated).move(getX(), getY(), getZ());
        if (!level().noCollision(this, box.deflate(1.0E-3))) {
            player.displayClientMessage(Component.literal("§cNo room to rotate it here."), true);
            return;
        }
        setParts(rotated);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 1.0f, 0.9f);
    }

    private void pickUp(Player player) {
        for (Part p : getParts()) {
            ItemStack item = new ItemStack(p.type().item());
            if (!player.getInventory().add(item)) player.drop(item, false);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.2f);
        discard();
    }

    // ------------------------------------------------------------------ welding

    /** Fuses {@code other} into this assembly where they currently sit, then removes it. */
    public void weldWith(SteelPartEntity other) {
        record World(PartType type, int rot, double x, double y, double z) {}
        List<World> all = new ArrayList<>();
        for (Part p : getParts()) {
            all.add(new World(p.type(), p.rot(), getX() + p.dx(), getY() + p.dy(), getZ() + p.dz()));
        }
        for (Part p : other.getParts()) {
            all.add(new World(p.type(), p.rot(), other.getX() + p.dx(), other.getY() + p.dy(), other.getZ() + p.dz()));
        }
        // New origin: centre of the bottom of the combined bounds.
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (World w : all) {
            AABB b = new Part(w.type(), w.rot(), w.x(), w.y(), w.z()).localBox();
            minX = Math.min(minX, b.minX);
            minY = Math.min(minY, b.minY);
            minZ = Math.min(minZ, b.minZ);
            maxX = Math.max(maxX, b.maxX);
            maxZ = Math.max(maxZ, b.maxZ);
        }
        double ox = (minX + maxX) / 2.0;
        double oz = (minZ + maxZ) / 2.0;
        List<Part> parts = new ArrayList<>();
        for (World w : all) {
            parts.add(new Part(w.type(), w.rot(), w.x() - ox, w.y() - minY, w.z() - oz));
        }
        other.discard();
        setPosRaw(ox, minY, oz);
        setParts(parts);
        setDeltaMovement(Vec3.ZERO);
    }

    /** Cuts every weld: each part becomes its own entity again, right where it was. */
    public void split() {
        List<Part> parts = getParts();
        if (parts.size() <= 1) return;
        for (int i = 1; i < parts.size(); i++) {
            Part p = parts.get(i);
            SteelPartEntity e = single(level(), p.type(), getX() + p.dx(), getY() + p.dy(), getZ() + p.dz(), p.rot());
            level().addFreshEntity(e);
        }
        Part first = parts.get(0);
        setPosRaw(getX() + first.dx(), getY() + first.dy(), getZ() + first.dz());
        setParts(List.of(new Part(first.type(), first.rot(), 0, 0, 0)));
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Parts", Tag.TAG_COMPOUND)) {
            entityData.set(DATA_PARTS, tag.getCompound("Parts"));
            partsCache = null;
            refreshShape();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Parts", entityData.get(DATA_PARTS).copy());
    }
}
