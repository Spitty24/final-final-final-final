package dev.freerun.life;

import dev.freerun.economy.Money;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The arcade and the bar games. Each cabinet costs $1 a play and runs a real game on your screen:
 *
 *   0 Gator Chomp   steer the gator, eat the fish, don't bite your own tail      (snake)
 *   1 Brick Bayou   keep the ball up with the paddle and clear the bricks        (breakout)
 *   2 Pelican Flap  flap through the gaps between the pilings                    (flappy)
 *   3 Darts         stop the swinging aim on the bullseye, three darts a round   (at the bar)
 *
 * Scores pay out in arcade tickets, which the prize counter swaps for prizes. Each game keeps a
 * city-wide high score. The claw machine is $2 and, as claw machines go, fairly honest.
 */
public final class Arcade {
    public static final int CHOMP = 0, BRICKS = 1, FLAP = 2, DARTS = 3;
    public static final String[] NAMES = {"Gator Chomp", "Brick Bayou", "Pelican Flap", "Darts"};

    private record Game(BlockPos pos, int game, int token, long started) {}

    private static final Map<UUID, Game> PLAYING = new HashMap<>();

    public static final class Scores extends SavedData {
        final int[] best = new int[NAMES.length];
        final String[] who = {"", "", "", ""};

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putIntArray("Best", best);
            for (int i = 0; i < who.length; i++) tag.putString("Who" + i, who[i]);
            return tag;
        }

        static Scores load(CompoundTag tag, HolderLookup.Provider registries) {
            Scores s = new Scores();
            int[] b = tag.getIntArray("Best");
            System.arraycopy(b, 0, s.best, 0, Math.min(b.length, s.best.length));
            for (int i = 0; i < s.who.length; i++) s.who[i] = tag.getString("Who" + i);
            return s;
        }
    }

    private static final SavedData.Factory<Scores> FACTORY = new SavedData.Factory<>(Scores::new, Scores::load,
            net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private Arcade() {}

    static Scores scores(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "freerun_arcade");
    }

    public static void register() {
    }

    static void open(ServerPlayer p, BlockPos pos, int game) {
        if (!Money.pay(p, 1)) {
            p.displayClientMessage(Component.literal("§cInsert $1 to play. §7(You're broke.)"), true);
            return;
        }
        int token = p.getRandom().nextInt(Integer.MAX_VALUE);
        PLAYING.put(p.getUUID(), new Game(pos.immutable(), game, token, p.serverLevel().getGameTime()));
        Scores s = scores(p.serverLevel());
        CompoundTag t = new CompoundTag();
        t.putInt("Game", game);
        t.putInt("Token", token);
        t.putInt("Best", s.best[game]);
        t.putString("BestName", s.who[game]);
        t.putLong("Pos", pos.asLong());
        LifePayloads.open(p, LifePayloads.ARCADE, t);
        p.serverLevel().playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    /** The most a real game of this length could score (anything above is a cheat or a bug). */
    private static int plausible(int game, double seconds) {
        return switch (game) {
            case CHOMP -> (int) (seconds * 3) + 5;
            case BRICKS -> (int) (seconds * 12) + 20;
            case FLAP -> (int) (seconds * 1.2) + 2;
            default -> 180;
        };
    }

    private static int tickets(int game, int score) {
        int t = switch (game) {
            case CHOMP -> score / 2;
            case BRICKS -> score / 10;
            case FLAP -> score;
            default -> score / 15;
        };
        return Math.max(1, Math.min(60, t));
    }

    static void result(ServerPlayer p, CompoundTag data) {
        Game g = PLAYING.remove(p.getUUID());
        if (g == null || g.token() != data.getInt("Token")) return;
        int score = Math.max(0, data.getInt("Score"));
        double seconds = (p.serverLevel().getGameTime() - g.started()) / 20.0;
        score = Math.min(score, plausible(g.game(), seconds));
        int tix = tickets(g.game(), score);
        ItemStack stack = new ItemStack(LifeRegistry.TICKET, tix);
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        Scores s = scores(p.serverLevel());
        String line = "§d🕹 " + NAMES[g.game()] + ": §f" + score + " points §7→ §e" + tix + " ticket" + (tix == 1 ? "" : "s");
        if (score > s.best[g.game()]) {
            s.best[g.game()] = score;
            s.who[g.game()] = p.getName().getString();
            s.setDirty();
            line += " §6§lNEW HIGH SCORE!";
            p.serverLevel().playSound(null, g.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.4f);
            for (ServerPlayer o : p.serverLevel().players()) {
                if (o != p && o.distanceToSqr(p) < 48 * 48) {
                    o.displayClientMessage(Component.literal("§d🕹 " + p.getName().getString() + " just set the " + NAMES[g.game()] + " high score: " + score), false);
                }
            }
        }
        p.displayClientMessage(Component.literal(line), false);
        dev.freerun.career.Careers.addXp(p, 1, "arcade");
    }

    static void claw(ServerPlayer p, BlockPos pos) {
        if (!Money.pay(p, 2)) {
            p.displayClientMessage(Component.literal("§c$2 a try. §7(You're broke.)"), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.4f);
        if (p.getRandom().nextInt(4) == 0) {
            Item[] prizes = {LifeRegistry.GATOR_PLUSH, LifeRegistry.PELICAN_PLUSH, LifeRegistry.RUBBER_DUCK};
            ItemStack prize = new ItemStack(prizes[p.getRandom().nextInt(prizes.length)]);
            if (!p.getInventory().add(prize)) p.drop(prize, false);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.8f);
            p.displayClientMessage(Component.literal("§a🧸 The claw grabs a " + prize.getHoverName().getString() + " and actually holds on! It's yours."), false);
        } else {
            String[] miss = {"The claw grabs it... and lets go at the top. Of course.", "Missed by a mile.",
                    "The claw brushes it gently, like a caress. Nothing.", "So close! (It was not close.)"};
            p.displayClientMessage(Component.literal("§7🧸 " + miss[p.getRandom().nextInt(miss.length)]), true);
        }
    }
}
