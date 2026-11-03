package dev.freerun.client.mobs;

import dev.freerun.mobs.OfficeSkit;
import dev.freerun.mobs.RoadhouseNpcEntity;
import dev.freerun.mobs.RoadhouseNpcEntity.Role;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Player-shaped NPC model with extra poses for the office scene. */
public class RoadhouseNpcModel extends HumanoidModel<RoadhouseNpcEntity> {

    public RoadhouseNpcModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(RoadhouseNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        head.zRot = 0.0f;           // the model is shared by every NPC, so clear the phone tilt
        hat.copyFrom(head);
        long time = entity.level().getGameTime();

        if (entity.getRole() == Role.SAFETY && OfficeSkit.nickDown(time)) {
            // Hands over his face, legs kicking like a toddler.
            rightArm.xRot = -2.6f;
            leftArm.xRot = -2.6f;
            rightArm.zRot = 0.35f;
            leftArm.zRot = -0.35f;
            float kick = Mth.sin(ageInTicks * 0.9f) * 0.9f;
            rightLeg.xRot = kick;
            leftLeg.xRot = -kick;
            head.xRot = 0.35f;
            hat.copyFrom(head);
        } else if (entity.getRole() == Role.SUPERVISOR && OfficeSkit.slaterOnCall(time)) {
            // Phone held up to his ear, head tilted into it, other hand on hip.
            rightArm.xRot = -1.9f;
            rightArm.yRot = -0.8f;
            rightArm.zRot = 0.0f;
            leftArm.xRot = -0.2f;
            leftArm.zRot = -0.7f;
            head.zRot = 0.15f;
            hat.copyFrom(head);
        } else if (entity.getRole() == Role.SUPERVISOR && OfficeSkit.slaterLecturing(time)) {
            // Pointing right at Nick, other hand on hip.
            rightArm.xRot = -1.5f;
            rightArm.yRot = -0.1f;
            leftArm.xRot = -0.2f;
            leftArm.zRot = -0.7f;
        }
    }
}
