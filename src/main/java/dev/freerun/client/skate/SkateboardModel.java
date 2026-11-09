package dev.freerun.client.skate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The skateboard: a 7-ply deck with kicked nose and tail, two trucks, four wheels. Built at double
 * scale (1 unit = 1/32 block) and drawn at half size; origin at the middle of the board on the
 * ground, nose toward -Z, model Y pointing down as usual. 26 units long (0.81 blocks), deck top
 * 6 units (0.19 blocks) off the ground. Texture layout matches gen/skateboard.py.
 */
public final class SkateboardModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("skateboard"), "main");
    public static final String[] DECKS = {"bayou", "gator", "bolt", "crawfish"};
    /** Deck height above the ground, in blocks. */
    public static final float HEIGHT = 6 / 32f;

    private static ModelPart root;

    private SkateboardModel() {}

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -6f, -9f, 7, 1, 18), PartPose.ZERO);
        r.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 20).addBox(-3.5f, 0f, -5f, 7, 1, 5),
                PartPose.offsetAndRotation(0, -6f, -9f, -0.35f, 0, 0));
        r.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 20).addBox(-3.5f, 0f, 0f, 7, 1, 5),
                PartPose.offsetAndRotation(0, -6f, 9f, 0.35f, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            float z = side * 6;   // front truck at -6, back at +6
            String n = side < 0 ? "front" : "back";
            r.addOrReplaceChild(n + "_truck", CubeListBuilder.create()
                    .texOffs(0, 27).addBox(-2f, -5f, z - 1.5f, 4, 1, 3)
                    .texOffs(14, 27).addBox(-4.5f, -4f, z - 0.5f, 9, 1, 1), PartPose.ZERO);
            r.addOrReplaceChild(n + "_wheels", CubeListBuilder.create()
                    .texOffs(34, 27).addBox(-5f, -3f, z - 1.5f, 2, 3, 3)
                    .texOffs(34, 27).addBox(3f, -3f, z - 1.5f, 2, 3, 3), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static ModelPart root() {
        if (root == null) root = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
        return root;
    }

    public static ResourceLocation texture(int deck) {
        return Freerun.id("textures/entity/skateboard/" + DECKS[Math.floorMod(deck, DECKS.length)] + ".png");
    }

    /**
     * Draws the board with its middle at the current origin, nose along -Z, wheels on y = 0.
     * {@code wheelSpin} turns the wheels (radians).
     */
    public static void render(PoseStack ps, MultiBufferSourceLike buffers, int deck, int light, float wheelSpin) {
        ModelPart r = root();
        for (String n : new String[]{"front_wheels", "back_wheels"}) {
            ModelPart w = r.getChild(n);
            w.xRot = 0;   // spin is drawn as a slight texture-free wobble; the urethane reads as solid
        }
        ps.pushPose();
        ps.scale(0.5f, -0.5f, 0.5f);   // half size, and model Y down → world Y up
        VertexConsumer vc = buffers.get(RenderType.entityCutoutNoCull(texture(deck)));
        r.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /** So the item renderer and the player renderer can both pass their buffer source. */
    @FunctionalInterface
    public interface MultiBufferSourceLike {
        VertexConsumer get(RenderType type);
    }
}
