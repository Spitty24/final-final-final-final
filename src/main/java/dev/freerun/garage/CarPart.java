package dev.freerun.garage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The pieces of a car you build yourself, and the bit each one sets on the car once it's bolted
 * on. A car needs everything marked {@link #needed} before it will drive; the body panels and the
 * windows are only for looks (and the rain).
 */
public enum CarPart {
    WHEEL_FL("Front-left wheel", true), WHEEL_FR("Front-right wheel", true),
    WHEEL_RL("Rear-left wheel", true), WHEEL_RR("Rear-right wheel", true),
    ENGINE("Engine", true), TRANSMISSION("Transmission", true), BATTERY("Battery", true),
    FUEL_TANK("Fuel tank", true), STEERING("Steering column", true), SEATS("Seats", true),
    BODY("Body panels", false), GLASS("Windows", false);

    public static final int ALL = (1 << values().length) - 1;
    public static final int WHEELS = 0b1111;

    public final String title;
    public final boolean needed;

    CarPart(String title, boolean needed) {
        this.title = title;
        this.needed = needed;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public static int neededMask() {
        int m = 0;
        for (CarPart p : values()) if (p.needed) m |= p.bit();
        return m;
    }

    /** The item for each kind of part (all four wheels are the same item). */
    public Item item() {
        return switch (this) {
            case WHEEL_FL, WHEEL_FR, WHEEL_RL, WHEEL_RR -> GarageRegistry.WHEEL;
            case ENGINE -> GarageRegistry.ENGINE;
            case TRANSMISSION -> GarageRegistry.TRANSMISSION;
            case BATTERY -> GarageRegistry.BATTERY;
            case FUEL_TANK -> GarageRegistry.FUEL_TANK;
            case STEERING -> GarageRegistry.STEERING;
            case SEATS -> GarageRegistry.SEATS;
            case BODY -> GarageRegistry.BODY_PANELS;
            case GLASS -> GarageRegistry.WINDOWS;
        };
    }

    /** An item that bolts one kind of part onto a car under construction. */
    public static class PartItem extends Item {
        private final CarPart part;
        private final String hint;

        public PartItem(CarPart part, String hint, Properties p) {
            super(p);
            this.part = part;
            this.hint = hint;
        }

        public CarPart part() {
            return part;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal(hint).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Right-click a car chassis to fit it.").withStyle(ChatFormatting.DARK_GRAY));
            if (!part.needed) lines.add(Component.literal("Optional: the car drives without it.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
