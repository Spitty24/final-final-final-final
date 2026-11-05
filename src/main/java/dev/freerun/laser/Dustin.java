package dev.freerun.laser;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Dustin, your manager, on the work phone's Teams. Every 1 to 15 minutes he messages you to
 * question your intelligence, then apologises because he's working on himself. Then he does it again.
 */
public final class Dustin {
    private Dustin() {}

    public static final String NAME = "Dustin";
    private static final int MAX = 40;

    static final String[] INSULTS = {
            "Did you seriously put the sheet on the bed upside down? Steel doesn't HAVE an upside down. How did you manage that.",
            "I've seen the co-ops figure things out faster and they're still looking for the bathroom.",
            "Quick question: do you think before you do things, or is it more of a surprise for everyone?",
            "Per my last message, which you clearly didn't read, because reading is hard for you.",
            "Your timesheet says you worked 9 hours. The building says you were in the break room for 6 of them.",
            "I asked for a bracket. You brought me a feeling.",
            "Can you hop on a quick call? I want to hear you explain that in your own words so I can understand how you think. Or don't.",
            "Ethan asked me if you were new. You've been here two years.",
            "I'm putting you on the safety committee so you can learn what safety is.",
            "Every time you touch the press brake I lose a year off my life and the press brake loses two.",
            "Please stop replying-all. I'm begging you. The whole plant knows about your sandwich now.",
            "Taryn says you asked her if the laser was 'on'. It's a laser. Nobody can tell. That's the point. Read the screen.",
            "I've attached a PDF titled 'How To Do Your Job'. It's one page. I made the font big for you.",
            "I don't know how you're still employed, and I'm the one who'd have to fire you.",
            "Thanks for the update! Next time, an update that's true would be great.",
            "Your weld looks like a pigeon sat on it. Redo it.",
            "Who taught you to drive a forklift? Tell me so I can have them retrained too.",
            "Saw your parking job. Bold choice. Wrong, but bold.",
            "I've had more productive meetings with the vending machine.",
            "Good morning! Just a reminder that you are, statistically, my biggest problem."
    };

    static final String[] SORRY = {
            "Sorry. That was out of line. I'm working on myself.",
            "Hey, I apologize for that last message. I'm working on myself.",
            "That came out wrong. My therapist says I need to pause before I hit send. I'm working on myself.",
            "Sorry. I'm working on myself. It's a journey.",
            "I shouldn't have said that. I'm working on myself, and honestly so should you.",
            "My bad. Working on myself. We're a family here."
    };

    static final String[] RELAPSE = {
            "Anyway.",
            "Moving on, the parts you cut yesterday are all wrong.",
            "...but seriously, fix it.",
            "Okay, that said, where are you right now? Why aren't you at your station?",
            "Growth isn't linear. Neither is your work."
    };

    /** Your canned replies, and what each gets back. */
    public static final String[] REPLIES = {"(thumbs up)", "On it!", "Sorry, won't happen again.", "That's not fair.", "Who is this?"};
    static final String[][] COMEBACKS = {
            {"A thumbs up. Wow. Shakespeare.", "Don't thumbs-up me.", "Was that sarcasm? You're not smart enough for sarcasm."},
            {"You said that last time.", "\"On it\" like you were \"on it\" Tuesday?", "Great. Be on it faster."},
            {"It will.", "It's happened four times this week.", "Apology noted. Filed. Ignored."},
            {"Life's not fair. Neither is your performance review.", "Fair would be firing you.", "Let's take this offline. Forever."},
            {"Dustin. Your manager. For three years.", "This is exactly what I'm talking about.", "I can see you've read it. Teams tells me."}
    };

    // ------------------------------------------------------------------ the phone's data

    static CompoundTag data(ItemStack phone) {
        CustomData d = phone.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    static void save(ItemStack phone, CompoundTag t) {
        phone.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    static void post(CompoundTag t, boolean fromDustin, String text, long now) {
        ListTag chat = t.getList("Chat", Tag.TAG_COMPOUND);
        CompoundTag m = new CompoundTag();
        m.putBoolean("Dustin", fromDustin);
        m.putString("Text", text);
        m.putLong("Time", now);
        chat.add(m);
        while (chat.size() > MAX) chat.remove(0);
        t.put("Chat", chat);
        if (fromDustin) t.putInt("Unread", t.getInt("Unread") + 1);
    }

    private static void notify(ServerPlayer p, String text) {
        CompoundTag n = new CompoundTag();
        n.putString("From", NAME);
        n.putString("Text", text);
        dev.freerun.life.LifePayloads.open(p, dev.freerun.life.LifePayloads.TEAMS_PING, n);
    }

    /** Called every second from the work phone in a player's inventory. */
    static void tick(ServerPlayer p, ItemStack phone) {
        long now = p.serverLevel().getGameTime();
        RandomSource r = p.getRandom();
        CompoundTag t = data(phone);
        boolean changed = false;
        if (!t.contains("Next")) {
            t.putLong("Next", now + 1200 + r.nextInt(18000 - 1200));
            changed = true;
        }
        // queued follow-ups (the apology, then the relapse, or his answer to your reply)
        ListTag queue = t.getList("Queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < queue.size(); i++) {
            CompoundTag q = queue.getCompound(i);
            if (q.getLong("At") > now) continue;
            post(t, true, q.getString("Text"), now);
            notify(p, q.getString("Text"));
            queue.remove(i--);
            changed = true;
        }
        t.put("Queue", queue);
        if (now >= t.getLong("Next")) {
            String insult = INSULTS[r.nextInt(INSULTS.length)];
            post(t, true, insult, now);
            notify(p, insult);
            queue(t, SORRY[r.nextInt(SORRY.length)], now + 100 + r.nextInt(200));
            if (r.nextInt(3) == 0) queue(t, RELAPSE[r.nextInt(RELAPSE.length)], now + 500 + r.nextInt(400));
            t.putLong("Next", now + 1200 + r.nextInt(18000 - 1200));   // 1 to 15 minutes
            changed = true;
        }
        if (changed) save(phone, t);
    }

    private static void queue(CompoundTag t, String text, long at) {
        ListTag queue = t.getList("Queue", Tag.TAG_COMPOUND);
        CompoundTag q = new CompoundTag();
        q.putString("Text", text);
        q.putLong("At", at);
        queue.add(q);
        t.put("Queue", queue);
    }

    /** The player tapped a quick reply (or opened the chat, which marks it read). */
    public static void action(ServerPlayer p, CompoundTag data) {
        ItemStack phone = WorkPhoneItem.find(p);
        if (phone.isEmpty()) return;
        long now = p.serverLevel().getGameTime();
        CompoundTag t = data(phone);
        if (data.getBoolean("Read")) {
            t.putInt("Unread", 0);
            save(phone, t);
            return;
        }
        int idx = data.getInt("Reply");
        if (idx < 0 || idx >= REPLIES.length) return;
        post(t, false, REPLIES[idx], now);
        RandomSource r = p.getRandom();
        String[] back = COMEBACKS[idx];
        queue(t, back[r.nextInt(back.length)], now + 40 + r.nextInt(60));
        if (r.nextInt(2) == 0) queue(t, SORRY[r.nextInt(SORRY.length)], now + 200 + r.nextInt(160));
        t.putInt("Unread", 0);
        save(phone, t);
    }
}
