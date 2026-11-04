package dev.freerun.client.factory;

import dev.freerun.Freerun;
import dev.freerun.factory.ForkliftDriverEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A big fella: wide torso with a round belly out front, thick arms and legs, a ball cap with a
 * brim. Uses the standard humanoid part names so walking and sitting animations come for free.
 * UV layout matches textures/entity/forklift_driver.png (128x64, shipped at 2x).
 */
public class ForkliftDriverModel extends HumanoidModel<ForkliftDriverEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("forklift_driver"), "main");

    public ForkliftDriverModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5f, -9.0f, -4.5f, 9, 9, 9), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-5.0f, -10.0f, -5.0f, 10, 4, 10)       // cap crown
                .texOffs(76, 0).addBox(-5.0f, -7.0f, -9.0f, 10, 1, 4), PartPose.ZERO);   // brim
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-6.0f, 0.0f, -3.5f, 12, 13, 7)        // torso
                .texOffs(38, 18).addBox(-5.5f, 3.0f, -6.5f, 11, 10, 3), PartPose.ZERO);  // belly
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(66, 18).addBox(-3.0f, -2.0f, -2.5f, 5, 12, 5), PartPose.offset(-8.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(86, 18).addBox(-2.0f, -2.0f, -2.5f, 5, 12, 5), PartPose.offset(8.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 38).addBox(-3.0f, 0.0f, -3.0f, 6, 12, 6), PartPose.offset(-3.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(24, 38).addBox(-3.0f, 0.0f, -3.0f, 6, 12, 6), PartPose.offset(3.0f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(ForkliftDriverEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (riding) {
            // Both hands on the wheel, the belly against it, bouncing along.
            rightArm.xRot = -1.05f + Mth.sin(ageInTicks * 0.3f) * 0.04f;
            leftArm.xRot = -1.05f - Mth.sin(ageInTicks * 0.3f) * 0.04f;
            rightArm.yRot = 0.3f;
            leftArm.yRot = -0.3f;
            rightArm.zRot = 0.0f;
            leftArm.zRot = 0.0f;
        }
        // Always talking: a little head bob.
        head.y = Mth.sin(ageInTicks * 0.6f) * 0.15f;
        hat.copyFrom(head);
    }
}
