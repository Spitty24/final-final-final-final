package dev.freerun.mobs;

import dev.freerun.factory.ForkliftDriverEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Johnny Sim: a balding Cajun forklift driver who, unlike the rest of the forklift crew, you can
 * actually understand. Hand him (and Ethan) some boudin and, well, see what happens.
 */
public class JohnnySimEntity extends ForkliftDriverEntity {
    static final String[] LINES = {
            "Mais, cher, that pallet ain't gonna move itself.",
            "I been driving this forklift since before they paved the parking lot.",
            "You hungry? They got boudin at the Double Quick, I guarantee.",
            "Watch your toes, now. These forks don't care whose feet they are.",
            "Ethan up on that crane again? Lord have mercy.",
            "My wife packed me a sandwich. I'm gonna eat boudin anyway.",
            "Pass a good time, but not on my forks, no.",
            "Boudreaux talks so fast even I can't understand him, and I'm from Breaux Bridge.",
            "Back in my day we carried steel on our backs. Now look at us. Spoiled.",
            "You want to make groceries after shift? Crawfish are on sale.",
            "Lost all my hair worrying about these co-ops.",
            "Hoo, it's hot in here. Hotter than a crawfish boil in August.",
            "Don't let Slater see you standing around. He'll write you up for breathing.",
            "Every morning I tell myself, Johnny, today you're gonna retire. Then I come in anyway.",
            "This forklift and me, we got an understanding. She don't tip, I don't speed."
    };

    private int loveTicks;
    private int breedCooldown;

    public JohnnySimEntity(EntityType<? extends ForkliftDriverEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void setNameIndex(int i) {
        super.setNameIndex(i);
        setCustomName(Component.literal("Johnny Sim §7(Forklift)"));
    }

    @Override
    protected String speaker() {
        return "Johnny Sim";
    }

    @Override
    protected String line() {
        return LINES[random.nextInt(LINES.length)];
    }

    @Override
    protected boolean speaksGibberish() {
        return false;
    }

    @Override
    protected void fedBoudin(Player player) {
        if (breedCooldown > 0) return;
        loveTicks = 600;
        say(null, "You know who else likes boudin? That tall fella. Ethan. ...Why'd I say that?");
    }

    public boolean inLove() {
        return loveTicks > 0;
    }

    public void bred() {
        loveTicks = 0;
        breedCooldown = 6000;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (breedCooldown > 0) breedCooldown--;
        if (loveTicks > 0) {
            loveTicks--;
            if (loveTicks % 10 == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.0, getZ(), 1, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BreedCooldown", breedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breedCooldown = tag.getInt("BreedCooldown");
    }
}
