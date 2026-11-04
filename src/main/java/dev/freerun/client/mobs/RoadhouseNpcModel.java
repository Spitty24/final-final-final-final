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
        if (entity.isVisitor()) {
            // On the factory floor yelling up at the crane: pointing straight at Ethan.
            if (limbSwingAmount < 0.1f) {
                rightArm.xRot = -2.5f;
                rightArm.yRot = -0.1f;
                leftArm.xRot = -0.2f;
                leftArm.zRot = -0.7f;
                head.xRot = -0.6f;
                hat.copyFrom(head);
            }
            return;
        }
        if (entity.onBreak()) return;   // walking to the Double Quick like a normal person

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
        } else if (entity.getRole() == Role.WELDER && !entity.isPassenger()) {
            // Hunched over the weld table: torch hand forward, other hand steadying the work.
            boolean arc = RoadhouseNpcEntity.arcOn(time, entity.getId());
            float jitter = arc ? Mth.sin(ageInTicks * 2.3f) * 0.03f : 0.0f;
            rightArm.xRot = -1.05f + jitter;
            rightArm.yRot = -0.25f;
            leftArm.xRot = -0.85f;
            leftArm.yRot = 0.35f;
            rightLeg.xRot = 0.0f;
            leftLeg.xRot = 0.0f;
        } else if ((entity.getRole() == Role.PRESS_OPERATOR || entity.getRole() == Role.SAUSAGE_MAKER) && !entity.isPassenger()) {
            // Feeding the press, then hands back off the bed while the ram comes down.
            int phase = RoadhouseNpcEntity.pressPhase(time, entity.getId());
            float reach = phase < 18 ? -0.35f : -0.95f;
            rightArm.xRot = reach;
            leftArm.xRot = reach;
            rightArm.yRot = -0.15f;
            leftArm.yRot = 0.15f;
            rightLeg.xRot = 0.0f;
            leftLeg.xRot = 0.0f;
        } else if (entity.getRole() == Role.SUPERVISOR && OfficeSkit.slaterLecturing(time)) {
            // Pointing right at Nick, other hand on hip.
            rightArm.xRot = -1.5f;
            rightArm.yRot = -0.1f;
            leftArm.xRot = -0.2f;
            leftArm.zRot = -0.7f;
        }
    }
}
