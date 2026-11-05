package dev.freerun.life;

import dev.freerun.city.CityLayout;
import dev.freerun.city.CityWorld;
import dev.freerun.economy.Money;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renting a place. Every house in the suburbs and every apartment building in the hood has a
 * mailbox out front; right-click a vacant one to see the terms, and again to sign. You pay a
 * deposit (two days' rent) and the first day up front, then rent comes out of your account each
 * morning. Your bed there becomes your respawn point, and nobody else can break, build or open
 * things on your lot. Miss three days' rent and you're evicted. Sneak-right-click your mailbox to
 * move out and get your deposit back. One place at a time.
 */
public final class Rentals {
    public static final int HOUSE_RENT = 40, APARTMENT_RENT = 25;

    static final class Lease {
        UUID tenant;
        String name;
        int rent;
        BoundingBox plot;
        long paidThrough;
        int overdue;
        long lastWarned;
        boolean apartment;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Tenant", tenant);
            t.putString("Name", name);
            t.putInt("Rent", rent);
            t.putIntArray("Plot", new int[]{plot.minX(), plot.minY(), plot.minZ(), plot.maxX(), plot.maxY(), plot.maxZ()});
            t.putLong("Paid", paidThrough);
            t.putInt("Overdue", overdue);
            t.putBoolean("Apartment", apartment);
            return t;
        }

        static Lease load(CompoundTag t) {
            Lease l = new Lease();
            l.tenant = t.getUUID("Tenant");
            l.name = t.getString("Name");
            l.rent = t.getInt("Rent");
            int[] b = t.getIntArray("Plot");
            l.plot = b.length == 6 ? new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]) : new BoundingBox(0, 0, 0, 0, 0, 0);
            l.paidThrough = t.getLong("Paid");
            l.overdue = t.getInt("Overdue");
            l.apartment = t.getBoolean("Apartment");
            return l;
        }
    }

    public static final class Data extends SavedData {
        final Map<BlockPos, Lease> leases = new HashMap<>();

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Map.Entry<BlockPos, Lease> e : leases.entrySet()) {
                CompoundTag c = e.getValue().save();
                c.putLong("Mailbox", e.getKey().asLong());
                list.add(c);
            }
            tag.put("Leases", list);
            return tag;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (Tag t : tag.getList("Leases", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                d.leases.put(BlockPos.of(c.getLong("Mailbox")), Lease.load(c));
            }
            return d;
        }
    }

    private static final SavedData.Factory<Data> FACTORY = new SavedData.Factory<>(Data::new, Data::load,
            net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private static final Map<UUID, BlockPos> SIGNING = new HashMap<>();
    private static final Map<UUID, Long> SIGNING_AT = new HashMap<>();

    private Rentals() {}

    static Data data(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "freerun_rentals");
    }

    private static long today(ServerLevel level) {
        return level.getServer().overworld().getDayTime() / 24000L;
    }

    // ------------------------------------------------------------------ the mailbox

    static void mailbox(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        Data d = data(level);
        Lease l = d.leases.get(pos);
        if (l != null) {
            if (!l.tenant.equals(p.getUUID())) {
                p.displayClientMessage(Component.literal("§7This place is rented by §f" + l.name + "§7. There's a lot of mail in the box."), true);
                return;
            }
            if (p.isShiftKeyDown()) {
                d.leases.remove(pos);
                d.setDirty();
                int back = l.overdue == 0 ? l.rent * 2 : 0;
                if (back > 0) Money.give(p, back);
                p.displayClientMessage(Component.literal("§e🏠 You moved out. " + (back > 0 ? "Deposit returned: §a" + Money.fmt(back)
                        : "§cYour deposit covered the rent you owed.")), false);
                return;
            }
            long left = l.paidThrough - today(level);
            p.displayClientMessage(Component.literal("§a🏠 Home sweet home. §7Rent " + Money.fmt(l.rent) + "/day, "
                    + (l.overdue > 0 ? "§c" + l.overdue + " day(s) overdue!" : left >= 0 ? "paid through tomorrow morning" : "due now")
                    + "§7. Sneak-right-click to move out."), false);
            return;
        }
        BoundingBox plot = plotFor(level, pos);
        boolean apartment = isApartment(level, pos);
        int rent = apartment ? APARTMENT_RENT : HOUSE_RENT;
        if (plot == null) {
            p.displayClientMessage(Component.literal("§7Just a mailbox. (Only city houses and apartments are for rent.)"), true);
            return;
        }
        for (Map.Entry<BlockPos, Lease> e : d.leases.entrySet()) {
            if (e.getValue().tenant.equals(p.getUUID())) {
                p.displayClientMessage(Component.literal("§cYou already rent a place. Sneak-right-click its mailbox to move out first."), false);
                return;
            }
        }
        long now = level.getGameTime();
        if (!pos.equals(SIGNING.get(p.getUUID())) || now - SIGNING_AT.getOrDefault(p.getUUID(), 0L) > 200) {
            SIGNING.put(p.getUUID(), pos.immutable());
            SIGNING_AT.put(p.getUUID(), now);
            p.displayClientMessage(Component.literal("§e🏠 FOR RENT: " + (apartment ? "an apartment in the hood" : "a furnished house with a yard and driveway")
                    + ". §f" + Money.fmt(rent) + "/day§7, plus a " + Money.fmt(rent * 2) + " deposit. Your bed here becomes your spawn, "
                    + "and nobody else can touch your place. §eRight-click again to sign."), false);
            return;
        }
        SIGNING.remove(p.getUUID());
        int upfront = rent * 3;
        if (!Money.pay(p, upfront)) {
            p.displayClientMessage(Component.literal("§cYou need " + Money.fmt(upfront) + " to move in (deposit + first day)."), false);
            return;
        }
        Lease nl = new Lease();
        nl.tenant = p.getUUID();
        nl.name = p.getName().getString();
        nl.rent = rent;
        nl.plot = plot;
        nl.paidThrough = today(level);
        nl.apartment = apartment;
        d.leases.put(pos.immutable(), nl);
        d.setDirty();
        BlockPos bed = findBed(level, plot);
        p.setRespawnPosition(level.dimension(), bed != null ? bed : pos.above(), 0.0f, true, false);
        level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.8f, 1.0f);
        p.displayClientMessage(Component.literal("§a🔑 Lease signed! Welcome home. §7Buy furniture at Bayou Home Furnishings and make it yours."), false);
    }

    private static boolean isApartment(ServerLevel level, BlockPos pos) {
        BlockPos city = CityWorld.cityAt(level, pos);
        return city != null && CityLayout.districtAt(pos.getX() + 0.5 - city.getX(), pos.getZ() + 0.5 - city.getZ()) == CityLayout.District.HOOD;
    }

    /** The lot that goes with a mailbox: a house's 19 x 19 plot, or the apartment building behind it. */
    static BoundingBox plotFor(ServerLevel level, BlockPos pos) {
        BlockPos city = CityWorld.cityAt(level, pos);
        if (city == null) return null;
        double lx = pos.getX() + 0.5 - city.getX(), lz = pos.getZ() + 0.5 - city.getZ();
        CityLayout.Lot lot = CityLayout.lotAt(lx, lz);
        if (lot == null) return null;
        int y = city.getY();
        if (lot.district() == CityLayout.District.SUBURB) {
            int px = (int) ((lx - lot.lx()) / 19), pz = (int) ((lz - lot.lz()) / 19);
            int x0 = city.getX() + lot.lx() + px * 19, z0 = city.getZ() + lot.lz() + pz * 19;
            return new BoundingBox(x0, y - 2, z0, x0 + 18, y + 12, z0 + 18);
        }
        if (lot.district() == CityLayout.District.HOOD) {
            return new BoundingBox(pos.getX() - 7, y - 2, pos.getZ(), pos.getX() + 11, y + 20, pos.getZ() + 17);
        }
        return null;
    }

    private static BlockPos findBed(ServerLevel level, BoundingBox b) {
        for (BlockPos q : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), Math.min(b.maxY(), b.minY() + 8), b.maxZ())) {
            var st = level.getBlockState(q);
            if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) return q.immutable();
        }
        return null;
    }

    /** "Rent $40/day at 120 64 -300" for the phone, or empty. */
    public static String describe(ServerPlayer p) {
        for (Map.Entry<BlockPos, Lease> e : data(p.serverLevel()).leases.entrySet()) {
            Lease l = e.getValue();
            if (!l.tenant.equals(p.getUUID())) continue;
            BlockPos at = e.getKey();
            return (l.apartment ? "Apartment" : "House") + " at " + at.getX() + ", " + at.getZ() + " · " + Money.fmt(l.rent) + "/day"
                    + (l.overdue > 0 ? " · " + l.overdue + " day(s) late!" : "");
        }
        return "";
    }

    // ------------------------------------------------------------------ rent day and the rules of the lot

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 100 != 17) return;
            Data d = data(level);
            if (d.leases.isEmpty()) return;
            long today = today(level);
            var it = d.leases.entrySet().iterator();
            while (it.hasNext()) {
                Lease l = it.next().getValue();
                if (l.paidThrough >= today) continue;
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(l.tenant);
                if (p == null) continue;   // you don't get charged while you're away
                if (Money.pay(p, l.rent)) {
                    l.paidThrough++;
                    l.overdue = 0;
                    p.displayClientMessage(Component.literal("§7🏠 Rent paid: " + Money.fmt(l.rent) + ". §8(balance " + Money.fmt(Money.balance(p)) + ")"), false);
                } else if (level.getGameTime() - l.lastWarned > 24000 - 200) {
                    l.lastWarned = level.getGameTime();
                    l.overdue++;
                    l.paidThrough++;
                    if (l.overdue >= 3) {
                        it.remove();
                        p.displayClientMessage(Component.literal("§c🏠 Three days without rent: you've been evicted. The landlord kept your deposit."), false);
                    } else {
                        p.displayClientMessage(Component.literal("§c🏠 You couldn't cover rent (" + Money.fmt(l.rent) + "). Day " + l.overdue
                                + " of 3 before eviction."), false);
                    }
                }
                d.setDirty();
            }
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!(world instanceof ServerLevel level) || allowed(level, player, pos)) return true;
            player.displayClientMessage(Component.literal("§cThat's somebody's home."), true);
            return false;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (level.getBlockState(pos).is(LifeRegistry.MAILBOX)) return InteractionResult.PASS;
            if (allowed(level, player, pos) && allowed(level, player, pos.relative(hit.getDirection()))) return InteractionResult.PASS;
            player.displayClientMessage(Component.literal("§cThat's somebody's home. Knock first."), true);
            return InteractionResult.FAIL;
        });
    }

    private static boolean allowed(ServerLevel level, Player player, BlockPos pos) {
        if (player.hasPermissions(2) && player.isCreative()) return true;
        Data d = data(level);
        if (d.leases.isEmpty()) return true;
        for (Lease l : d.leases.values()) {
            if (l.plot.isInside(pos) && !l.tenant.equals(player.getUUID())) return false;
        }
        return true;
    }
}
