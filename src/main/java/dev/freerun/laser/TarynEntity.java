package dev.freerun.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Taryn, the laser tech. She keeps the laser cutter running, and like Ethan she has a lot of
 * questions, all of them about lasers. One time in ten she says something that's actually right.
 */
public class TarynEntity extends PathfinderMob {
    private static final String NAME = "Taryn";

    static final String[] QUESTIONS = {
            "If I look at the beam with my other eye, does that one count too?",
            "Is the laser hot, or does it just make things hot? Is there a difference?",
            "Why's it called a fiber laser? Is there oatmeal in it?",
            "Do lasers work in the dark? I've only ever run it with the lights on.",
            "If I cut the sheet in half, then cut the halves in half, when do I get to atoms?",
            "Can the laser cut a laser? Like, if you crossed two of them?",
            "Is nitrogen the one in the green tanks or the one that smells like nothing?",
            "Why does the oxygen make sparks? Is the steel scared?",
            "Do you think the cutting head gets dizzy going in circles all day?",
            "If I put a mirror on the bed, where does the beam go? Asking for me.",
            "Is 12 kilowatts a lot? How many toasters is that?",
            "Can it cut boudin? Has anybody checked?",
            "Is the kerf the cut, or the stuff that used to be where the cut is?",
            "Why do we need nanojoints? Do the parts try to escape?",
            "What's the laser's favourite shape? I think it's circles. It does a lot of circles.",
            "Who decides where the focus goes? Does it have a focus? I don't have a focus.",
            "Should the lens be this colour? I don't remember what colour it was.",
            "If the pallet changer swaps the tables, which one is the original table?",
            "Do they make laser safety glasses in prescription? I've been squinting.",
            "Is the laser the light, or the box the light comes out of?",
            "Can I draw a picture of the laser and have the laser cut it out? Is that weird for it?",
            "Ethan asked me what a press brake brakes. I said lasers. Was that right?"
    };

    static final String[] GENIUS = {
            "A fiber laser makes its beam inside a doped glass fiber and sends it down a cable to the cutting head. No mirrors to line up, like the old CO2 machines.",
            "The beam melts the steel; the assist gas is what blows the melt out of the kerf. No gas, no clean cut, just a welded groove.",
            "Oxygen actually burns the steel in an exothermic reaction, so mild steel cuts faster, but you get a dark oxide skin on the edge that has to come off before paint.",
            "Nitrogen is inert, so it doesn't add heat. Slower on mild steel, but the edge comes out bright and oxide-free, ready to weld or powder-coat. That's why stainless gets nitrogen.",
            "Nitrogen cutting runs high pressure, somewhere around 10 to 25 bar. Oxygen cutting is way lower, a few bar, or it over-burns thin sheet.",
            "If you see dross hanging off the bottom edge, the gas pressure's too low and the melt froze before it got blown out. Bump the pressure a bar or two.",
            "Every separate piece needs its own pierce before the cut can start, and piercing eats time and gas. Common-line cutting shares edges so you pierce less.",
            "The kerf is only a few tenths of a millimetre wide, but the CAM software still offsets the toolpath by half of it, or every part comes out undersize.",
            "Nanojoints leave a tiny tab at the bottom of the kerf so parts stay in the skeleton without tipping up into the head. You break them out after.",
            "The cutting head rides a capacitive height sensor, holding the nozzle about a millimetre off the sheet even when the steel's warped."
    };

    private BlockPos home;
    private int talkCooldown = 400;

    public TarynEntity(EntityType<? extends TarynEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal(NAME));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.23);
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, 7);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.02f));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    private String line() {
        return random.nextInt(10) == 0 ? "§e" + GENIUS[random.nextInt(GENIUS.length)] : QUESTIONS[random.nextInt(QUESTIONS.length)];
    }

    private void say(String text) {
        Component msg = Component.literal("§d<" + NAME + "> §f" + text);
        for (Player p : level().players()) if (distanceToSqr(p) < 12 * 12) p.displayClientMessage(msg, false);
        playSound(SoundEvents.VILLAGER_AMBIENT, 0.7f, 1.45f);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            getLookControl().setLookAt(player, 30.0f, 30.0f);
            say(line());
            talkCooldown = 500 + random.nextInt(500);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home != null && !hasRestriction()) restrictTo(home, 7);
        if (--talkCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 6.0);
        if (near != null) {
            getLookControl().setLookAt(near, 30.0f, 30.0f);
            say(line());
            talkCooldown = 700 + random.nextInt(700);
        } else {
            talkCooldown = 100;
        }
    }

    private static TarynEntity near(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(TarynEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(20)).stream().findFirst().orElse(null);
    }

    static void onCutStarted(ServerLevel level, BlockPos pos, ServerPlayer by) {
        TarynEntity t = near(level, pos);
        if (t == null || t.random.nextInt(2) != 0) return;
        String[] lines = {"Ooh, what are we making? Is it a circle? Please be a circle.",
                "Glasses on! ...Wait, are mine on? Can you check?",
                "Did you pick the gas on purpose or did you just press a button? Both are valid.",
                "Okay don't look directly at it. Or do. I've never actually asked anyone."};
        t.getLookControl().setLookAt(by, 30.0f, 30.0f);
        t.say(lines[t.random.nextInt(lines.length)]);
    }

    static void onCutFinished(ServerLevel level, BlockPos pos) {
        TarynEntity t = near(level, pos);
        if (t == null || t.random.nextInt(3) != 0) return;
        String[] lines = {"It's done! I think. The noise stopped, so it's done.",
                "Nice part. Is it supposed to be that shape? I'm not judging, I'm asking.",
                "Don't grab it right away. Is it hot? I never know if it's hot."};
        t.say(lines[t.random.nextInt(lines.length)]);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) setHome(BlockPos.of(tag.getLong("Home")));
    }
}
