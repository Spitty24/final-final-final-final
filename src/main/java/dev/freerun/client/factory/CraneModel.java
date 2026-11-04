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
 * 3D model of the overhead crane: girder segment, end truck, hoist trolley, cable, hook block.
 * Generated geometry: pixel units, Y up, +Z forward (rendered without the usual entity flip).
 * The UV layout matches textures/entity/crane.png (128x128, shipped at 2x).
 */
public final class CraneModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("crane"), "main");

    public final ModelPart beam;
    public final ModelPart end_truck;
    public final ModelPart trolley;
    public final ModelPart cable;
    public final ModelPart hook;

    public CraneModel(ModelPart root) {
        this.beam = root.getChild("beam");
        this.end_truck = root.getChild("end_truck");
        this.trolley = root.getChild("trolley");
        this.cable = root.getChild("cable");
        this.hook = root.getChild("hook");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("beam", CubeListBuilder.create()
                        .texOffs(68, 33).addBox(-8.0f, 1.0f, -4.0f, 16, 8, 8)
                        .texOffs(0, 55).addBox(-8.0f, 0.0f, -6.0f, 16, 1, 12)
                        .texOffs(56, 55).addBox(-8.0f, 9.0f, -6.0f, 16, 1, 12), PartPose.ZERO);
        root.addOrReplaceChild("end_truck", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0f, 0.0f, -13.0f, 12, 7, 26)
                        .texOffs(60, 68).addBox(-7.0f, 0.0f, -11.0f, 1, 4, 4)
                        .texOffs(70, 68).addBox(-7.0f, 0.0f, 7.0f, 1, 4, 4)
                        .texOffs(80, 68).addBox(-6.0f, 7.0f, -2.0f, 12, 2, 4), PartPose.ZERO);
        root.addOrReplaceChild("trolley", CubeListBuilder.create()
                        .texOffs(0, 33).addBox(-7.0f, -6.0f, -8.0f, 14, 6, 16)
                        .texOffs(0, 78).addBox(-5.0f, -5.0f, -10.0f, 10, 4, 2)
                        .texOffs(0, 68).addBox(7.0f, -5.0f, -3.0f, 4, 4, 6)
                        .texOffs(20, 68).addBox(-11.0f, -5.0f, -3.0f, 4, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("cable", CubeListBuilder.create()
                        .texOffs(60, 33).addBox(-2.0f, -16.0f, -0.5f, 1, 16, 1)
                        .texOffs(64, 33).addBox(1.0f, -16.0f, -0.5f, 1, 16, 1), PartPose.ZERO);
        root.addOrReplaceChild("hook", CubeListBuilder.create()
                        .texOffs(40, 68).addBox(-3.0f, -6.0f, -2.0f, 6, 6, 4)
                        .texOffs(24, 78).addBox(-1.0f, -10.0f, -1.0f, 2, 4, 2)
                        .texOffs(32, 78).addBox(-1.0f, -11.0f, -1.0f, 4, 1, 2)
                        .texOffs(44, 78).addBox(2.0f, -10.0f, -1.0f, 1, 1, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }
}
