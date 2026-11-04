package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.LivingStoneEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A block of stone on four stubby legs, with two little arms and an attitude. */
public class LivingStoneModel extends HierarchicalModel<LivingStoneEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("living_stone"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public LivingStoneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg" + i);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -12, -6, 12, 12, 12),
                PartPose.offset(0, 21, 0));
        float[][] legPos = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 24).addBox(-1.5f, 0, -1.5f, 3, 3, 3),
                    PartPose.offset(legPos[i][0], 21, legPos[i][1]));
        }
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(12, 24).addBox(-2, 0, -1, 2, 5, 2),
                PartPose.offset(-6, 12, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(20, 24).addBox(0, 0, -1, 2, 5, 2),
                PartPose.offset(6, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LivingStoneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 1.3f) * 1.2f * limbSwingAmount;
        legs[0].xRot = swing;
        legs[3].xRot = swing;
        legs[1].xRot = -swing;
        legs[2].xRot = -swing;
        // A grumpy little waddle, and arms crossed-ish swinging.
        body.zRot = Mth.sin(limbSwing * 0.65f) * 0.08f * limbSwingAmount;
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3f;
        rightArm.xRot = -swing * 0.8f;
        leftArm.xRot = swing * 0.8f;
        rightArm.zRot = 0.2f;
        leftArm.zRot = -0.2f;
    }
}
