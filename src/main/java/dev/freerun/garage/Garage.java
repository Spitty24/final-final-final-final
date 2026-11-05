package dev.freerun.garage;

import dev.freerun.vehicle.CarEntity;
import dev.freerun.vehicle.VehicleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Building cars from parts and tuning them with the wrench. */
public final class Garage {
    private Garage() {}

    public static final float[] TUNES = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};

    /** Part items and the wrench on a car. PASS for anything else. */
    public static InteractionResult interact(CarEntity car, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = car.level().isClientSide;
        if (held.getItem() instanceof CarPart.PartItem pi) {
            if (client) return InteractionResult.SUCCESS;
            CarPart part = pi.part();
            if (part.item() == GarageRegistry.WHEEL) {   // the next empty wheel hub
                part = null;
                for (CarPart w : new CarPart[]{CarPart.WHEEL_FL, CarPart.WHEEL_FR, CarPart.WHEEL_RL, CarPart.WHEEL_RR}) {
                    if (!car.has(w)) {
                        part = w;
                        break;
                    }
                }
                if (part == null) {
                    player.displayClientMessage(Component.literal("§7All four wheels are already on."), true);
                    return InteractionResult.CONSUME;
                }
            } else if (car.has(part)) {
                player.displayClientMessage(Component.literal("§7It already has " + part.title.toLowerCase() + "."), true);
                return InteractionResult.CONSUME;
            }
            if (!car.getPassengers().isEmpty()) {
                player.displayClientMessage(Component.literal("§cEveryone out of the car first."), true);
                return InteractionResult.CONSUME;
            }
            car.setBuild(car.build() | part.bit());
            held.consume(1, player);
            car.playSound(SoundEvents.ANVIL_USE, 0.5f, 1.4f);
            car.playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.9f);
            if (car.roadworthy() && !wasRoadworthy(car, part)) {
                player.displayClientMessage(Component.literal("§a§lIt runs! §7Climb in and turn the key. Fuel: " + Math.round(car.fuel()) + "%"), false);
                car.playSound(dev.freerun.economy.ModSounds.CAR_ENGINE, 1.0f, 1.0f);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) dev.freerun.career.Careers.addXp(sp, 40, "built a car");
            } else {
                player.displayClientMessage(Component.literal("§aFitted: " + part.title + " §7(" + missing(car).size() + " to go)"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (held.is(GarageRegistry.WRENCH)) {
            if (client) return InteractionResult.SUCCESS;
            float now = car.tune();
            int i = 0;
            for (int k = 0; k < TUNES.length; k++) if (Math.abs(TUNES[k] - now) < 0.01) i = k;
            i = player.isShiftKeyDown() ? Math.max(0, i - 1) : Math.min(TUNES.length - 1, i + 1);
            if (player.isShiftKeyDown() && i == 0 && Math.abs(now - TUNES[0]) < 0.01 || !player.isShiftKeyDown() && i == TUNES.length - 1 && Math.abs(now - TUNES[i]) < 0.01) {
                player.displayClientMessage(Component.literal("§7The governor won't go any " + (player.isShiftKeyDown() ? "lower." : "higher.")), true);
                return InteractionResult.CONSUME;
            }
            car.setTune(TUNES[i]);
            car.playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 1.0f, 0.6f + i * 0.1f);
            int kmh = Math.round(72 * TUNES[i]);
            player.displayClientMessage(Component.literal("§eTop speed set to §f" + Math.round(TUNES[i] * 100) + "% §7(about " + kmh + " km/h)"
                    + " · right-click faster, sneak + right-click slower"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static boolean wasRoadworthy(CarEntity car, CarPart just) {
        int need = CarPart.neededMask();
        return ((car.build() & ~just.bit()) & need) == need;
    }

    public static List<CarPart> missing(CarEntity car) {
        List<CarPart> out = new ArrayList<>();
        for (CarPart p : CarPart.values()) if (p.needed && !car.has(p)) out.add(p);
        return out;
    }

    /** What an unfinished car still needs. */
    public static void checklist(CarEntity car, Player player) {
        List<CarPart> need = missing(car);
        StringBuilder sb = new StringBuilder("§6This car won't drive yet. §7Still needs: §f");
        for (int i = 0; i < need.size(); i++) sb.append(i > 0 ? ", " : "").append(need.get(i).title);
        player.displayClientMessage(Component.literal(sb.toString()), false);
    }

    /** Sneak-punching an unfinished car: the chassis and every part on it, back as items. */
    public static List<ItemStack> dismantle(CarEntity car) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(GarageRegistry.CHASSIS));
        for (CarPart p : CarPart.values()) if (car.has(p)) out.add(new ItemStack(p.item()));
        return out;
    }

    /** A bare chassis: wheels and all still to come. */
    public static CarEntity chassis(net.minecraft.world.level.Level level) {
        CarEntity car = new CarEntity(VehicleRegistry.CAR, level);
        car.setBuild(0);
        car.setAlignment(Alignment.worn(level.random));
        return car;
    }
}
