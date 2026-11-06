package dev.freerun.alien;

import net.minecraft.world.entity.EntityDimensions;

/**
 * The ten creatures on the Swampwatch's dial. All original bayou wildlife, each with its own
 * shape, passives and one active power (the ability key). Sizes are in blocks.
 */
public enum Alien {
    MOSSBACK("Mossback", "Snapping-turtle bruiser", 0x4F7A3C, 1.3f, 1.7f, 1.45f,
            "Shell: +20 hearts, 12 armour, can't be knocked back", "Slow, but nothing moves it",
            "SHELL SLAM: leap, then slam down. Everything within 5 blocks takes 8 damage and goes flying", 160),
    SKEETER("Skeeter", "Mosquito on the wing", 0x3A3A44, 0.6f, 1.25f, 1.05f,
            "Flight, fast and free", "Fragile: half your hearts", "DRAIN BITE: bite what you're looking at for 6 damage and heal yourself 3 hearts", 60),
    MUDBUG("Mudbug", "Crawfish in armour", 0xA0321E, 1.0f, 1.9f, 1.6f,
            "Hard shell: 14 armour. Pincers hit for double", "Breathes water and swims fast",
            "PINCER SNAP: a snap that hits everything in front for 10 and dazes it for 4 seconds", 80),
    VOLTBELLY("Voltbelly", "Electric eel, upright", 0x1E3A6E, 0.7f, 2.2f, 1.95f,
            "Swims fast, breathes water", "Anything that touches you in water gets shocked",
            "ARC: a lightning arc at what you're looking at, 8 damage, that jumps to two more targets", 70),
    CYPRESS("Cypress", "Walking bald cypress", 0x6A4A2E, 0.9f, 3.0f, 2.7f,
            "Three blocks tall, 8 armour, long reach", "Heals in rain and water",
            "ROOT SNARE: roots burst up and pin everything within 7 blocks for 5 seconds, healing you", 140),
    FOGLET("Foglet", "A patch of bayou fog", 0xC8D8E8, 0.7f, 1.6f, 1.4f,
            "No fall damage, half translucent", "Immune to arrows and bullets",
            "MIST DASH: become fog and pass 8 blocks through anything, even walls one block thick", 50),
    CLOCKROACH("Clockroach", "Factory cockroach", 0x6A3A1A, 0.45f, 0.5f, 0.4f,
            "Tiny: fits through one-block gaps", "Climbs walls, runs at 1.6x, sees in the dark",
            "SCURRY: a 4-second burst of triple speed", 100),
    BAYOU_BULL("Bayou Bull", "Swamp bull, two legs", 0x4A2E1E, 1.4f, 2.3f, 2.0f,
            "+10 hearts, hits for +6, 6 armour", "Can't be knocked back",
            "STAMPEDE: a 1.5-second charge. Everything you hit takes 10 and is thrown aside", 120),
    SLAGWORM("Slagworm", "Worm of cooled furnace slag", 0x2A2426, 1.0f, 2.0f, 1.75f,
            "Immune to fire and lava", "Anything within 2 blocks catches fire",
            "SLAG SPIT: a ball of molten slag", 30),
    GUMBO("Gumbo", "A roux that got up and walked", 0x7A4A1E, 0.9f, 1.4f, 1.1f,
            "Bouncy: no fall damage, jumps 2 blocks", "Sneak to squash through one-block gaps. Hits slow their target",
            "SPLAT: a wave of gumbo that slows and sticks everything within 6 blocks for 6 seconds", 90);

    public final String title, tagline, passive1, passive2, active;
    public final int color, cooldownTicks;
    public final float width, height, eye;

    Alien(String title, String tagline, int color, float width, float height, float eye,
          String passive1, String passive2, String active, int cooldownTicks) {
        this.title = title;
        this.tagline = tagline;
        this.color = color;
        this.width = width;
        this.height = height;
        this.eye = eye;
        this.passive1 = passive1;
        this.passive2 = passive2;
        this.active = active;
        this.cooldownTicks = cooldownTicks;
    }

    public String id() {
        return name().toLowerCase();
    }

    /** The name of the active power, for the HUD. */
    public String activeName() {
        return active.substring(0, active.indexOf(':'));
    }

    public EntityDimensions dimensions(boolean crouching) {
        if (this == GUMBO && crouching) return EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.45f);
        if (crouching && height > 1.5f) return EntityDimensions.scalable(width, height - 0.3f).withEyeHeight(eye - 0.3f);
        return EntityDimensions.scalable(width, height).withEyeHeight(eye);
    }

    public static Alien byOrdinal(int i) {
        return i < 0 || i >= values().length ? null : values()[i];
    }
}
