package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.MeowbeanEntity;
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
 * Meowbean: an egg-shaped body built from stacked, tapering boxes (with the cat face painted on
 * the front), two skinny arms held up with little hands, and two short legs with feet.
 *
 * UVs use a 64x64 layout; the texture file is 256x256 (4 texels per model pixel) for the face.
 */
public class MeowbeanModel extends HierarchicalModel<MeowbeanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("meowbean"), "main");
    private static final float BODY_Y = 20.0f;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MeowbeanModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // The bean: narrow on top, widest in the middle, rounding off at the bottom.
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0f, -16, -2.5f, 6, 2, 5)      // top cap
                        .texOffs(22, 0).addBox(-4.5f, -14, -3.5f, 9, 2, 7)     // upper
                        .texOffs(0, 9).addBox(-6.0f, -12, -4.5f, 12, 9, 9)     // main (face on front)
                        .texOffs(0, 27).addBox(-5.0f, -3, -4.0f, 10, 2, 8)     // lower
                        .texOffs(0, 37).addBox(-3.5f, -1, -2.5f, 7, 1, 5),     // bottom
                PartPose.offset(0, BODY_Y, 0));

        // Skinny arms pointing up with little hands.
        body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(44, 9).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f)
                        .texOffs(44, 18).addBox(-1.25f, -9, -1.25f, 2.5f, 2, 2.5f),
                PartPose.offset(6.0f, -9, 0));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(44, 9).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f)
                        .texOffs(44, 18).addBox(-1.25f, -9, -1.25f, 2.5f, 2, 2.5f),
                PartPose.offset(-6.0f, -9, 0));

        // Short legs with feet.
        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(44, 24).addBox(-0.75f, 0, -0.75f, 1.5f, 3, 1.5f)
                .texOffs(44, 30).addBox(-1.25f, 3, -2.25f, 2.5f, 1, 3.5f);
        r.addOrReplaceChild("left_leg", leg, PartPose.offset(2.5f, BODY_Y, 0));
        r.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.5f, BODY_Y, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MeowbeanEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // The whole bean turns toward whoever he's singing at, and bounces to the beat.
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        body.xRot = 0;
        body.zRot = Mth.sin(ageInTicks * 0.5f) * 0.08f;
        body.y = BODY_Y - Math.abs(Mth.sin(ageInTicks * 0.5f)) * 0.6f;

        // Arms up, waving alternately.
        float wave = Mth.sin(ageInTicks * 0.6f) * 0.35f;
        leftArm.zRot = 0.45f + wave;
        rightArm.zRot = -0.45f + wave;
        leftArm.xRot = 0;
        rightArm.xRot = 0;

        // Little steps.
        float step = Mth.cos(limbSwing * 0.9f) * 1.2f * limbSwingAmount;
        leftLeg.xRot = step;
        rightLeg.xRot = -step;
    }
}
