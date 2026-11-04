package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.EthanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Enderman proportions on a regular guy: a normal head on a narrow body with very long, thin arms
 * and legs (2.9 blocks tall). Positions are re-set after the humanoid animation, the way the
 * vanilla enderman model does it. UV layout: textures/entity/ethan_james_patrick.png (64x64).
 */
public class EthanModel extends HumanoidModel<EthanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("ethan_james_patrick"), "main");

    public EthanModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-3, 0, -1.5f, 6, 12, 3), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, -1, -1, 2, 28, 2),
                PartPose.offset(-4, -13, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 16).addBox(-1, -1, -1, 2, 28, 2),
                PartPose.offset(4, -13, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -1, 2, 26, 2),
                PartPose.offset(-1.2f, -2, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 16).addBox(-1, 0, -1, 2, 26, 2),
                PartPose.offset(1.2f, -2, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EthanEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Long limbs swing in smaller arcs, like an enderman's.
        rightArm.xRot *= 0.5f;
        leftArm.xRot *= 0.5f;
        rightLeg.xRot *= 0.5f;
        leftLeg.xRot *= 0.5f;
        head.y = -14.0f;
        head.z = 0.0f;
        body.y = -14.0f;
        body.z = 0.0f;
        rightArm.setPos(-4.0f, -13.0f, 0.0f);
        leftArm.setPos(4.0f, -13.0f, 0.0f);
        rightLeg.setPos(-1.2f, -2.0f, 0.0f);
        leftLeg.setPos(1.2f, -2.0f, 0.0f);
        if (entity.isHanging()) {
            // Both arms straight up, gripping the crane girder; legs dangling.
            rightArm.xRot = (float) Math.PI;
            leftArm.xRot = (float) Math.PI;
            rightArm.zRot = -0.1f;
            leftArm.zRot = 0.1f;
            rightLeg.xRot = (float) Math.sin(ageInTicks * 0.15f) * 0.25f;
            leftLeg.xRot = -rightLeg.xRot;
        }
        // Puzzled head tilt every so often.
        head.zRot = (float) Math.sin(ageInTicks * 0.05f) * 0.12f;
        hat.copyFrom(head);
    }
}
