package dev.freerun.client.chaos;

import dev.freerun.chaos.ChaosRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The game is over. You went for a drink with the Ender Dragon. It was... a lot. */
public class AwkwardCreditsScreen extends Screen {
    private final List<String> lines;
    private float scroll;
    private ChaosClient.Loop music;
    private boolean done;

    AwkwardCreditsScreen(String you) {
        super(Component.literal("The End(?)"));
        lines = List.of(
                "§5§lTHE END", "§8(?)", "", "", "",
                "§7A small diner, just off the Main Island.", "§7Table for two. One of them is forty blocks long.", "", "",
                "§dDRAGON: §fso.", "§b" + you.toUpperCase() + ": §fso.", "", "§dDRAGON: §fyou come here often?",
                "§b" + you.toUpperCase() + ": §fit's my first time in the End.", "§dDRAGON: §fright. right. of course.", "",
                "§8(an Enderman refills the water glasses. nobody makes eye contact.)", "",
                "§dDRAGON: §fthe Wither and I used to come here.", "§b" + you.toUpperCase() + ": §foh.", "§dDRAGON: §fsorry.",
                "§b" + you.toUpperCase() + ": §fno, it's fine.", "§dDRAGON: §fit's not fine. none of this is fine.", "",
                "§8(eleven minutes of silence)", "", "§dDRAGON: §fso what do you do?",
                "§b" + you.toUpperCase() + ": §fI punch trees, mostly.", "§dDRAGON: §f...that's nice. that's a nice hobby.", "",
                "§8(the Enderman brings the bill. it is enormous.)", "", "§dDRAGON: §fshould we split it?",
                "§b" + you.toUpperCase() + ": §fI only brought four emeralds.", "§dDRAGON: §fI'll get it.", "§dDRAGON: §fI always get it.", "",
                "§dDRAGON: §fthis was nice. weird. but nice.", "§dDRAGON: §fdon't call me.", "§dDRAGON: §f...unless you want to.", "", "", "",
                "§6§lSTARRING", "", "§f" + you, "§7as Themselves", "", "§fThe Ender Dragon", "§7as Herself (going through a lot right now)", "",
                "§fThe Wither", "§7mentioned, not present", "", "§fAn Enderman", "§7as The Waiter", "", "",
                "§6§lSPECIAL THANKS", "", "§7the silence", "§7the forty-block table", "§7her therapist", "", "",
                "§8No dragons were harmed in the making of this ending.", "§8Some feelings were.", "", "", "",
                "§5§lTHE END", "§8probably", "", "", "", "§7(press Esc)");
    }

    @Override
    public void tick() {
        if (music == null) {
            music = new ChaosClient.Loop(ChaosRegistry.SAD_PIANO, 0.8f, () -> !done);
            Minecraft.getInstance().getSoundManager().play(music);
            Minecraft.getInstance().getMusicManager().stopPlaying();
        }
        scroll += 0.5f;
    }

    @Override
    public void onClose() {
        done = true;
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        float y = height - (scroll + pt * 0.5f);
        for (String l : lines) {
            if (y > -12 && y < height + 12) g.drawCenteredString(font, l, width / 2, (int) y, 0xFFFFFFFF);
            y += 13;
        }
        if (y < -40) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
