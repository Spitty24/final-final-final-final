package dev.freerun.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

/**
 * A 3-wide by 4-tall crafting menu — the vanilla crafting table plus one extra row of 3 slots on
 * top (12 input slots instead of 9). All standard 3x3-or-smaller recipes still work (they match
 * anywhere they fit in the taller grid); the extra row is just more room.
 *
 * Slot indices: 0 = result, 1..12 = craft grid, 13..48 = player inventory (27 main + 9 hotbar).
 */
public class BigCraftingMenu extends AbstractContainerMenu {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 4;
    private static final int GRID = WIDTH * HEIGHT;          // 12
    private static final int INV_START = 1 + GRID;           // 13
    private static final int INV_END = INV_START + 36;       // 49

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, WIDTH, HEIGHT);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final Level level;

    // Client-side constructor used by the MenuType.
    public BigCraftingMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public BigCraftingMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(MenuRegistry.BIG_CRAFTING, id);
        this.access = access;
        this.player = inv.player;
        this.level = inv.player.level();

        this.addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 124, 45));

        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                this.addSlot(new Slot(craftSlots, col + row * WIDTH, 30 + col * 18, 18 + row * 18));
            }
        }
        // player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 160));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return;
        ItemStack result = ItemStack.EMPTY;
        CraftingInput input = craftSlots.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> opt =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (opt.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = opt.get();
            CraftingRecipe recipe = holder.value();
            if (resultSlots.setRecipeUsed(level, sp, holder)) {
                ItemStack assembled = recipe.assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == 0) {                                   // result -> inventory
                access.execute((lvl, pos) -> stack.getItem().onCraftedBy(stack, lvl, player));
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, moved);
            } else if (index >= INV_START && index < INV_END) { // inventory -> grid
                if (!this.moveItemStackTo(stack, 1, INV_START, false)) return ItemStack.EMPTY;
            } else {                                            // grid -> inventory
                if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            if (index == 0) player.drop(stack, false);
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((lvl, pos) -> this.clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.CRAFTING_TABLE);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}
