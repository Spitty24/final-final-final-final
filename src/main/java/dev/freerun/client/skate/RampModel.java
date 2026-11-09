package dev.freerun.client.skate;

import dev.freerun.Freerun;
import dev.freerun.skate.RampProfiles;
import dev.freerun.skate.SkateRampBlock;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Draws a skate ramp block as a real smooth surface: the curve is cut into thin strips, each a
 * sloped quad, with matching side profiles, so quarter pipes and banks look like the real thing
 * rather than a staircase.
 */
public final class RampModel implements UnbakedModel, BakedModel {
    public static final ResourceLocation ID = Freerun.id("block/skate_ramp");
    private static final Material TOP = new Material(InventoryMenu.BLOCK_ATLAS, Freerun.id("block/skate_ramp_top"));
    private static final Material SIDE = new Material(InventoryMenu.BLOCK_ATLAS, Freerun.id("block/skate_ramp_side"));
    private static final Material UNDER = new Material(InventoryMenu.BLOCK_ATLAS, Freerun.id("block/skatepark_concrete"));
    private TextureAtlasSprite top, side, under;

    // ------------------------------------------------------------------ unbaked

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state) {
        top = sprites.apply(TOP);
        side = sprites.apply(SIDE);
        under = sprites.apply(UNDER);
        return this;
    }

    // ------------------------------------------------------------------ baked

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        return List.of();
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext ctx) {
        emit(ctx.getEmitter(), state.getValue(SkateRampBlock.FACING), state.getValue(SkateRampBlock.SHAPE));
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> random, RenderContext ctx) {
        emit(ctx.getEmitter(), Direction.NORTH, RampProfiles.KICKER);
    }

    /** Local (w across, t up the ramp, y) to block coordinates for a ramp rising toward {@code f}. */
    private static float[] xyz(Direction f, float w, float t, float y) {
        return switch (f) {
            case NORTH -> new float[]{w, y, 1 - t};
            case SOUTH -> new float[]{1 - w, y, t};
            case EAST -> new float[]{t, y, w};
            default -> new float[]{1 - t, y, 1 - w};
        };
    }

    private void emit(QuadEmitter e, Direction f, int profile) {
        boolean curved = profile >= RampProfiles.QP;
        int n = curved ? 8 : 1;
        float[] h = new float[n + 1];
        for (int i = 0; i <= n; i++) h[i] = (float) RampProfiles.height(profile, i / (float) n);
        // the riding surface: one strip per step
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            if (h[i] < 0.001f && h[i + 1] < 0.001f) continue;
            quad(e, top, new float[][]{xyz(f, 0, t0, h[i]), xyz(f, 0, t1, h[i + 1]), xyz(f, 1, t1, h[i + 1]), xyz(f, 1, t0, h[i])},
                    new float[][]{{0, 1 - t0}, {0, 1 - t1}, {1, 1 - t1}, {1, 1 - t0}}, new float[]{0, 1, 0}, null);
        }
        // the two side profiles
        for (int s = 0; s < 2; s++) {
            float w = s;
            float[] out = xyz(f, s == 0 ? -1 : 2, 0.5f, 0.5f);
            float[] mid = xyz(f, 0.5f, 0.5f, 0.5f);
            float[] normal = {out[0] - mid[0], 0, out[2] - mid[2]};
            for (int i = 0; i < n; i++) {
                float t0 = i / (float) n, t1 = (i + 1) / (float) n;
                if (h[i] < 0.001f && h[i + 1] < 0.001f) continue;
                quad(e, side, new float[][]{xyz(f, w, t0, 0), xyz(f, w, t0, h[i]), xyz(f, w, t1, h[i + 1]), xyz(f, w, t1, 0)},
                        new float[][]{{t0, 1}, {t0, 1 - h[i]}, {t1, 1 - h[i + 1]}, {t1, 1}}, normal, null);
            }
        }
        // the high end and the low end
        if (h[n] > 0.001f) {
            float[] o = xyz(f, 0.5f, 2, 0.5f), m = xyz(f, 0.5f, 0.5f, 0.5f);
            quad(e, side, new float[][]{xyz(f, 0, 1, 0), xyz(f, 0, 1, h[n]), xyz(f, 1, 1, h[n]), xyz(f, 1, 1, 0)},
                    new float[][]{{0, 1}, {0, 1 - h[n]}, {1, 1 - h[n]}, {1, 1}}, new float[]{o[0] - m[0], 0, o[2] - m[2]}, f);
        }
        if (h[0] > 0.001f) {
            float[] o = xyz(f, 0.5f, -1, 0.5f), m = xyz(f, 0.5f, 0.5f, 0.5f);
            quad(e, side, new float[][]{xyz(f, 0, 0, 0), xyz(f, 0, 0, h[0]), xyz(f, 1, 0, h[0]), xyz(f, 1, 0, 0)},
                    new float[][]{{0, 1}, {0, 1 - h[0]}, {1, 1 - h[0]}, {1, 1}}, new float[]{o[0] - m[0], 0, o[2] - m[2]}, f.getOpposite());
        }
        // the underside
        quad(e, under, new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
                new float[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}}, new float[]{0, -1, 0}, Direction.DOWN);
    }

    /**
     * One quad, wound so it faces {@code outward}. UVs are 0..1 within the sprite (wrapped values
     * above 1 are folded back so long strips still tile).
     */
    private static void quad(QuadEmitter e, TextureAtlasSprite sprite, float[][] p, float[][] uv, float[] outward, @Nullable Direction cull) {
        float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
        float bx = p[2][0] - p[0][0], by = p[2][1] - p[0][1], bz = p[2][2] - p[0][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        boolean flip = nx * outward[0] + ny * outward[1] + nz * outward[2] > 0;
        // Minecraft quads are counter-clockwise seen from the front: (p1 - p0) x (p2 - p0) points out
        for (int k = 0; k < 4; k++) {
            int i = flip ? k : 3 - k;
            e.pos(k, p[i][0], p[i][1], p[i][2]);
            float u = uv[i][0], v = uv[i][1];
            e.uv(k, Math.max(0, Math.min(1, u)), Math.max(0, Math.min(1, v)));
        }
        e.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED);
        e.color(-1, -1, -1, -1);
        if (cull != null) e.cullFace(cull);
        else e.cullFace(null);
        e.emit();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return top;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
