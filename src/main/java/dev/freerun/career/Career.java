package dev.freerun.career;

import net.minecraft.util.Mth;

/** The four career paths (plus "not picked yet"). New careers must be appended: the ordinal is saved. */
public enum Career {
    NONE("Unassigned", "", 0xAAAAAA),
    EMPLOYEE("Employee", "Clock in, do the real work, go home. Hands-on and hard to tire out.", 0x4FA3E0),
    SUPERVISOR("Supervisor", "Keep the floor moving and the crew safe. Tough, sharp-eyed, and sick and tired.", 0xE0B030),
    SUPERINTENDENT("Superintendent", "Run the whole site: the machines, the schedule, the big picture.", 0x50C070),
    UPPER_MANAGEMENT("Upper Management", "Meetings, bonuses and a corner office. The perks are excellent.", 0xC060E0);

    public final String title;
    public final String tagline;
    public final int color;

    Career(String title, String tagline, int color) {
        this.title = title;
        this.tagline = tagline;
        this.color = color;
    }

    public static Career byId(int id) {
        Career[] all = values();
        return all[Mth.clamp(id, 0, all.length - 1)];
    }
}
