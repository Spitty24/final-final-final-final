package dev.freerun.life;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One class for all the furniture and fixtures (office, home, bar, arcade): a facing, an on/off
 * state for the ones that light up, a shape, and what happens when you right-click it.
 */
public class FurnitureBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public enum Use {
        NONE, SEAT, STORAGE, LAMP, STOVE, SINK, TOILET, PIANO, CLOCK, WATER_COOLER, PRINTER, SERVER, WHITEBOARD, PHONE,
        JOB_BOARD, MAILBOX, CLAW, DARTS, POOL, TAP, PLANT, DESK
    }

    private static final java.util.Map<java.util.UUID, Long> COOLER_USE = new java.util.HashMap<>();
    private final Use use;
    private final VoxelShape north;
    private final boolean solid;

    public FurnitureBlock(Properties properties, Use use, VoxelShape north, boolean solid) {
        super(properties);
        this.use = use;
        this.north = north;
        this.solid = solid;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public Use use() {
        return use;
    }

    public boolean seat() {
        return use == Use.SEAT || use == Use.TOILET;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new FurnitureBlock(p, use, north, solid));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** Rotates a shape drawn facing north to the block's facing. */
    static VoxelShape rotate(VoxelShape shape, Direction to) {
        if (to == Direction.NORTH) return shape;
        VoxelShape[] out = {Shapes.empty()};
        int turns = switch (to) {
            case EAST -> 1;
            case SOUTH -> 2;
            default -> 3;
        };
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
            for (int i = 0; i < turns; i++) {   // (x, z) -> (1 - z, x), clockwise seen from above
                double nx0 = 1 - az1, nz0 = ax0, nx1 = 1 - az0, nz1 = ax1;
                ax0 = nx0;
                az0 = nz0;
                ax1 = nx1;
                az1 = nz1;
            }
            out[0] = Shapes.or(out[0], Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
        return out[0];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return rotate(north, state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return solid ? getShape(state, level, pos, ctx) : Shapes.empty();
    }

    /** Furniture with storage (fridge, wardrobe, nightstand, filing cabinet). */
    public static class Storage extends FurnitureBlock implements EntityBlock {
        public Storage(Properties properties, VoxelShape north) {
            super(properties, Use.STORAGE, north, true);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FurnitureStorageBlockEntity(pos, state);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (use == Use.STORAGE) Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (use == Use.STOVE && !stack.isEmpty()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            SingleRecipeInput in = new SingleRecipeInput(stack);
            ItemStack out = level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, in, level)
                    .map(r -> r.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
            if (out.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                out = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, in, level)
                        .map(r -> r.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
            }
            if (out.isEmpty()) {
                player.displayClientMessage(Component.literal("§7You can't cook that on the stove."), true);
                return ItemInteractionResult.CONSUME;
            }
            stack.consume(1, player);
            if (!player.getInventory().add(out)) player.drop(out, false);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
            player.displayClientMessage(Component.literal("§6Sizzle. §7" + out.getHoverName().getString() + ", done."), true);
            return ItemInteractionResult.CONSUME;
        }
        if (use == Use.SINK && (stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET))) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            ItemStack out = stack.is(Items.BUCKET) ? new ItemStack(Items.WATER_BUCKET) : PotionContents.createItemStack(Items.POTION, Potions.WATER);
            stack.consume(1, player);
            if (!player.getInventory().add(out)) player.drop(out, false);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
            return ItemInteractionResult.CONSUME;
        }
        if (use == Use.PLANT && stack.is(Items.WATER_BUCKET) || use == Use.PLANT && stack.is(Items.POTION)) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("§aThe plant looks happier."), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return use == Use.NONE || use == Use.DESK ? InteractionResult.PASS : InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        RandomSource r = level.getRandom();
        switch (use) {
            case SEAT, TOILET -> {
                if (player.isPassenger() || player.isShiftKeyDown()) return InteractionResult.PASS;
                if (!level.getEntitiesOfClass(dev.freerun.world.SeatEntity.class, new AABB(pos)).isEmpty()) return InteractionResult.PASS;
                dev.freerun.world.SeatEntity seat = dev.freerun.world.SeatEntity.create(level, pos);
                level.addFreshEntity(seat);
                player.startRiding(seat);
                if (use == Use.TOILET) {
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.9f, 0.6f);
                    sp.displayClientMessage(Component.literal("§7*flush*"), true);
                }
            }
            case STORAGE -> {
                if (level.getBlockEntity(pos) instanceof FurnitureStorageBlockEntity be) {
                    player.openMenu(be);
                    level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5f, 1.2f);
                }
            }
            case LAMP -> {
                level.setBlock(pos, state.cycle(LIT), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, state.getValue(LIT) ? 0.6f : 0.9f);
            }
            case STOVE -> {
                level.setBlock(pos, state.cycle(LIT), Block.UPDATE_ALL);
                sp.displayClientMessage(Component.literal("§7Burner " + (state.getValue(LIT) ? "off" : "on") + ". Right-click with raw food to cook it."), true);
            }
            case SINK -> {
                level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.6f);
                sp.displayClientMessage(Component.literal("§7You wash your hands. Use a bottle or bucket to fill it."), true);
            }
            case PIANO -> {
                // where you click along the keys picks the note
                Direction f = state.getValue(FACING);
                double along = f.getAxis() == Direction.Axis.Z ? hit.getLocation().x - pos.getX() : hit.getLocation().z - pos.getZ();
                if (f == Direction.SOUTH || f == Direction.WEST) along = 1 - along;
                int note = net.minecraft.util.Mth.clamp((int) (along * 24), 0, 24);
                float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.RECORDS, 1.0f, pitch);
                ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
            }
            case CLOCK -> {
                long t = (level.getDayTime() + 6000) % 24000;
                int h = (int) (t / 1000), m = (int) (t % 1000 * 60 / 1000);
                sp.displayClientMessage(Component.literal(String.format("§fThe clock says §e%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM")), true);
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 0.7f);
            }
            case WATER_COOLER -> {
                long now = level.getGameTime();
                if (now - COOLER_USE.getOrDefault(sp.getUUID(), -10000L) < 600) {
                    sp.displayClientMessage(Component.literal("§7*glug* You've had plenty for now."), true);
                } else {
                    ItemStack water = new ItemStack(dev.freerun.economy.EconomyRegistry.BOTTLED_WATER);
                    if (!player.getInventory().add(water)) player.drop(water, false);
                    COOLER_USE.put(sp.getUUID(), now);
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.2f);
                    sp.displayClientMessage(Component.literal("§b*glug glug* §7A cup of cold water."), true);
                }
                OfficeJobs.gossip(sp, pos);
            }
            case PRINTER -> {
                if (!OfficeJobs.fixPrinter(sp, pos)) {
                    ItemStack paper = new ItemStack(Items.PAPER);
                    if (!player.getInventory().add(paper)) player.drop(paper, false);
                    level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.8f, 0.8f);
                    sp.displayClientMessage(Component.literal("§7*whirr* The printer spits out a page. Still warm."), true);
                }
            }
            case SERVER -> {
                if (!OfficeJobs.fixServer(sp, pos)) {
                    sp.displayClientMessage(Component.literal("§a● §7All systems nominal. Lots of blinking lights."), true);
                }
            }
            case WHITEBOARD -> {
                String[] notes = {"Q3 GOALS: synergy??", "DO NOT ERASE", "Lunch order: 4 po'boys, 1 salad (Gary)",
                        "Sprint retro: what went well? (nothing)", "Days since last printer fire: 3", "Brainstorm: ideas", "Wi-Fi password: password1"};
                sp.displayClientMessage(Component.literal("§fThe whiteboard says: §o" + notes[r.nextInt(notes.length)]), true);
            }
            case PHONE -> OfficeJobs.answerPhone(sp, pos);
            case JOB_BOARD -> OfficeJobs.openBoard(sp, pos);
            case MAILBOX -> Rentals.mailbox(sp, pos);
            case CLAW -> Arcade.claw(sp, pos);
            case DARTS -> Arcade.open(sp, pos, Arcade.DARTS);
            case POOL -> {
                String[] shots = {"You sink the 3 in the corner pocket. Smooth.", "You scratch. The cue ball rolls sadly into a pocket.",
                        "Bank shot off two rails... in! The bar cheers.", "You miss everything. Even the table, somehow.",
                        "Called the 8 ball, sank the 8 ball. You win the table."};
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.9f, 1.6f);
                sp.displayClientMessage(Component.literal("§a🎱 §f" + shots[r.nextInt(shots.length)]), true);
            }
            case TAP -> sp.displayClientMessage(Component.literal("§7The bartender handles the taps. Talk to them to order."), true);
            case PLANT -> sp.displayClientMessage(Component.literal("§aA very healthy plant. It's probably fake."), true);
            default -> {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (use == Use.SERVER && random.nextInt(3) == 0) {
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(
                            random.nextBoolean() ? new org.joml.Vector3f(0.2f, 1f, 0.3f) : new org.joml.Vector3f(1f, 0.6f, 0.1f), 0.5f),
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.2 + random.nextDouble() * 1.4, pos.getZ() + 0.5, 0, 0, 0);
        }
        if ((use == Use.LAMP || use == Use.STOVE) && state.getValue(LIT) && use == Use.STOVE && random.nextInt(4) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
