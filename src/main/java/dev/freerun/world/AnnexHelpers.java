package dev.freerun.world;

import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.SoldierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

/** Shared bits for the factory annexes: filling containers, writing signs, placing staff. */
final class AnnexHelpers {
    private AnnexHelpers() {}

    static void fill(WorldGenLevel level, BlockPos pos, ItemStack... stacks) {
        if (level.getBlockEntity(pos) instanceof Container c) {
            for (int i = 0; i < stacks.length && i < c.getContainerSize(); i++) c.setItem(i, stacks[i]);
        }
    }

    /**
     * Writes up to four lines on a sign. During world generation the sign isn't attached to a level
     * yet (so it can't send updates), so we load the text as data instead.
     */
    static void signText(WorldGenLevel level, BlockPos pos, String... lines) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        if (sign.getLevel() != null) {
            SignText text = new SignText();
            for (int i = 0; i < 4; i++) text = text.setMessage(i, Component.literal(i < lines.length ? lines[i] : ""));
            sign.setText(text, true);
            return;
        }
        ListTag messages = new ListTag();
        for (int i = 0; i < 4; i++) {
            messages.add(StringTag.valueOf(Serializer.toJson(Component.literal(i < lines.length ? lines[i] : ""),
                    level.registryAccess())));
        }
        CompoundTag front = new CompoundTag();
        front.put("messages", messages);
        front.putString("color", "black");
        front.putBoolean("has_glowing_text", false);
        CompoundTag tag = new CompoundTag();
        tag.put("front_text", front);
        sign.loadWithComponents(tag, level.registryAccess());
    }

    /** A worker sitting in the chair at {@code pos}, holding something. */
    static void seatedWorker(WorldGenLevel level, BlockPos pos, Direction facing, RoadhouseNpcEntity.Role role, ItemStack held) {
        ServerLevel server = level.getLevel();
        SeatEntity seat = SeatEntity.create(server, pos);
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, server);
        npc.setRole(role, 0);
        npc.setItemSlot(EquipmentSlot.MAINHAND, held);
        npc.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        float yaw = facing.toYRot();
        npc.moveTo(seat.getX(), seat.getY(), seat.getZ(), yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.startRiding(seat, true);
        level.addFreshEntityWithPassengers(seat);
    }

    static void standingWorker(WorldGenLevel level, BlockPos pos, Direction facing, RoadhouseNpcEntity.Role role, ItemStack held) {
        RoadhouseNpcEntity npc = new RoadhouseNpcEntity(SoldierRegistry.ROADHOUSE_NPC, level.getLevel());
        npc.setRole(role, 0);
        npc.setItemSlot(EquipmentSlot.MAINHAND, held);
        npc.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        float yaw = facing.toYRot();
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        level.addFreshEntityWithPassengers(npc);
    }
}
