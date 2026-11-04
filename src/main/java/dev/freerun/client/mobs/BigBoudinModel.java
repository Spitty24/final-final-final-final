package dev.freerun.client.mobs;

import dev.freerun.Freerun;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Big Boudin: a seven-foot walking link of boudin in a hard hat, swinging a giant grill spatula.
 * Generated geometry: pixel units, standard entity space (Y down, the usual entity flip applies).
 * The UV layout matches textures/entity/big_boudin.png (128x128, shipped at 2x).
 */
public final class BigBoudinModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("big_boudin"), "main");

    public final ModelPart body;
    public final ModelPart hat;
    public final ModelPart right_arm;
    public final ModelPart left_arm;
    public final ModelPart right_leg;
    public final ModelPart left_leg;

    public BigBoudinModel(ModelPart root) {
        this.body = root.getChild("body");
        this.hat = root.getChild("hat");
        this.right_arm = root.getChild("right_arm");
        this.left_arm = root.getChild("left_arm");
        this.right_leg = root.getChild("right_leg");
        this.left_leg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0f, -20.0f, -7.0f, 14.0f, 36.0f, 14.0f)
                        .texOffs(64, 50).addBox(-6.0f, -24.0f, -6.0f, 12.0f, 4.0f, 12.0f)
                        .texOffs(56, 83).addBox(-5.0f, 16.0f, -5.0f, 10.0f, 2.0f, 10.0f)
                        .texOffs(0, 67).addBox(-7.5f, -16.0f, -7.5f, 15.0f, 1.0f, 15.0f)
                        .texOffs(60, 67).addBox(-7.5f, 12.0f, -7.5f, 15.0f, 1.0f, 15.0f)
                        .texOffs(112, 99).addBox(-4.0f, -14.0f, -7.5f, 3.0f, 3.0f, 1.0f)
                        .texOffs(120, 99).addBox(1.0f, -14.0f, -7.5f, 3.0f, 3.0f, 1.0f)
                        .texOffs(14, 107).addBox(-3.0f, -13.0f, -7.8f, 1.0f, 1.0f, 1.0f)
                        .texOffs(18, 107).addBox(2.0f, -13.0f, -7.8f, 1.0f, 1.0f, 1.0f)
                        .texOffs(0, 107).addBox(-3.0f, -8.0f, -7.5f, 6.0f, 2.0f, 1.0f), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-6.0f, -29.0f, -6.0f, 12.0f, 5.0f, 12.0f)
                        .texOffs(0, 50).addBox(-8.0f, -25.0f, -8.0f, 16.0f, 1.0f, 16.0f), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 83).addBox(-3.0f, -1.0f, -2.0f, 3.0f, 12.0f, 4.0f)
                        .texOffs(0, 99).addBox(-3.5f, 10.0f, -2.5f, 4.0f, 3.0f, 5.0f)
                        .texOffs(28, 83).addBox(-2.5f, 11.0f, -14.0f, 2.0f, 2.0f, 12.0f)
                        .texOffs(96, 83).addBox(-4.5f, 11.0f, -24.0f, 6.0f, 1.0f, 10.0f), PartPose.offsetAndRotation(-8.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(14, 83).addBox(0.0f, -1.0f, -2.0f, 3.0f, 12.0f, 4.0f)
                        .texOffs(18, 99).addBox(-0.5f, 10.0f, -2.5f, 4.0f, 3.0f, 5.0f), PartPose.offsetAndRotation(8.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(36, 99).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 4.0f, 4.0f)
                        .texOffs(52, 99).addBox(-2.5f, 4.0f, -3.5f, 5.0f, 2.0f, 6.0f), PartPose.offsetAndRotation(-3.5f, 18.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(74, 99).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 4.0f, 4.0f)
                        .texOffs(90, 99).addBox(-2.5f, 4.0f, -3.5f, 5.0f, 2.0f, 6.0f), PartPose.offsetAndRotation(3.5f, 18.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
