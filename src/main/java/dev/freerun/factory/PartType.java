package dev.freerun.factory;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

/**
 * The shapes a steel part can have. Sizes are in blocks, for rotation 0 (long side along X):
 * w = X extent, h = height, d = Z extent. Odd rotations swap w and d.
 *
 * New types must be appended: the ordinal is saved with each part.
 */
public enum PartType {
    /** Flat 1x1 sheet, straight off the stock rack. */
    SHEET(1.0, 0.0625, 1.0),
    /** Half a sheet: what the press brake's CUT mode makes. */
    PLATE(1.0, 0.0625, 0.5),
    /** L-shaped angle: a sheet bent 90 degrees. */
    BRACKET(1.0, 0.5, 0.5),
    /** U-shaped channel: a plate bent twice. */
    CHANNEL(1.0, 0.375, 0.375),
    /** A part cut on the laser: a 1x1 sheet's footprint, carrying the shape that was drawn. */
    LASER(1.0, 0.0625, 1.0);

    public final double w;
    public final double h;
    public final double d;

    PartType(double w, double h, double d) {
        this.w = w;
        this.h = h;
        this.d = d;
    }

    public Item item() {
        return switch (this) {
            case SHEET -> FactoryRegistry.STEEL_SHEET;
            case PLATE -> FactoryRegistry.STEEL_PLATE;
            case BRACKET -> FactoryRegistry.STEEL_BRACKET;
            case CHANNEL -> FactoryRegistry.STEEL_CHANNEL;
            case LASER -> dev.freerun.laser.LaserRegistry.LASER_PART;
        };
    }

    public String displayName() {
        return switch (this) {
            case SHEET -> "sheet";
            case PLATE -> "plate";
            case BRACKET -> "bracket";
            case CHANNEL -> "channel";
            case LASER -> "laser-cut part";
        };
    }

    public static PartType byId(int id) {
        PartType[] all = values();
        return all[Mth.clamp(id, 0, all.length - 1)];
    }
}
