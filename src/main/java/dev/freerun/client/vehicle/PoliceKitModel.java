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
 * What turns a car into a police cruiser: the roof light bar (a black base with a red and a blue
 * lamp), white door panels with a gold badge on each side, and a push bar on the front.
 * Same model space as {@link CarModel} (pixels, y up, nose toward +z).
 */
public class PoliceKitModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Freerun.id("police_kit"), "main");

    public final ModelPart base;
    public final ModelPart red;
    public final ModelPart blue;
    public final ModelPart doors;

    public PoliceKitModel(ModelPart root) {
        base = root.getChild("base");
        red = root.getChild("red");
        blue = root.getChild("blue");
        doors = root.getChild("doors");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0f, 26.0f, -6.0f, 22.0f, 1.0f, 5.0f)
                .texOffs(0, 24).addBox(-12.0f, 3.0f, 36.5f, 24.0f, 6.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("red", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-10.5f, 27.0f, -5.5f, 10.0f, 2.0f, 4.0f), PartPose.ZERO);
        root.addOrReplaceChild("blue", CubeListBuilder.create()
                .texOffs(0, 16).addBox(0.5f, 27.0f, -5.5f, 10.0f, 2.0f, 4.0f), PartPose.ZERO);
        root.addOrReplaceChild("doors", CubeListBuilder.create()
                .texOffs(0, 34).addBox(15.05f, 5.0f, -13.0f, 0.2f, 7.0f, 20.0f)
                .texOffs(0, 64).addBox(-15.25f, 5.0f, -13.0f, 0.2f, 7.0f, 20.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 96);
    }
}
