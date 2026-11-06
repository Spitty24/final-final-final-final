package dev.freerun.client.alien;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.freerun.Freerun;
import dev.freerun.alien.Alien;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

import java.util.HashMap;
import java.util.Map;

/** One creature's baked rig: the root plus every named part, so the animator can grab them by name. */
public final class AlienModel {
    public final Alien alien;
    public final ModelPart root;
    public final Map<String, ModelPart> parts = new HashMap<>();

    public AlienModel(Alien alien, ModelPart root) {
        this.alien = alien;
        this.root = root;
        for (String n : NAMES) if (root.hasChild(n)) parts.put(n, root.getChild(n));
    }

    private static final String[] NAMES = {"body", "belly", "shell", "head", "armL", "armR", "legL", "legR", "tail", "abdomen", "wingL", "wingR",
            "legL2", "legR2", "antL", "antR", "pincerL", "pincerR", "sideL", "sideR", "crest", "tail1", "tail2", "finBack", "canopy",
            "kneeL", "kneeR", "mossL", "hornL", "hornR", "ring", "jaw", "bits", "drip"};

    public static ModelLayerLocation layer(Alien a) {
        return new ModelLayerLocation(Freerun.id("alien_" + a.id()), "main");
    }

    public ModelPart part(String name) {
        return parts.get(name);
    }

    public void reset() {
        root.getAllParts().forEach(ModelPart::resetPose);
    }

    public void render(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        root.render(ps, vc, light, overlay, color);
    }
}
