package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.FlavusEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Flavus Florbington: a broad V-shaped chest, huge arms, short legs, and a head of 3D curly hair
 * made of separate curl cubes.
 *
 * UVs are laid out on a 64x64 grid but the texture file is 128x128, so every model pixel gets
 * 2x2 texels — double the usual Minecraft resolution.
 */
public class FlavusModel extends HierarchicalModel<FlavusEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("flavus"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FlavusModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // Head with a mop of 3D curls (all curls share the hair texture patch at 32,0).
        PartDefinition head = r.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6),
                PartPose.offset(0, 12, 0));
        CubeListBuilder curls = CubeListBuilder.create().texOffs(32, 0)
                .addBox(-3.5f, -7.5f, -3.5f, 3, 2, 3)      // top curls (4 quadrants)
                .addBox(0.5f, -7.5f, -3.5f, 3, 2, 3)
                .addBox(-3.5f, -7.5f, 0.5f, 3, 2, 3)
                .addBox(0.5f, -7.5f, 0.5f, 3, 2, 3)
                .addBox(-1.5f, -8.5f, -1.5f, 3, 2, 3)      // crown curl
                .addBox(-3.5f, -6.0f, 2.5f, 7, 3, 1)       // back of head
                .addBox(-3.8f, -6.0f, -2.0f, 1, 2, 4)      // side curls
                .addBox(2.8f, -6.0f, -2.0f, 1, 2, 4)
                .addBox(-3.0f, -6.5f, -3.6f, 2, 1, 1)      // front fringe curls
                .addBox(1.0f, -6.5f, -3.6f, 2, 1, 1)
                .addBox(-0.5f, -6.8f, -3.7f, 1, 1, 1);
        head.addOrReplaceChild("hair", curls, PartPose.ZERO);

        // Broad chest tapering to a narrower waist.
        r.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-4.5f, 0, -2.5f, 9, 4, 5)      // chest
                        .texOffs(0, 22).addBox(-3.5f, 4, -2.0f, 7, 3, 4),     // waist
                PartPose.offset(0, 12, 0));

        // Huge arms.
        r.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 12).addBox(-2, -0.5f, -2, 4, 7, 4),
                PartPose.offset(-6.5f, 12.5f, 0));
        r.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 12).mirror().addBox(-2, -0.5f, -2, 4, 7, 4),
                PartPose.offset(6.5f, 12.5f, 0));

        // Short legs.
        r.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(32, 24).addBox(-1.5f, 0, -1.5f, 3, 5, 3),
                PartPose.offset(-2, 19, 0));
        r.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-1.5f, 0, -1.5f, 3, 5, 3),
                PartPose.offset(2, 19, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FlavusEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        float swing = Mth.cos(limbSwing * 0.6662f) * limbSwingAmount;
        float breathe = Mth.sin(ageInTicks * 0.08f) * 0.04f;

        if (entity.isPassenger()) {
            // Sitting astride the Wingus, holding on with both arms.
            rightLeg.xRot = -1.4f;
            leftLeg.xRot = -1.4f;
            rightLeg.yRot = 0.3f;
            leftLeg.yRot = -0.3f;
            rightArm.xRot = -0.7f;
            leftArm.xRot = -0.7f;
        } else {
            rightLeg.xRot = swing * 1.4f;
            leftLeg.xRot = -swing * 1.4f;
            rightLeg.yRot = 0;
            leftLeg.yRot = 0;
            rightArm.xRot = -swing * 0.9f;
            leftArm.xRot = swing * 0.9f;
        }
        // Arms held out a little — too much muscle to hang straight.
        rightArm.zRot = 0.18f + breathe;
        leftArm.zRot = -0.18f - breathe;
        rightArm.yRot = 0;
        leftArm.yRot = 0;
        body.xRot = 0;
    }
}
