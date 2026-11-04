package dev.freerun.career;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's career progress. Saved on the player and kept through death.
 *
 * @param completed bitmask of careers finished through prestige (bit = career ordinal). Skills
 *                  from completed careers stay owned and keep working forever.
 */
public record CareerData(int career, int level, int xp, List<String> skills, int completed) {
    public static final Codec<CareerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("career").forGetter(CareerData::career),
            Codec.INT.fieldOf("level").forGetter(CareerData::level),
            Codec.INT.fieldOf("xp").forGetter(CareerData::xp),
            Codec.STRING.listOf().fieldOf("skills").forGetter(CareerData::skills),
            Codec.INT.optionalFieldOf("completed", 0).forGetter(CareerData::completed)
    ).apply(i, CareerData::new));

    public static final int MAX_LEVEL = 20;

    public static CareerData fresh() {
        return new CareerData(0, 1, 0, List.of(), 0);
    }

    public Career careerType() {
        return Career.byId(career);
    }

    public boolean has(String id) {
        return skills.contains(id);
    }

    public boolean isCompleted(Career c) {
        return (completed & (1 << c.ordinal())) != 0;
    }

    public int prestigeCount() {
        return Integer.bitCount(completed);
    }

    /** Skills owned in the career you're working on now. */
    public int currentSkillCount() {
        int n = 0;
        for (String id : skills) {
            Skill s = Skills.get(id);
            if (s != null && s.career() == careerType()) n++;
        }
        return n;
    }

    /** One skill point per level in the current career; level 1 starts with one to spend. */
    public int points() {
        return level - currentSkillCount();
    }

    /** XP needed to go from {@code level} to the next. */
    public static int xpToNext(int level) {
        return 30 + 15 * level;
    }

    public CareerData withSkill(String id) {
        List<String> list = new ArrayList<>(skills);
        list.add(id);
        return new CareerData(career, level, xp, List.copyOf(list), completed);
    }

    /** Every skill in the current tree is unlocked. */
    public boolean treeComplete() {
        Career c = careerType();
        if (c == Career.NONE) return false;
        for (Skill s : Skills.forCareer(c)) if (!has(s.id())) return false;
        return true;
    }

    /** The career a promotion leads to: the next one in order that you haven't finished yet. */
    public Career nextCareer() {
        Career[] all = Career.values();
        int start = Math.max(1, career);
        for (int step = 1; step < all.length; step++) {
            int idx = 1 + Math.floorMod(start - 1 + step, all.length - 1);
            Career c = all[idx];
            if (c != careerType() && !isCompleted(c)) return c;
        }
        return Career.NONE;
    }

    public boolean canPrestige() {
        return treeComplete() && nextCareer() != Career.NONE;
    }
}
