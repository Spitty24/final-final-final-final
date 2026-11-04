package dev.freerun.gang;

import net.minecraft.util.Mth;

/** The two (made-up) crews fighting over the factory parking lot. */
public enum Crew {
    RUST_RATS("Rust Rat", "§6", new String[]{
            "This lot belongs to the Rats!",
            "Chrome Kings? More like Chrome Things.",
            "Hey! You parked in OUR spot!",
            "Rats run this asphalt.",
            "Go polish your hubcaps, Kings."
    }),
    CHROME_KINGS("Chrome King", "§5", new String[]{
            "Kings run this lot!",
            "Back to your dumpster, Rats!",
            "Nice rust bucket. Did it come with the tetanus?",
            "Every spot here is a Kings spot.",
            "You hear something squeaking? Oh, it's the Rats."
    });

    public final String member;
    public final String color;
    public final String[] taunts;

    Crew(String member, String color, String[] taunts) {
        this.member = member;
        this.color = color;
        this.taunts = taunts;
    }

    public Crew rival() {
        return this == RUST_RATS ? CHROME_KINGS : RUST_RATS;
    }

    public static Crew byId(int id) {
        Crew[] all = values();
        return all[Mth.clamp(id, 0, all.length - 1)];
    }
}
