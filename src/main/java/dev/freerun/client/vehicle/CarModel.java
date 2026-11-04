package dev.freerun.client.vehicle;

import dev.freerun.Freerun;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A four-door sedan: painted body, trim, interior, glass, lights and four wheels on their own pivots.
 * Generated geometry: pixel units, Y up, +Z forward, +X is the driver's (left) side (rendered without the usual entity flip).
 * The UV layout matches textures/entity/car.png (256x256, shipped at 2x).
 */
public final class CarModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("car"), "main");

    public final ModelPart body;
    public final ModelPart trim;
    public final ModelPart interior;
    public final ModelPart lights_front;
    public final ModelPart lights_rear;
    public final ModelPart glass;
    public final ModelPart windshield;
    public final ModelPart rear_window;
    public final ModelPart wheel_fl;
    public final ModelPart wheel_fr;
    public final ModelPart wheel_rl;
    public final ModelPart wheel_rr;

    public CarModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.interior = root.getChild("interior");
        this.lights_front = root.getChild("lights_front");
        this.lights_rear = root.getChild("lights_rear");
        this.glass = root.getChild("glass");
        this.windshield = root.getChild("windshield");
        this.rear_window = root.getChild("rear_window");
        this.wheel_fl = root.getChild("wheel_fl");
        this.wheel_fr = root.getChild("wheel_fr");
        this.wheel_rl = root.getChild("wheel_rl");
        this.wheel_rr = root.getChild("wheel_rr");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-15.0f, 4.0f, -36.0f, 30.0f, 10.0f, 72.0f)
                        .texOffs(90, 144).addBox(-14.0f, 14.0f, 13.0f, 28.0f, 2.0f, 22.0f)
                        .texOffs(44, 198).addBox(-14.0f, 14.0f, -35.0f, 28.0f, 2.0f, 15.0f)
                        .texOffs(102, 174).addBox(-13.5f, 25.0f, -13.0f, 27.0f, 1.0f, 19.0f)
                        .texOffs(0, 217).addBox(-13.5f, 14.0f, -4.0f, 27.0f, 11.0f, 2.0f)
                        .texOffs(0, 244).addBox(-13.5f, 14.0f, 5.0f, 27.0f, 1.0f, 1.0f)
                        .texOffs(72, 238).addBox(-17.0f, 15.0f, 8.0f, 2.0f, 2.0f, 3.0f)
                        .texOffs(82, 238).addBox(15.0f, 15.0f, 8.0f, 2.0f, 2.0f, 3.0f), PartPose.ZERO);
        root.addOrReplaceChild("trim", CubeListBuilder.create()
                        .texOffs(110, 230).addBox(-15.5f, 3.0f, 35.0f, 31.0f, 4.0f, 2.0f)
                        .texOffs(176, 230).addBox(-15.5f, 3.0f, -37.0f, 31.0f, 4.0f, 2.0f)
                        .texOffs(92, 238).addBox(-9.0f, 8.0f, 35.5f, 18.0f, 4.0f, 1.0f)
                        .texOffs(140, 238).addBox(-3.0f, 4.0f, -37.5f, 6.0f, 3.0f, 1.0f)
                        .texOffs(0, 82).addBox(-14.0f, 2.0f, -30.0f, 28.0f, 2.0f, 60.0f)
                        .texOffs(130, 238).addBox(9.0f, 2.5f, -38.0f, 2.0f, 2.0f, 3.0f)
                        .texOffs(154, 238).addBox(14.5f, 11.0f, 1.0f, 1.0f, 1.0f, 3.0f)
                        .texOffs(162, 238).addBox(14.5f, 11.0f, -13.0f, 1.0f, 1.0f, 3.0f)
                        .texOffs(170, 238).addBox(-15.5f, 11.0f, 1.0f, 1.0f, 1.0f, 3.0f)
                        .texOffs(178, 238).addBox(-15.5f, 11.0f, -13.0f, 1.0f, 1.0f, 3.0f)
                        .texOffs(194, 174).addBox(-15.2f, 4.0f, 18.0f, 1.0f, 9.0f, 10.0f)
                        .texOffs(216, 174).addBox(14.2f, 4.0f, 18.0f, 1.0f, 9.0f, 10.0f)
                        .texOffs(0, 198).addBox(-15.2f, 4.0f, -28.0f, 1.0f, 9.0f, 10.0f)
                        .texOffs(22, 198).addBox(14.2f, 4.0f, -28.0f, 1.0f, 9.0f, 10.0f), PartPose.ZERO);
        root.addOrReplaceChild("interior", CubeListBuilder.create()
                        .texOffs(112, 217).addBox(2.0f, 14.0f, -4.0f, 10.0f, 2.0f, 8.0f)
                        .texOffs(148, 217).addBox(-12.0f, 14.0f, -4.0f, 10.0f, 2.0f, 8.0f)
                        .texOffs(184, 217).addBox(2.0f, 16.0f, -5.0f, 10.0f, 8.0f, 2.0f)
                        .texOffs(208, 217).addBox(-12.0f, 16.0f, -5.0f, 10.0f, 8.0f, 2.0f)
                        .texOffs(0, 230).addBox(-12.0f, 14.0f, -12.0f, 24.0f, 2.0f, 6.0f)
                        .texOffs(60, 230).addBox(-12.0f, 16.0f, -13.0f, 24.0f, 7.0f, 1.0f)
                        .texOffs(0, 238).addBox(-13.0f, 14.0f, 3.0f, 26.0f, 3.0f, 3.0f)
                        .texOffs(58, 238).addBox(4.0f, 16.0f, 2.0f, 6.0f, 5.0f, 1.0f), PartPose.ZERO);
        root.addOrReplaceChild("lights_front", CubeListBuilder.create()
                        .texOffs(186, 238).addBox(-14.0f, 10.0f, 35.5f, 5.0f, 3.0f, 1.0f)
                        .texOffs(198, 238).addBox(9.0f, 10.0f, 35.5f, 5.0f, 3.0f, 1.0f), PartPose.ZERO);
        root.addOrReplaceChild("lights_rear", CubeListBuilder.create()
                        .texOffs(210, 238).addBox(-14.0f, 9.0f, -36.5f, 5.0f, 3.0f, 1.0f)
                        .texOffs(222, 238).addBox(9.0f, 9.0f, -36.5f, 5.0f, 3.0f, 1.0f), PartPose.ZERO);
        root.addOrReplaceChild("glass", CubeListBuilder.create()
                        .texOffs(0, 144).addBox(-13.0f, 14.0f, -13.0f, 26.0f, 11.0f, 19.0f), PartPose.ZERO);
        root.addOrReplaceChild("windshield", CubeListBuilder.create()
                        .texOffs(130, 198).addBox(-13.0f, 0.0f, -0.5f, 26.0f, 13.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 14.0f, 13.0f, -0.62f, 0.0f, 0.0f));
        root.addOrReplaceChild("rear_window", CubeListBuilder.create()
                        .texOffs(58, 217).addBox(-13.0f, 0.0f, -0.5f, 26.0f, 12.0f, 1.0f), PartPose.offsetAndRotation(0.0f, 14.0f, -20.0f, 0.55f, 0.0f, 0.0f));
        root.addOrReplaceChild("wheel_fl", CubeListBuilder.create()
                        .texOffs(190, 144).addBox(-2.5f, -6.0f, -6.0f, 5.0f, 12.0f, 12.0f)
                        .texOffs(184, 198).addBox(2.0f, -3.5f, -3.5f, 1.0f, 7.0f, 7.0f), PartPose.offsetAndRotation(12.9f, 6.0f, 23.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("wheel_fr", CubeListBuilder.create()
                        .texOffs(0, 174).addBox(-2.5f, -6.0f, -6.0f, 5.0f, 12.0f, 12.0f)
                        .texOffs(200, 198).addBox(-3.0f, -3.5f, -3.5f, 1.0f, 7.0f, 7.0f), PartPose.offsetAndRotation(-12.9f, 6.0f, 23.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("wheel_rl", CubeListBuilder.create()
                        .texOffs(34, 174).addBox(-2.5f, -6.0f, -6.0f, 5.0f, 12.0f, 12.0f)
                        .texOffs(216, 198).addBox(2.0f, -3.5f, -3.5f, 1.0f, 7.0f, 7.0f), PartPose.offsetAndRotation(12.9f, 6.0f, -23.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("wheel_rr", CubeListBuilder.create()
                        .texOffs(68, 174).addBox(-2.5f, -6.0f, -6.0f, 5.0f, 12.0f, 12.0f)
                        .texOffs(232, 198).addBox(-3.0f, -3.5f, -3.5f, 1.0f, 7.0f, 7.0f), PartPose.offsetAndRotation(-12.9f, 6.0f, -23.0f, 0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 256, 256);
    }
}
