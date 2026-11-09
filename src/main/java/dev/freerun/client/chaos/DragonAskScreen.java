package dev.freerun.client.chaos;

import dev.freerun.chaos.ChaosPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Halfway through the fight, the Ender Dragon asks you something. */
public class DragonAskScreen extends Screen {
    private boolean decided;

    DragonAskScreen() {
        super(Component.literal("The Ender Dragon"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("§dSure. Let's grab a drink."), b -> decide(true))
                .bounds(width / 2 - 154, height / 2 + 30, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§cNo. We fight."), b -> decide(false))
                .bounds(width / 2 + 4, height / 2 + 30, 150, 20).build());
    }

    private void decide(boolean yes) {
        if (decided) return;
        decided = true;
        ChaosClient.send(ChaosPayloads.DRAGON_ANSWER, 0, yes ? 1 : 0);
        onClose();
    }

    @Override
    public void onClose() {
        if (!decided) {
            decided = true;
            ChaosClient.send(ChaosPayloads.DRAGON_ANSWER, 0, 0);
        }
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90100818);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, "§5§lThe Ender Dragon", width / 2, height / 2 - 40, 0xFFFFFFFF);
        g.drawCenteredString(font, "§d§o\"...do you want to just grab a drink instead?\"", width / 2, height / 2 - 22, 0xFFFFFFFF);
        g.drawCenteredString(font, "§8§o\"I know a place. It's quiet. Nobody's trying to kill anybody.\"", width / 2, height / 2 - 10, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
