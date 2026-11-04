package dev.freerun.client.factory;

import dev.freerun.Freerun;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 3D model of the counterbalance forklift: body, moving forks, pallet, load, beacon.
 * Generated geometry: pixel units, Y up, +Z forward (rendered without the usual entity flip).
 * The UV layout matches textures/entity/forklift.png (256x128, shipped at 2x).
 */
public final class ForkliftModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("forklift"), "main");

    public final ModelPart body;
    public final ModelPart forks;
    public final ModelPart pallet;
    public final ModelPart load;
    public final ModelPart beacon;

    public ForkliftModel(ModelPart root) {
        this.body = root.getChild("body");
        this.forks = root.getChild("forks");
        this.pallet = root.getChild("pallet");
        this.load = root.getChild("load");
        this.beacon = root.getChild("beacon");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-10.0f, 3.0f, -15.0f, 20, 8, 24)
                        .texOffs(0, 64).addBox(-11.0f, 2.0f, -19.0f, 22, 13, 5)
                        .texOffs(140, 82).addBox(-9.0f, 6.0f, -19.5f, 18, 5, 1)
                        .texOffs(54, 64).addBox(-8.0f, 11.0f, -14.0f, 16, 4, 11)
                        .texOffs(38, 82).addBox(-5.0f, 15.0f, -12.0f, 10, 1, 8)
                        .texOffs(228, 64).addBox(-5.0f, 16.0f, -13.0f, 10, 9, 2)
                        .texOffs(148, 64).addBox(-8.0f, 11.0f, 3.0f, 16, 7, 6)
                        .texOffs(178, 82).addBox(-1.0f, 18.0f, 4.0f, 2, 4, 2)
                        .texOffs(112, 82).addBox(-4.0f, 22.0f, 2.0f, 8, 1, 6)
                        .texOffs(116, 0).addBox(-10.0f, 11.0f, 7.0f, 2, 27, 2)
                        .texOffs(124, 0).addBox(8.0f, 11.0f, 7.0f, 2, 27, 2)
                        .texOffs(132, 0).addBox(-10.0f, 15.0f, -15.0f, 2, 23, 2)
                        .texOffs(140, 0).addBox(8.0f, 15.0f, -15.0f, 2, 23, 2)
                        .texOffs(148, 0).addBox(-10.0f, 38.0f, -15.0f, 20, 1, 24)
                        .texOffs(108, 64).addBox(-12.0f, 0.0f, 2.0f, 3, 7, 7)
                        .texOffs(128, 64).addBox(9.0f, 0.0f, 2.0f, 3, 7, 7)
                        .texOffs(192, 64).addBox(-12.0f, 0.0f, -13.0f, 3, 6, 6)
                        .texOffs(210, 64).addBox(9.0f, 0.0f, -13.0f, 3, 6, 6)
                        .texOffs(0, 0).addBox(-8.0f, 1.0f, 9.0f, 2, 40, 3)
                        .texOffs(10, 0).addBox(6.0f, 1.0f, 9.0f, 2, 40, 3)
                        .texOffs(186, 82).addBox(-8.0f, 39.0f, 9.0f, 16, 2, 3)
                        .texOffs(108, 0).addBox(-1.0f, 3.0f, 10.0f, 2, 30, 2)
                        .texOffs(232, 82).addBox(-10.0f, 10.0f, -19.6f, 2, 2, 1)
                        .texOffs(238, 82).addBox(8.0f, 10.0f, -19.6f, 2, 2, 1)
                        .texOffs(244, 82).addBox(-10.0f, 30.0f, 9.0f, 2, 2, 1)
                        .texOffs(250, 82).addBox(8.0f, 30.0f, 9.0f, 2, 2, 1), PartPose.ZERO);
        root.addOrReplaceChild("forks", CubeListBuilder.create()
                        .texOffs(74, 82).addBox(-9.0f, 1.0f, 12.0f, 18, 7, 1)
                        .texOffs(0, 82).addBox(-9.0f, 8.0f, 12.0f, 18, 10, 1)
                        .texOffs(0, 43).addBox(-7.0f, 0.0f, 13.0f, 3, 1, 20)
                        .texOffs(46, 43).addBox(4.0f, 0.0f, 13.0f, 3, 1, 20), PartPose.ZERO);
        root.addOrReplaceChild("pallet", CubeListBuilder.create()
                        .texOffs(92, 43).addBox(-11.0f, 1.0f, 14.0f, 22, 3, 18), PartPose.ZERO);
        root.addOrReplaceChild("load", CubeListBuilder.create()
                        .texOffs(172, 43).addBox(-10.0f, 4.0f, 15.0f, 20, 4, 16), PartPose.ZERO);
        root.addOrReplaceChild("beacon", CubeListBuilder.create()
                        .texOffs(224, 82).addBox(-1.0f, 39.0f, -13.0f, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }
}
