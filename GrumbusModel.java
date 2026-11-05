package dev.freerun.client.mobs;

import dev.freerun.mobs.GrumbusEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Reuses the standard biped parts but gives Grumbus a huge head, a rotund body, and short stubby
 * limbs that flail constantly — so he reads as a fat little gray man even without a bespoke model.
 */
public class GrumbusModel extends HumanoidModel<GrumbusEntity> {

    public GrumbusModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(GrumbusEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Big ol' goofy head.
        head.xScale = head.yScale = head.zScale = 1.9f;
        hat.xScale = hat.yScale = hat.zScale = 1.95f;

        // Hugely rotund torso — wide and deep, barely any height.
        body.xScale = 2.4f;
        body.zScale = 2.4f;
        body.yScale = 0.9f;

        // Tiny stubby arms flailing fast off the sides of the belly.
        float flail = Mth.cos(ageInTicks * 1.7f) * 1.4f;
        rightArm.xRot = -0.9f + flail;
        leftArm.xRot = -0.9f - flail;
        rightArm.zRot = 0.6f;    // pushed out around the gut
        leftArm.zRot = -0.6f;
        rightArm.yScale = leftArm.yScale = 0.4f;
        rightArm.xScale = leftArm.xScale = 1.1f;

        // Little legs with a quick waddle.
        float waddle = Mth.cos(ageInTicks * 1.0f) * 0.3f + Mth.sin(limbSwing * 0.6f) * 0.7f * limbSwingAmount;
        rightLeg.xRot = waddle;
        leftLeg.xRot = -waddle;
        rightLeg.yScale = leftLeg.yScale = 0.4f;
        rightLeg.zRot = 0.15f;
        leftLeg.zRot = -0.15f;
    }
}
