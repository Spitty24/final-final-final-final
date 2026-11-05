package dev.freerun.laser;

import dev.freerun.factory.FactoryRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** The server side of the CNC terminal's CUT button. */
public final class LaserJobs {
    private LaserJobs() {}

    public static void cut(ServerPlayer p, CompoundTag data) {
        BlockPos pos = BlockPos.of(data.getLong("Cutter"));
        if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 12 * 12) return;
        if (!(p.level().getBlockEntity(pos) instanceof LaserCutterBlockEntity be)) return;
        if (be.busy()) {
            p.displayClientMessage(Component.literal("§cThe laser is still cutting. Wait for the bed to clear."), true);
            return;
        }
        long[] shape = LaserShape.copy(data.getLongArray("Shape"));
        if (LaserShape.empty(shape)) {
            p.displayClientMessage(Component.literal("§cDraw a part first."), true);
            return;
        }
        int gas = data.getInt("Gas") == LaserCutterBlockEntity.NITROGEN ? LaserCutterBlockEntity.NITROGEN : LaserCutterBlockEntity.OXYGEN;
        if (!p.getAbilities().instabuild) {
            int slot = -1;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(FactoryRegistry.STEEL_SHEET)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                p.displayClientMessage(Component.literal("§cNo steel sheet to load. §7Bring a Steel Sheet (the rack by the west wall has plenty)."), true);
                return;
            }
            p.getInventory().getItem(slot).shrink(1);
        }
        be.startJob(p, shape, gas);
        int secs = Math.max(1, be.duration() / 20);
        p.displayClientMessage(Component.literal("§6Sheet loaded. §7Cutting with " + (gas == 0 ? "oxygen" : "nitrogen") + ", about " + secs + "s."), true);
        TarynEntity.onCutStarted(p.serverLevel(), pos, p);
    }
}
