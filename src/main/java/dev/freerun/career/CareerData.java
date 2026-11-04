package dev.freerun.career;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** A player's career progress. Saved on the player and kept through death. */
public record CareerData(int career, int level, int xp, List<String> skills) {
    public static final Codec<CareerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("career").forGetter(CareerData::career),
            Codec.INT.fieldOf("level").forGetter(CareerData::level),
            Codec.INT.fieldOf("xp").forGetter(CareerData::xp),
            Codec.STRING.listOf().fieldOf("skills").forGetter(CareerData::skills)
    ).apply(i, CareerData::new));

    public static final int MAX_LEVEL = 20;

    public static CareerData fresh() {
        return new CareerData(0, 1, 0, List.of());
    }

    public Career careerType() {
        return Career.byId(career);
    }

    public boolean has(String id) {
        return skills.contains(id);
    }

    /** One skill point per level; level 1 starts with one to spend. */
    public int points() {
        return level - skills.size();
    }

    /** XP needed to go from {@code level} to the next. */
    public static int xpToNext(int level) {
        return 30 + 15 * level;
    }

    public CareerData withSkill(String id) {
        List<String> list = new ArrayList<>(skills);
        list.add(id);
        return new CareerData(career, level, xp, List.copyOf(list));
    }
}
