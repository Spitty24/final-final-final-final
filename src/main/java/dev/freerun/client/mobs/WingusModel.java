package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import dev.freerun.mobs.WingusEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A round little body with big eyes, two wide flapping wings, and stubby feet. */
public class WingusModel extends HierarchicalModel<WingusEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("wingus"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public WingusModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition body = r.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8),
                PartPose.offset(0, 16, 0));
        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 16).addBox(0, 0, -3, 12, 1, 6),
                PartPose.offset(4, -2, 0));
        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 24).addBox(-12, 0, -3, 12, 1, 6),
                PartPose.offset(-4, -2, 0));
        body.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(2, 4, 0));
        body.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1, 0, -1, 2, 3, 2),
                PartPose.offset(-2, 4, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(WingusEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.cos(ageInTicks * 0.9f) * 0.9f;
        leftWing.zRot = flap;
        rightWing.zRot = -flap;
        body.y = 16 + Mth.sin(ageInTicks * 0.3f) * 1.0f;   // gentle bob while flying
    }
}
