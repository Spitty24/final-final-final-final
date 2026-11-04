package dev.freerun.economy;

import com.mojang.serialization.MapCodec;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** The fixtures you find in a gas station: ATM, vending machine, slushie machine, roller grill, gas pump, TV. */
public final class StoreBlocks {
    private StoreBlocks() {}

    // ------------------------------------------------------------------ ATM

    /** Deposit your bills (you get a free wallet the first time), check your balance, or sneak to withdraw $20. */
    public static class Atm extends AmenityBlock {
        public static final MapCodec<Atm> CODEC = simpleCodec(Atm::new);

        public Atm(Properties p) {
            super(p, Block.box(2, 0, 3, 14, 16, 13));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            ItemStack wallet = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem() instanceof WalletItem) {
                    wallet = player.getInventory().getItem(i);
                    break;
                }
            }
            if (wallet.isEmpty()) {
                wallet = new ItemStack(EconomyRegistry.WALLET);
                give(player, wallet);
                // give() may have dropped it on the floor; look it up again in the inventory.
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).getItem() instanceof WalletItem) wallet = player.getInventory().getItem(i);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§b[Bayou Federal Credit Union] §fWelcome, new member! Here's a free wallet."), false);
            }
            if (player.isShiftKeyDown()) {
                int bal = Money.walletBalance(wallet);
                int take = Math.min(20, bal);
                if (take > 0) {
                    Money.setWalletBalance(wallet, bal - take);
                    for (ItemStack bill : Money.bills(take)) give(player, bill);
                    say(player, "§aWithdrew " + Money.fmt(take) + ". §7Balance: " + Money.fmt(bal - take));
                } else {
                    say(player, "§cInsufficient funds.");
                }
            } else {
                int dep = Money.depositAll(player, wallet);
                say(player, (dep > 0 ? "§aDeposited " + Money.fmt(dep) + ". " : "") + "§7Balance: §a"
                        + Money.fmt(Money.walletBalance(wallet)) + " §8(sneak-click to withdraw $20)");
            }
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6f, 1.8f);
            return InteractionResult.CONSUME;
        }
    }

    // ------------------------------------------------------------------ vending machine

    /**
     * $2 for a random snack or drink. Every so often it gets stuck; give the machine a whack (punch
     * it) and it drops.
     */
    public static class VendingMachine extends AmenityBlock {
        public static final MapCodec<VendingMachine> CODEC = simpleCodec(VendingMachine::new);
        public static final BooleanProperty STUCK = BooleanProperty.create("stuck");

        public VendingMachine(Properties p) {
            super(p, Block.box(1, 0, 2, 15, 16, 14));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected BlockState defaultState(BlockState state) {
            return state.setValue(STUCK, false);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STUCK);
        }

        private static ItemStack randomItem(RandomSource r) {
            List<Item> pool = EconomyRegistry.VENDING;
            return new ItemStack(pool.get(r.nextInt(pool.size())));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (state.getValue(STUCK)) {
                say(player, "§eIt's stuck hanging on the coil. Give the machine a whack.");
                return InteractionResult.CONSUME;
            }
            if (!charge(player, 2)) return InteractionResult.CONSUME;
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0f, 0.8f);
            if (level.random.nextInt(8) == 0) {
                level.setBlock(pos, state.setValue(STUCK, true), Block.UPDATE_ALL);
                say(player, "§e...and it gets stuck. Of course it does.");
            } else {
                Block.popResourceFromFace(level, pos, state.getValue(FACING), randomItem(level.random));
            }
            return InteractionResult.CONSUME;
        }

        @Override
        protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
            if (level.isClientSide || !state.getValue(STUCK)) return;
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 1.4f);
            if (level.random.nextInt(3) != 0) {
                level.setBlock(pos, state.setValue(STUCK, false), Block.UPDATE_ALL);
                Block.popResourceFromFace(level, pos, state.getValue(FACING), randomItem(level.random));
                say(player, "§a*THUNK* It dropped!");
            } else {
                say(player, "§7It wobbles. Hit it again.");
            }
        }
    }

    // ------------------------------------------------------------------ slushie machine

    public static class SlushieMachine extends AmenityBlock {
        public static final MapCodec<SlushieMachine> CODEC = simpleCodec(SlushieMachine::new);

        public SlushieMachine(Properties p) {
            super(p, Block.box(1, 0, 3, 15, 16, 13));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!charge(player, 2)) return InteractionResult.CONSUME;
            // Left tank is blue raspberry, right tank is cherry.
            Direction facing = state.getValue(FACING);
            double side = (hit.getLocation().x - pos.getX() - 0.5) * facing.getClockWise().getStepX()
                    + (hit.getLocation().z - pos.getZ() - 0.5) * facing.getClockWise().getStepZ();
            Item flavour = side > 0 ? EconomyRegistry.CHERRY_SLUSHIE : EconomyRegistry.BLUE_SLUSHIE;
            give(player, new ItemStack(flavour));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7f, 1.5f);
            say(player, "§b*glorp*");
            return InteractionResult.CONSUME;
        }
    }

    // ------------------------------------------------------------------ roller grill

    /** Put raw pork, beef or chicken on the rollers and it comes off as a roller-grill hot dog. */
    public static class RollerGrill extends AmenityBlock {
        public static final MapCodec<RollerGrill> CODEC = simpleCodec(RollerGrill::new);
        public static final BooleanProperty COOKING = BooleanProperty.create("cooking");

        public RollerGrill(Properties p) {
            super(p, Block.box(1, 0, 3, 15, 7, 13));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected BlockState defaultState(BlockState state) {
            return state.setValue(COOKING, false);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, COOKING);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                  InteractionHand hand, BlockHitResult hit) {
            boolean meat = stack.is(Items.PORKCHOP) || stack.is(Items.BEEF) || stack.is(Items.CHICKEN);
            if (!meat) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (state.getValue(COOKING)) return ItemInteractionResult.CONSUME;
            if (level instanceof ServerLevel server) {
                stack.consume(1, player);
                server.setBlock(pos, state.setValue(COOKING, true), Block.UPDATE_ALL);
                server.scheduleTick(pos, this, 120);
                server.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.2f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(COOKING)) return;
            level.setBlock(pos, state.setValue(COOKING, false), Block.UPDATE_ALL);
            Block.popResourceFromFace(level, pos, state.getValue(FACING), new ItemStack(EconomyRegistry.ROLLER_HOT_DOG, 2));
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(COOKING) && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
            }
        }
    }

    // ------------------------------------------------------------------ gas pump

    /**
     * Fills the nearest car within 6 blocks ($1 per 10%), or an empty gas can you're holding ($6).
     */
    public static class GasPump extends AmenityBlock {
        public static final MapCodec<GasPump> CODEC = simpleCodec(GasPump::new);

        public GasPump(Properties p) {
            super(p, Block.box(3, 0, 4, 13, 24, 12));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                  InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(EconomyRegistry.EMPTY_GAS_CAN)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide && charge(player, 6)) {
                stack.consume(1, player);
                give(player, new ItemStack(EconomyRegistry.GAS_CAN));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 0.7f);
                say(player, "§aFilled the gas can. §7($6)");
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            CarEntity car = level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(6.0)).stream()
                    .min(java.util.Comparator.comparingDouble(c -> c.distanceToSqr(pos.getCenter()))).orElse(null);
            if (car == null) {
                say(player, "§7Pull a car up within 6 blocks of the pump. §8(Or bring an empty gas can.)");
                return InteractionResult.CONSUME;
            }
            float missing = 100.0f - car.fuel();
            if (missing < 1.0f) {
                say(player, "§7That tank's already full.");
                return InteractionResult.CONSUME;
            }
            int tenths = (int) Math.ceil(missing / 10.0f);
            int affordable = Math.min(tenths, Money.balance(player) + (player.getAbilities().instabuild ? 99 : 0));
            if (affordable <= 0) {
                say(player, "§cGas is $1 per 10%. You're broke.");
                return InteractionResult.CONSUME;
            }
            Money.pay(player, affordable);
            car.addFuel(affordable * 10.0f);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.playSound(null, pos, ModSounds.CASH_REGISTER, SoundSource.BLOCKS, 0.6f, 1.0f);
            say(player, "§aPumped " + Math.min(100, Math.round(car.fuel())) + "% §7(" + Money.fmt(affordable) + ")");
            return InteractionResult.CONSUME;
        }
    }

    // ------------------------------------------------------------------ TV

    public static class Tv extends AmenityBlock {
        public static final MapCodec<Tv> CODEC = simpleCodec(Tv::new);
        public static final BooleanProperty ON = BooleanProperty.create("on");
        private static final String[] SHOWS = {
                "§7📺 *local news* \"...and the humidity today will be: yes.\"",
                "§7📺 *a fishing show* \"Look at the size of that bass, cher!\"",
                "§7📺 *a cooking show* \"First, you make a roux...\"",
                "§7📺 *football* \"AND HE'S GOT NOTHING BUT TURF IN FRONT OF HIM!\"",
                "§7📺 *a mattress commercial* \"PRICES SO LOW WE MUST BE CRAZY!\"",
                "§7📺 *a weather map that is entirely red*",
                "§7📺 *a lawyer ad* \"Hurt on the job? Call 1-800-HURT-NOW.\""
        };

        public Tv(Properties p) {
            super(p, Block.box(0, 0, 6, 16, 12, 10));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected BlockState defaultState(BlockState state) {
            return state.setValue(ON, false);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ON);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            boolean on = !state.getValue(ON);
            level.setBlock(pos, state.setValue(ON, on), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6f, on ? 1.4f : 0.9f);
            if (on) player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    SHOWS[level.random.nextInt(SHOWS.length)]), false);
            return InteractionResult.CONSUME;
        }
    }
}
