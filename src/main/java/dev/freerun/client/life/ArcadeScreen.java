package dev.freerun.client.life;

import dev.freerun.life.Arcade;
import dev.freerun.life.LifePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The arcade cabinet's screen. Every game runs here, on the client, at 20 steps a second; the
 * final score goes to the server, which pays out tickets. Arrow keys or WASD; Space to flap or
 * throw.
 */
public class ArcadeScreen extends Screen {
    private static final int GW = 200, GH = 150;   // the playfield, in screen pixels
    private final CompoundTag data;
    private final int game;
    private final RandomSource r = RandomSource.create();
    private int left, top;
    private int score;
    private boolean over, sent, started;
    private int t;
    private boolean leftKey, rightKey;

    // Gator Chomp
    private static final int CELL = 10, CW = GW / CELL, CH = GH / CELL;
    private final Deque<int[]> snake = new ArrayDeque<>();
    private int dir = 1, nextDir = 1, foodX, foodY, grow;
    // Brick Bayou
    private float px = GW / 2f, bx, by, bvx, bvy;
    private final boolean[][] bricks = new boolean[6][10];
    private int lives = 3;
    // Pelican Flap
    private float fy = GH / 2f, fvy;
    private final List<float[]> pipes = new ArrayList<>();
    // Darts
    private int dart;
    private final List<float[]> darts = new ArrayList<>();

    public ArcadeScreen(CompoundTag data) {
        super(Component.literal(Arcade.NAMES[Mth.clamp(data.getInt("Game"), 0, 3)]));
        this.data = data;
        this.game = Mth.clamp(data.getInt("Game"), 0, 3);
        reset();
    }

    private void reset() {
        switch (game) {
            case Arcade.CHOMP -> {
                snake.clear();
                for (int i = 0; i < 4; i++) snake.addLast(new int[]{CW / 2 - i, CH / 2});
                placeFood();
            }
            case Arcade.BRICKS -> {
                for (boolean[] row : bricks) java.util.Arrays.fill(row, true);
                serve();
            }
            case Arcade.FLAP -> pipes.add(new float[]{GW + 20, 40 + r.nextInt(GH - 80)});
            default -> {}
        }
    }

    @Override
    protected void init() {
        left = (width - GW) / 2;
        top = (height - GH) / 2;
    }

    private void sound(SoundEvent e, float pitch) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, 0.5f));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(key, scan, mods);
        started = true;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> {
                leftKey = true;
                if (dir != 1) nextDir = 3;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> {
                rightKey = true;
                if (dir != 3) nextDir = 1;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> {
                if (dir != 2) nextDir = 0;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> {
                if (dir != 0) nextDir = 2;
            }
            case GLFW.GLFW_KEY_SPACE -> {
                if (over) {
                    onClose();
                } else if (game == Arcade.FLAP) {
                    fvy = -3.2f;
                    sound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.6f);
                } else if (game == Arcade.DARTS) {
                    throwDart();
                }
            }
            default -> {}
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) leftKey = false;
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) rightKey = false;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        started = true;
        if (over) onClose();
        else if (game == Arcade.FLAP) fvy = -3.2f;
        else if (game == Arcade.DARTS) throwDart();
        return true;
    }

    // ------------------------------------------------------------------ the games

    @Override
    public void tick() {
        if (over || !started) return;
        t++;
        switch (game) {
            case Arcade.CHOMP -> tickChomp();
            case Arcade.BRICKS -> tickBricks();
            case Arcade.FLAP -> tickFlap();
            default -> {
                if (dart >= 3 && t > 30) end();
            }
        }
    }

    private void placeFood() {
        for (int k = 0; k < 200; k++) {
            int x = r.nextInt(CW), y = r.nextInt(CH);
            boolean on = false;
            for (int[] s : snake) if (s[0] == x && s[1] == y) on = true;
            if (!on) {
                foodX = x;
                foodY = y;
                return;
            }
        }
    }

    private void tickChomp() {
        int speed = Math.max(1, 3 - score / 40);
        if (t % speed != 0) return;
        dir = nextDir;
        int[] head = snake.peekFirst();
        int nx = head[0] + (dir == 1 ? 1 : dir == 3 ? -1 : 0), ny = head[1] + (dir == 2 ? 1 : dir == 0 ? -1 : 0);
        if (nx < 0 || ny < 0 || nx >= CW || ny >= CH) {
            end();
            return;
        }
        for (int[] s : snake) {
            if (s[0] == nx && s[1] == ny) {
                end();
                return;
            }
        }
        snake.addFirst(new int[]{nx, ny});
        if (nx == foodX && ny == foodY) {
            score += 5;
            grow += 2;
            placeFood();
            sound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.8f);
        }
        if (grow > 0) grow--;
        else snake.removeLast();
    }

    private void serve() {
        bx = px;
        by = GH - 30;
        bvx = (r.nextBoolean() ? 1 : -1) * 1.8f;
        bvy = -2.4f;
    }

    private void tickBricks() {
        if (leftKey) px -= 5;
        if (rightKey) px += 5;
        px = Mth.clamp(px, 18, GW - 18);
        for (int step = 0; step < 2; step++) {
            bx += bvx;
            by += bvy;
            if (bx < 2 || bx > GW - 2) {
                bvx = -bvx;
                bx = Mth.clamp(bx, 2, GW - 2);
            }
            if (by < 2) {
                bvy = -bvy;
                by = 2;
            }
            // paddle
            if (bvy > 0 && by >= GH - 14 && by <= GH - 9 && Math.abs(bx - px) < 18) {
                bvy = -Math.abs(bvy);
                bvx = Mth.clamp((bx - px) / 6f, -3.2f, 3.2f);
                sound(SoundEvents.NOTE_BLOCK_HAT.value(), 1.4f);
            }
            // bricks: 10 x 6, each 20 x 8, starting at y 16
            int col = (int) (bx / 20), row = (int) ((by - 16) / 8);
            if (row >= 0 && row < 6 && col >= 0 && col < 10 && bricks[row][col]) {
                bricks[row][col] = false;
                bvy = -bvy;
                score += 10;
                sound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.8f + row * 0.15f);
                boolean any = false;
                for (boolean[] rr : bricks) for (boolean b : rr) any |= b;
                if (!any) {   // board cleared: a fresh wall, a little faster
                    for (boolean[] rr : bricks) java.util.Arrays.fill(rr, true);
                    bvy *= 1.15f;
                    score += 50;
                }
            }
            if (by > GH) {
                if (--lives <= 0) {
                    end();
                    return;
                }
                sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f);
                serve();
            }
        }
    }

    private void tickFlap() {
        fvy += 0.32f;
        fy += fvy;
        for (float[] p : pipes) p[0] -= 2.2f;
        if (pipes.get(pipes.size() - 1)[0] < GW - 70) pipes.add(new float[]{GW + 10, 30 + r.nextInt(GH - 60)});
        if (pipes.get(0)[0] < -20) pipes.remove(0);
        float x = 50;
        for (float[] p : pipes) {
            boolean inX = x + 6 > p[0] && x - 6 < p[0] + 16;
            if (inX && (fy - 5 < p[1] - 22 || fy + 5 > p[1] + 22)) {
                end();
                return;
            }
            if (p[0] + 16 < x - 6 && p[0] + 16 >= x - 8.2f) {
                score++;
                sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f);
            }
        }
        if (fy > GH || fy < 0) end();
    }

    private float aimX() {
        return GW / 2f + Mth.sin(t * 0.13f) * 44 + Mth.sin(t * 0.31f) * 14;
    }

    private float aimY() {
        return GH / 2f + Mth.cos(t * 0.17f) * 40 + Mth.sin(t * 0.23f) * 10;
    }

    private void throwDart() {
        if (dart >= 3) return;
        float x = aimX(), y = aimY();
        float d = Mth.sqrt((x - GW / 2f) * (x - GW / 2f) + (y - GH / 2f) * (y - GH / 2f));
        int pts = d < 4 ? 50 : d < 9 ? 25 : d < 20 ? 20 : d < 32 ? 15 : d < 44 ? 10 : d < 56 ? 5 : 0;
        score += pts;
        darts.add(new float[]{x, y, pts});
        dart++;
        t = dart >= 3 ? 1 : t;
        sound(pts >= 25 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.WOOD_HIT, pts >= 25 ? 1.6f : 1.2f);
    }

    private void end() {
        if (over) return;
        over = true;
        sound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 0.8f);
        send();
    }

    private void send() {
        if (sent) return;
        sent = true;
        CompoundTag tag = new CompoundTag();
        tag.putInt("Token", data.getInt("Token"));
        tag.putInt("Score", score);
        if (ClientPlayNetworking.canSend(LifePayloads.Action.TYPE)) ClientPlayNetworking.send(new LifePayloads.Action(LifePayloads.ARCADE, tag));
    }

    @Override
    public void removed() {
        send();   // walking away still counts what you scored
        super.removed();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int[] bezel = {0xFF1A5C2A, 0xFF2A3A8A, 0xFF8A4A1A, 0xFF5A1A2A};
        g.fill(left - 10, top - 26, left + GW + 10, top + GH + 22, bezel[game]);
        g.fill(left, top, left + GW, top + GH, 0xFF05070A);
        g.drawCenteredString(font, "§l" + Arcade.NAMES[game].toUpperCase(), left + GW / 2, top - 20, 0xFFE040);
        g.drawString(font, "SCORE " + score, left, top - 10, 0xFFFFFF);
        String best = "HI " + data.getInt("Best") + (data.getString("BestName").isEmpty() ? "" : " " + data.getString("BestName"));
        g.drawString(font, best, left + GW - font.width(best), top - 10, 0xFFAA00);
        g.enableScissor(left, top, left + GW, top + GH);
        switch (game) {
            case Arcade.CHOMP -> drawChomp(g);
            case Arcade.BRICKS -> drawBricks(g);
            case Arcade.FLAP -> drawFlap(g);
            default -> drawDarts(g);
        }
        g.disableScissor();
        String help = switch (game) {
            case Arcade.CHOMP -> "Arrows/WASD to steer";
            case Arcade.BRICKS -> "←/→ or A/D to move · " + lives + " balls left";
            case Arcade.FLAP -> "Space or click to flap";
            default -> "Space or click to throw · dart " + Math.min(3, dart + 1) + " of 3";
        };
        g.drawCenteredString(font, "§7" + help, left + GW / 2, top + GH + 6, 0xFFFFFF);
        if (!started) {
            g.fill(left, top + GH / 2 - 14, left + GW, top + GH / 2 + 14, 0xC0000000);
            g.drawCenteredString(font, "§ePress any key to start", left + GW / 2, top + GH / 2 - 4, 0xFFFFFF);
        }
        if (over) {
            g.fill(left, top + GH / 2 - 20, left + GW, top + GH / 2 + 20, 0xD0000000);
            g.drawCenteredString(font, "§c§lGAME OVER", left + GW / 2, top + GH / 2 - 14, 0xFFFFFF);
            g.drawCenteredString(font, "§fScore " + score + " §7· Space to collect your tickets", left + GW / 2, top + GH / 2 + 2, 0xFFFFFF);
        }
    }

    private void drawChomp(GuiGraphics g) {
        for (int x = 0; x < CW; x++) for (int y = 0; y < CH; y++) {
            if ((x + y) % 2 == 0) g.fill(left + x * CELL, top + y * CELL, left + x * CELL + CELL, top + y * CELL + CELL, 0xFF0A2A12);
        }
        g.fill(left + foodX * CELL + 2, top + foodY * CELL + 3, left + foodX * CELL + 9, top + foodY * CELL + 7, 0xFF60A0FF);   // a fish
        g.fill(left + foodX * CELL, top + foodY * CELL + 2, left + foodX * CELL + 2, top + foodY * CELL + 8, 0xFF4080E0);
        boolean first = true;
        for (int[] s : snake) {
            int x = left + s[0] * CELL, y = top + s[1] * CELL;
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, first ? 0xFF3CB043 : 0xFF2E8B37);
            if (first) {
                g.fill(x + 2, y + 2, x + 4, y + 4, 0xFFFFFF60);
                g.fill(x + 6, y + 2, x + 8, y + 4, 0xFFFFFF60);
            }
            first = false;
        }
    }

    private void drawBricks(GuiGraphics g) {
        int[] colors = {0xFFE04040, 0xFFE08A30, 0xFFE0D040, 0xFF40C040, 0xFF4080E0, 0xFF9050D0};
        for (int row = 0; row < 6; row++) for (int col = 0; col < 10; col++) {
            if (!bricks[row][col]) continue;
            int x = left + col * 20, y = top + 16 + row * 8;
            g.fill(x + 1, y + 1, x + 19, y + 7, colors[row]);
        }
        g.fill(left + (int) px - 18, top + GH - 12, left + (int) px + 18, top + GH - 8, 0xFFE0E0E0);
        g.fill(left + (int) bx - 2, top + (int) by - 2, left + (int) bx + 2, top + (int) by + 2, 0xFFFFFFFF);
    }

    private void drawFlap(GuiGraphics g) {
        g.fill(left, top, left + GW, top + GH, 0xFF3A78B8);
        g.fill(left, top + GH - 12, left + GW, top + GH, 0xFF2A5A3A);
        for (float[] p : pipes) {
            int x = left + (int) p[0];
            g.fill(x, top, x + 16, top + (int) p[1] - 22, 0xFF6A4A2A);
            g.fill(x, top + (int) p[1] + 22, x + 16, top + GH, 0xFF6A4A2A);
            g.fill(x - 1, top + (int) p[1] - 26, x + 17, top + (int) p[1] - 22, 0xFF4A3218);
            g.fill(x - 1, top + (int) p[1] + 22, x + 17, top + (int) p[1] + 26, 0xFF4A3218);
        }
        int x = left + 50, y = top + (int) fy;
        g.fill(x - 6, y - 4, x + 5, y + 4, 0xFFF4F4F4);       // pelican
        g.fill(x + 3, y - 1, x + 10, y + 2, 0xFFF0B020);      // the beak
        g.fill(x - 4, y - (t / 3 % 2 == 0 ? 7 : 1), x + 2, y, 0xFFDADADA);   // flapping wing
        g.fill(x + 1, y - 3, x + 3, y - 1, 0xFF000000);
    }

    private void drawDarts(GuiGraphics g) {
        int cx = left + GW / 2, cy = top + GH / 2;
        int[][] rings = {{56, 0xFF202020}, {44, 0xFFE8E0C8}, {32, 0xFF1A7A2A}, {20, 0xFFC02020}, {9, 0xFF1A7A2A}, {4, 0xFFC02020}};
        for (int[] ring : rings) {
            int rad = ring[0];
            for (int dy = -rad; dy <= rad; dy++) {
                int half = (int) Math.sqrt(rad * rad - dy * dy);
                g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, ring[1]);
            }
        }
        for (float[] d : darts) {
            int x = left + (int) d[0], y = top + (int) d[1];
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFD000);
            g.drawString(font, "+" + (int) d[2], x + 4, y - 4, 0xFFFFFF);
        }
        if (dart < 3) {
            int x = left + (int) aimX(), y = top + (int) aimY();
            g.fill(x - 6, y, x + 7, y + 1, 0xFFFFFFFF);
            g.fill(x, y - 6, x + 1, y + 7, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
