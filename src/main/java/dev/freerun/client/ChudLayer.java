package dev.freerun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.food.ChudMaxingEffect;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Draws a much bigger body over any player who is Chud-Maxing: a wide, deep torso, a belly out
 * front, thick arms and legs, and a double chin, all textured with the player's own skin so it's
 * still recognisably them. Visible to everyone (the effect's speed modifier is synced to
 * everyone tracking the player, so every client can tell).
 */
public class ChudLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("chud_body"), "main");

    private final ModelPart body;
    private final ModelPart belly;
    private final ModelPart chin;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ChudLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        body = root.getChild("body");
        belly = root.getChild("belly");
        chin = root.getChild("chin");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    /** Same UVs as a player skin, just inflated. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(2.2f, 0.2f, 2.0f)), PartPose.ZERO);
        root.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-4, 4, -5.5f, 8, 7, 3, new CubeDeformation(1.4f, 0.6f, 1.2f)), PartPose.ZERO);
        root.addOrReplaceChild("chin", CubeListBuilder.create().texOffs(8, 8)
                .addBox(-4, -2.2f, -4.6f, 8, 2, 4, new CubeDeformation(0.35f)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -2, -2, 4, 12, 4, new CubeDeformation(1.1f, 0.1f, 1.1f)), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1, -2, -2, 4, 12, 4, new CubeDeformation(1.1f, 0.1f, 1.1f)), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4, new CubeDeformation(1.0f, 0.0f, 1.0f)), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2, 0, -2, 4, 12, 4, new CubeDeformation(1.0f, 0.0f, 1.0f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static boolean isChudMaxing(AbstractClientPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(ChudMaxingEffect.SPEED_MOD);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !isChudMaxing(player)) return;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(player)));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0f);
        draw(model.body, body, pose, vc, light, overlay);
        draw(model.body, belly, pose, vc, light, overlay);
        draw(model.head, chin, pose, vc, light, overlay);
        draw(model.rightArm, rightArm, pose, vc, light, overlay);
        draw(model.leftArm, leftArm, pose, vc, light, overlay);
        draw(model.rightLeg, rightLeg, pose, vc, light, overlay);
        draw(model.leftLeg, leftLeg, pose, vc, light, overlay);
    }

    /** Render {@code part} wherever (and however rotated) the matching vanilla part is right now. */
    private static void draw(ModelPart follow, ModelPart part, PoseStack pose, VertexConsumer vc, int light, int overlay) {
        if (!follow.visible) return;
        pose.pushPose();
        follow.translateAndRotate(pose);
        part.render(pose, vc, light, overlay);
        pose.popPose();
    }
}
