package dev.freerun.life;

import dev.freerun.career.Careers;
import dev.freerun.city.CitizenEntity;
import dev.freerun.city.CityLayout;
import dev.freerun.city.CityWorld;
import dev.freerun.economy.Money;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Office jobs. Pick one at the job board in any office tower's lobby and you're on the clock:
 * work turns up around you while you're in the building, and every job done pays straight into
 * your account (plus a little career XP). Leave the building for a minute, or quit at the
 * board, and your shift ends.
 *
 *   Janitor        spills and split trash bags appear; clean them with the mop
 *   IT Technician  computers crash, printers jam, server racks throw alarms; fix them with the toolkit
 *   Accountant     ledgers pop up on nearby computers; work out the balance
 *   Office Manager coworkers bring you requests; approve or deny them by company policy
 *   Receptionist   the desk phone rings; put the caller through to the right department
 *
 * Every ten jobs on one shift is a raise.
 */
public final class OfficeJobs {
    public enum Job {
        JANITOR("Janitor", 9, "Mop up spills and haul off trash around the floor."),
        IT("IT Technician", 15, "Fix crashed computers, jammed printers and server alarms."),
        ACCOUNTANT("Accountant", 13, "Balance the ledgers that land on nearby computers."),
        MANAGER("Office Manager", 17, "Approve or deny your team's requests by company policy."),
        RECEPTIONIST("Receptionist", 8, "Answer the phone and put callers through.");

        public final String title;
        public final int pay;
        public final String blurb;

        Job(String title, int pay, String blurb) {
            this.title = title;
            this.pay = pay;
            this.blurb = blurb;
        }
    }

    private record Pending(int answer, String kind, BlockPos pos, int entity) {}

    private static final class Shift {
        final Job job;
        int done, earned;
        long nextTask, lastInside;
        final Set<BlockPos> work = new HashSet<>();
        final Map<Integer, Pending> tasks = new HashMap<>();
        final Map<Integer, Integer> requests = new HashMap<>();   // coworker entity id -> task id
        final Map<BlockPos, Long> ringing = new HashMap<>();

        Shift(Job job, long now) {
            this.job = job;
            this.nextTask = now + 100;
            this.lastInside = now;
        }
    }

    private static final Map<UUID, Shift> SHIFTS = new HashMap<>();
    private static final Map<BlockPos, Integer> REPAIRS = new HashMap<>();
    private static int nextId = 1;

    private OfficeJobs() {}

    // ------------------------------------------------------------------ where am I

    public static boolean inOffice(ServerPlayer p) {
        BlockPos city = CityWorld.cityAt(p.serverLevel(), p.blockPosition());
        if (city == null || p.getY() < city.getY()) return false;
        CityLayout.District d = CityLayout.districtAt(p.getX() - city.getX(), p.getZ() - city.getZ());
        return d == CityLayout.District.OFFICE;
    }

    /** "Janitor · 4 jobs · $36" for the phone, or empty. */
    public static String describe(ServerPlayer p) {
        Shift s = SHIFTS.get(p.getUUID());
        return s == null ? "" : s.job.title + " · " + s.done + " jobs · " + Money.fmt(s.earned) + " this shift";
    }

    public static boolean onShift(ServerPlayer p) {
        return SHIFTS.containsKey(p.getUUID());
    }

    // ------------------------------------------------------------------ the job board

    static void openBoard(ServerPlayer p, BlockPos pos) {
        Shift s = SHIFTS.get(p.getUUID());
        CompoundTag t = new CompoundTag();
        t.putInt("Job", s == null ? -1 : s.job.ordinal());
        t.putInt("Done", s == null ? 0 : s.done);
        t.putInt("Earned", s == null ? 0 : s.earned);
        t.putInt("Bonus", bonus(p));
        LifePayloads.open(p, LifePayloads.BOARD, t);
        p.serverLevel().playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    private static int bonus(ServerPlayer p) {
        return Careers.get(p).level() / 2;
    }

    static void pick(ServerPlayer p, CompoundTag data) {
        int j = data.getInt("Job");
        Shift old = SHIFTS.remove(p.getUUID());
        if (old != null) {
            p.displayClientMessage(Component.literal("§7You clock out. §fShift total: §a" + Money.fmt(old.earned) + " §7for " + old.done + " job"
                    + (old.done == 1 ? "" : "s") + "."), false);
        }
        if (j < 0 || j >= Job.values().length) return;
        if (!inOffice(p)) {
            p.displayClientMessage(Component.literal("§cYou need to be in an office building to start a shift."), false);
            return;
        }
        Job job = Job.values()[j];
        SHIFTS.put(p.getUUID(), new Shift(job, p.serverLevel().getGameTime()));
        p.displayClientMessage(Component.literal("§a⏱ Clocked in as §f" + job.title + "§a. §7" + job.blurb
                + " Base pay " + Money.fmt(job.pay + bonus(p)) + " a job."), false);
        if (job == Job.JANITOR) give(p, new ItemStack(LifeRegistry.MOP));
        if (job == Job.IT) give(p, new ItemStack(LifeRegistry.IT_TOOLKIT));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (dev.freerun.economy.Shops.count(p, stack.getItem()) > 0) return;
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    // ------------------------------------------------------------------ getting paid

    private static void pay(ServerPlayer p, Shift s, String what) {
        int raise = s.done / 10;
        int amount = (int) Math.round((s.job.pay + bonus(p)) * (1.0 + 0.1 * raise));
        s.done++;
        s.earned += amount;
        Money.give(p, amount);
        Careers.addXp(p, 4, "office work");
        CityWorld.recordWages(p.serverLevel(), p.blockPosition(), amount);
        p.displayClientMessage(Component.literal("§a✔ " + what + " §f+" + Money.fmt(amount)), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
        if (s.done % 10 == 0) {
            p.displayClientMessage(Component.literal("§6★ Ten jobs this shift: you got a raise! §7(+" + (raise + 1) * 10 + "% pay)"), false);
        }
    }

    // ------------------------------------------------------------------ the shift loop

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 20 != 3 || SHIFTS.isEmpty()) return;
            long now = level.getGameTime();
            Iterator<Map.Entry<UUID, Shift>> it = SHIFTS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Shift> e = it.next();
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                if (p.serverLevel() != level) continue;
                Shift s = e.getValue();
                if (inOffice(p)) {
                    s.lastInside = now;
                } else if (now - s.lastInside > 20 * 60) {
                    p.displayClientMessage(Component.literal("§7You left the building, so you've been clocked out. §fShift total: §a"
                            + Money.fmt(s.earned)), false);
                    it.remove();
                    continue;
                } else {
                    continue;
                }
                ringPhones(level, p, s, now);
                if (now >= s.nextTask && open(s) < 3) {
                    if (makeWork(level, p, s)) s.nextTask = now + 20L * (12 + level.getRandom().nextInt(16));
                    else s.nextTask = now + 100;
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SHIFTS.remove(handler.player.getUUID()));
    }

    private static int open(Shift s) {
        return s.work.size() + s.tasks.size();
    }

    private static String where(ServerPlayer p, BlockPos at) {
        double dx = at.getX() + 0.5 - p.getX(), dz = at.getZ() + 0.5 - p.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        int dy = at.getY() - p.getBlockY();
        String floor = dy > 2 ? " (upstairs)" : dy < -2 ? " (downstairs)" : "";
        return dist + " blocks " + dev.freerun.city.CityCommands.compass(dx, dz) + floor;
    }

    private static boolean makeWork(ServerLevel level, ServerPlayer p, Shift s) {
        RandomSource r = level.getRandom();
        switch (s.job) {
            case JANITOR -> {
                for (int i = 0; i < 30; i++) {
                    BlockPos at = p.blockPosition().offset(r.nextInt(25) - 12, 0, r.nextInt(25) - 12);
                    if (at.distSqr(p.blockPosition()) < 16) continue;
                    if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                    if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) continue;
                    if (!inOfficeAt(level, at)) continue;
                    boolean trash = r.nextInt(3) == 0;
                    level.setBlock(at, (trash ? LifeRegistry.TRASH_BAG : LifeRegistry.SPILL).defaultBlockState(), Block.UPDATE_ALL);
                    s.work.add(at.immutable());
                    p.displayClientMessage(Component.literal("§e🧹 " + (trash ? "A trash bag split open " : "Someone spilled coffee ")
                            + where(p, at) + "."), false);
                    return true;
                }
            }
            case IT -> {
                int pick = r.nextInt(4);
                if (pick < 2) {
                    BlockPos c = find(level, p.blockPosition(), 14, 6, st -> st.is(LifeRegistry.COMPUTER) && st.getValue(ComputerBlock.SCREEN) == ComputerBlock.ON);
                    if (c != null) {
                        level.setBlock(c, level.getBlockState(c).setValue(ComputerBlock.SCREEN, ComputerBlock.CRASHED), Block.UPDATE_ALL);
                        s.work.add(c);
                        p.displayClientMessage(Component.literal("§b💻 Ticket: a computer crashed " + where(p, c) + ". (Blue screen.)"), false);
                        return true;
                    }
                }
                Block b = pick == 2 ? LifeRegistry.PRINTER : LifeRegistry.SERVER_RACK;
                BlockPos c = find(level, p.blockPosition(), 16, 6, st -> st.is(b) && !st.getValue(FurnitureBlock.LIT));
                if (c != null) {
                    level.setBlock(c, level.getBlockState(c).setValue(FurnitureBlock.LIT, true), Block.UPDATE_ALL);
                    s.work.add(c);
                    p.displayClientMessage(Component.literal("§b💻 Ticket: " + (b == LifeRegistry.PRINTER ? "printer jam" : "server alarm")
                            + " " + where(p, c) + "."), false);
                    return true;
                }
            }
            case ACCOUNTANT -> {
                BlockPos c = find(level, p.blockPosition(), 14, 6, st -> st.is(LifeRegistry.COMPUTER) && st.getValue(ComputerBlock.SCREEN) == ComputerBlock.ON);
                if (c != null) {
                    level.setBlock(c, level.getBlockState(c).setValue(ComputerBlock.SCREEN, ComputerBlock.LEDGER), Block.UPDATE_ALL);
                    s.work.add(c);
                    p.displayClientMessage(Component.literal("§a📊 A ledger needs balancing on the computer " + where(p, c) + "."), false);
                    return true;
                }
            }
            case MANAGER -> {
                List<CitizenEntity> team = level.getEntitiesOfClass(CitizenEntity.class, p.getBoundingBox().inflate(20, 6, 20),
                        c -> !s.requests.containsKey(c.getId()) && c.distanceToSqr(p) > 9);
                if (!team.isEmpty()) {
                    CitizenEntity c = team.get(r.nextInt(team.size()));
                    int id = request(p, s, c);
                    s.requests.put(c.getId(), id);
                    c.setGlowingTag(true);
                    p.displayClientMessage(Component.literal("§d📋 " + c.getName().getString() + " (glowing) needs your sign-off on something."), false);
                    return true;
                }
                // nobody around: it comes in by email
                int id = request(p, s, null);
                send(p, id);
                return true;
            }
            case RECEPTIONIST -> {
                BlockPos c = find(level, p.blockPosition(), 16, 6, st -> st.is(LifeRegistry.DESK_PHONE) && !st.getValue(FurnitureBlock.LIT));
                if (c != null) {
                    level.setBlock(c, level.getBlockState(c).setValue(FurnitureBlock.LIT, true), Block.UPDATE_ALL);
                    s.ringing.put(c, level.getGameTime());
                    s.work.add(c);
                    p.displayClientMessage(Component.literal("§e☎ The phone's ringing " + where(p, c) + "!"), false);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean inOfficeAt(ServerLevel level, BlockPos at) {
        BlockPos city = CityWorld.cityAt(level, at);
        return city != null && CityLayout.districtAt(at.getX() + 0.5 - city.getX(), at.getZ() + 0.5 - city.getZ()) == CityLayout.District.OFFICE;
    }

    private static BlockPos find(ServerLevel level, BlockPos c, int r, int dy, Predicate<BlockState> test) {
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, dy, r))) {
            if (test.test(level.getBlockState(q))) hits.add(q.immutable());
        }
        return hits.isEmpty() ? null : hits.get(level.getRandom().nextInt(hits.size()));
    }

    private static void ringPhones(ServerLevel level, ServerPlayer p, Shift s, long now) {
        Iterator<Map.Entry<BlockPos, Long>> it = s.ringing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            BlockPos at = e.getKey();
            if (now - e.getValue() > 20 * 25) {
                it.remove();
                s.work.remove(at);
                BlockState st = level.getBlockState(at);
                if (st.is(LifeRegistry.DESK_PHONE)) level.setBlock(at, st.setValue(FurnitureBlock.LIT, false), Block.UPDATE_ALL);
                p.displayClientMessage(Component.literal("§7The caller hung up."), true);
            } else if ((now - e.getValue()) % 40 < 20) {
                level.playSound(null, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 1.9f);
            }
        }
    }

    // ------------------------------------------------------------------ the work itself

    static void clean(ServerPlayer p, BlockPos pos, boolean mop) {
        if (!mop) {
            p.displayClientMessage(Component.literal("§7You'll want a mop for that. (Janitors get one at the job board.)"), true);
            return;
        }
        p.serverLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        p.serverLevel().playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.1);
        Shift s = SHIFTS.get(p.getUUID());
        if (s != null && s.job == Job.JANITOR && s.work.remove(pos)) pay(p, s, "Cleaned up.");
        else p.displayClientMessage(Component.literal("§7Squeaky clean."), true);
    }

    /** IT toolkit on a block: three goes to fix a crashed computer, a jammed printer or a server alarm. */
    static boolean toolkit(ServerPlayer p, BlockPos pos, BlockState state) {
        boolean broken = state.is(LifeRegistry.COMPUTER) && state.getValue(ComputerBlock.SCREEN) == ComputerBlock.CRASHED
                || (state.is(LifeRegistry.PRINTER) || state.is(LifeRegistry.SERVER_RACK)) && state.getValue(FurnitureBlock.LIT);
        if (!broken) {
            p.displayClientMessage(Component.literal("§7Nothing wrong with that one."), true);
            return state.getBlock() instanceof ComputerBlock || state.getBlock() instanceof FurnitureBlock;
        }
        ServerLevel level = p.serverLevel();
        int n = REPAIRS.merge(pos.immutable(), 1, Integer::sum);
        String[] steps = state.is(LifeRegistry.COMPUTER) ? new String[]{"Turning it off and on again...", "Running diagnostics...", "Fixed!"}
                : state.is(LifeRegistry.PRINTER) ? new String[]{"Pulling out a crumpled page...", "Clearing the rollers...", "Fixed!"}
                : new String[]{"Checking the logs...", "Reseating a cable...", "Fixed!"};
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.4f, 1.6f + n * 0.1f);
        if (n < 3) {
            p.displayClientMessage(Component.literal("§b🔧 " + steps[n - 1]), true);
            return true;
        }
        REPAIRS.remove(pos);
        if (state.is(LifeRegistry.COMPUTER)) level.setBlock(pos, state.setValue(ComputerBlock.SCREEN, ComputerBlock.ON), Block.UPDATE_ALL);
        else level.setBlock(pos, state.setValue(FurnitureBlock.LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
        Shift s = SHIFTS.get(p.getUUID());
        if (s != null && s.job == Job.IT && s.work.remove(pos)) pay(p, s, "Ticket closed.");
        else p.displayClientMessage(Component.literal("§b🔧 Fixed! §7(Not your ticket, but they'll appreciate it.)"), true);
        return true;
    }

    static void repairComputer(ServerPlayer p, BlockPos pos, BlockState state) {
        p.displayClientMessage(Component.literal(p.getMainHandItem().is(LifeRegistry.IT_TOOLKIT) ? "§7Right-click it with the toolkit."
                : "§9:( §7Blue screen. \"Your PC ran into a problem.\" Someone should call IT."), true);
    }

    static boolean fixPrinter(ServerPlayer p, BlockPos pos) {
        if (!p.serverLevel().getBlockState(pos).getValue(FurnitureBlock.LIT)) return false;
        p.displayClientMessage(Component.literal("§cPC LOAD LETTER. §7It's jammed. IT has a toolkit for this."), true);
        return true;
    }

    static boolean fixServer(ServerPlayer p, BlockPos pos) {
        if (!p.serverLevel().getBlockState(pos).getValue(FurnitureBlock.LIT)) return false;
        p.displayClientMessage(Component.literal("§c⚠ ALARM: disk 3 degraded. §7Somebody from IT should look at this."), true);
        return true;
    }

    // ------------------------------------------------------------------ ledgers, requests and calls

    static void openLedger(ServerPlayer p, BlockPos pos, BlockState state) {
        Shift s = SHIFTS.get(p.getUUID());
        if (s == null || s.job != Job.ACCOUNTANT) {
            p.displayClientMessage(Component.literal("§7A spreadsheet's open, waiting for an accountant. It looks very boring."), true);
            return;
        }
        RandomSource r = p.getRandom();
        int balance = 100 + r.nextInt(80) * 10;
        String[] what = {"Office supplies", "Client payment", "Coffee service", "Software licence", "Consulting fee", "Refund", "Catering", "Invoice #"};
        StringBuilder sb = new StringBuilder("Opening balance: $" + balance + "\n");
        int total = balance;
        int lines = 3 + r.nextInt(2);
        for (int i = 0; i < lines; i++) {
            int amt = (5 + r.nextInt(60)) * 5;
            boolean in = r.nextBoolean();
            total += in ? amt : -amt;
            String w = what[r.nextInt(what.length)];
            if (w.endsWith("#")) w += 1000 + r.nextInt(9000);
            sb.append(in ? "  + $" : "  - $").append(amt).append("   ").append(w).append("\n");
        }
        sb.append("\nWhat's the closing balance?");
        int correct = r.nextInt(4);
        List<String> opts = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        used.add(total);
        for (int i = 0; i < 4; i++) {
            int v;
            if (i == correct) v = total;
            else do v = total + (r.nextBoolean() ? 1 : -1) * (1 + r.nextInt(12)) * 10; while (!used.add(v));
            opts.add("$" + v);
        }
        int id = nextId++;
        s.tasks.put(id, new Pending(correct, "ledger", pos, -1));
        sendTask(p, id, "📊 Ledger", sb.toString(), opts);
    }

    private static final String[] NAMES = {"Gary", "Denise", "Brayden", "Tamika", "Lou", "Priya", "Kevin", "Becky", "Marcus", "Hollis"};

    /** Builds a policy question for the manager. Returns its task id. */
    private static int request(ServerPlayer p, Shift s, CitizenEntity from) {
        RandomSource r = p.getRandom();
        String who = from != null ? from.getName().getString().split(" ")[0] : NAMES[r.nextInt(NAMES.length)];
        String prompt;
        boolean approve;
        switch (r.nextInt(6)) {
            case 0 -> {
                int n = 150 + r.nextInt(40) * 10;
                boolean receipt = r.nextBoolean();
                prompt = who + " wants $" + n + " back for a client dinner.\n\nPolicy: meals over $300 need a receipt.\nReceipt attached: " + (receipt ? "yes" : "no");
                approve = n <= 300 || receipt;
            }
            case 1 -> {
                int off = r.nextInt(3);
                prompt = who + " asks for Friday off.\n\nPolicy: at least 2 of the 5 people on the team must be in.\nAlready off on Friday: " + off;
                approve = 5 - off - 1 >= 2;
            }
            case 2 -> {
                int budget = r.nextInt(40) * 10;
                prompt = who + " wants a second monitor ($180).\n\nEquipment budget left this quarter: $" + budget;
                approve = budget >= 180;
            }
            case 3 -> {
                int h = 4 + r.nextInt(12);
                boolean signed = r.nextBoolean();
                prompt = who + " put in for " + h + " hours of overtime.\n\nPolicy: over 10 hours a week needs the director's sign-off.\nDirector signed: " + (signed ? "yes" : "no");
                approve = h <= 10 || signed;
            }
            case 4 -> {
                int meetings = r.nextInt(3);
                prompt = who + " wants to work from home on Wednesday.\n\nPolicy: fine unless they have client meetings.\nClient meetings on Wednesday: " + meetings;
                approve = meetings == 0;
            }
            default -> {
                String[] silly = {"a 'team-building' trip to the casino", "a hot tub for the break room", "a company jet (used)",
                        "an office pet alligator", "a fourth coffee machine"};
                prompt = who + " wants to expense " + silly[r.nextInt(silly.length)] + ".\n\nPolicy: use common sense.";
                approve = false;
            }
        }
        int id = nextId++;
        s.tasks.put(id, new Pending(approve ? 0 : 1, "request\n" + prompt, null, from == null ? -1 : from.getId()));
        return id;
    }

    private static void send(ServerPlayer p, int id) {
        Shift s = SHIFTS.get(p.getUUID());
        if (s == null) return;
        Pending t = s.tasks.get(id);
        if (t == null) return;
        String prompt = t.kind().substring(t.kind().indexOf('\n') + 1);
        sendTask(p, id, "📋 Request", prompt, List.of("Approve", "Deny"));
    }

    /** A manager talking to a coworker who's waiting on them. Returns true if that's what happened. */
    public static boolean talk(ServerPlayer p, CitizenEntity c) {
        Shift s = SHIFTS.get(p.getUUID());
        if (s == null || s.job != Job.MANAGER) return false;
        Integer id = s.requests.get(c.getId());
        if (id == null) return false;
        send(p, id);
        return true;
    }

    private static final String[][] CALLS = {
            {"My screen went blue and nothing works.", "IT"}, {"My password expired and now I'm locked out.", "IT"},
            {"The Wi-Fi in your conference room keeps dropping.", "IT"}, {"I'm calling about an unpaid invoice.", "Accounting"},
            {"We got charged twice on our last bill.", "Accounting"}, {"I need a copy of last year's tax forms.", "Accounting"},
            {"I'd like to file a complaint about one of your employees.", "Management"}, {"I'd like to set up a meeting with your boss.", "Management"},
            {"I'm calling about a partnership proposal.", "Management"}, {"Someone spilled soda all over the lobby carpet.", "Janitorial"},
            {"The restroom on the third floor is flooding!", "Janitorial"}, {"There's a weird smell coming from the break room.", "Janitorial"}};
    private static final List<String> DEPARTMENTS = List.of("IT", "Accounting", "Management", "Janitorial");

    static void answerPhone(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        BlockState st = level.getBlockState(pos);
        Shift s = SHIFTS.get(p.getUUID());
        if (!st.getValue(FurnitureBlock.LIT)) {
            p.displayClientMessage(Component.literal("§7*dial tone*"), true);
            return;
        }
        level.setBlock(pos, st.setValue(FurnitureBlock.LIT, false), Block.UPDATE_ALL);
        if (s == null || s.job != Job.RECEPTIONIST || !s.ringing.containsKey(pos)) {
            p.displayClientMessage(Component.literal("§7\"Hello? ...Hello?\" They hang up."), true);
            return;
        }
        s.ringing.remove(pos);
        s.work.remove(pos);
        String[] call = CALLS[p.getRandom().nextInt(CALLS.length)];
        int id = nextId++;
        s.tasks.put(id, new Pending(DEPARTMENTS.indexOf(call[1]), "call", pos, -1));
        sendTask(p, id, "☎ Incoming call", "\"Thank you for calling, how can I direct your call?\"\n\nCaller: \"" + call[0] + "\"\n\nTransfer to:", DEPARTMENTS);
    }

    private static void sendTask(ServerPlayer p, int id, String title, String prompt, List<String> options) {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Title", title);
        t.putString("Prompt", prompt);
        ListTag l = new ListTag();
        for (String o : options) l.add(StringTag.valueOf(o));
        t.put("Options", l);
        LifePayloads.open(p, LifePayloads.TASK, t);
    }

    static void answer(ServerPlayer p, CompoundTag data) {
        Shift s = SHIFTS.get(p.getUUID());
        if (s == null) return;
        int id = data.getInt("Id");
        Pending t = s.tasks.remove(id);
        if (t == null) return;
        boolean right = data.getInt("Option") == t.answer();
        ServerLevel level = p.serverLevel();
        if (t.kind().equals("ledger") && t.pos() != null) {
            BlockState st = level.getBlockState(t.pos());
            if (st.is(LifeRegistry.COMPUTER)) level.setBlock(t.pos(), st.setValue(ComputerBlock.SCREEN, ComputerBlock.ON), Block.UPDATE_ALL);
            s.work.remove(t.pos());
        }
        if (t.entity() >= 0) {
            s.requests.remove(t.entity());
            if (level.getEntity(t.entity()) instanceof CitizenEntity c) {
                c.setGlowingTag(false);
                if (t.kind().startsWith("request")) {
                    p.displayClientMessage(Component.literal("§f<" + c.getName().getString() + "> " + (data.getInt("Option") == 0
                            ? "Thanks, boss!" : "Aw, come on... fine.")), false);
                }
            }
        }
        if (right) {
            pay(p, s, t.kind().equals("ledger") ? "The books balance." : t.kind().equals("call") ? "Call put through." : "Good call.");
        } else {
            String why = t.kind().equals("ledger") ? "That's not what it adds up to. The auditors will notice."
                    : t.kind().equals("call") ? "Wrong department. They get transferred around for twenty minutes."
                    : "That's against policy. HR wants a word.";
            p.displayClientMessage(Component.literal("§c✘ " + why), true);
            level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    // ------------------------------------------------------------------ the water cooler

    private static final String[] GOSSIP = {"Did you hear? Gary's been 'working from home' at the Roadhouse.",
            "They're doing layoffs on the fourth floor. Allegedly.", "Somebody keeps stealing my yogurt from the fridge.",
            "The new manager wants us to 'circle back' on everything.", "I think the printer is haunted. I'm serious.",
            "Did you see the game last night?", "The server room is 90 degrees. IT says it's 'fine'.",
            "Becky got promoted. To what, nobody knows.", "Happy hour at the bar after work? First round's on Kevin."};

    static void gossip(ServerPlayer p, BlockPos pos) {
        List<CitizenEntity> near = p.serverLevel().getEntitiesOfClass(CitizenEntity.class, p.getBoundingBox().inflate(8));
        if (near.isEmpty() || p.getRandom().nextInt(2) == 0) return;
        CitizenEntity c = near.get(p.getRandom().nextInt(near.size()));
        p.displayClientMessage(Component.literal("§f<" + c.getName().getString() + "> §7" + GOSSIP[p.getRandom().nextInt(GOSSIP.length)]), false);
    }
}
