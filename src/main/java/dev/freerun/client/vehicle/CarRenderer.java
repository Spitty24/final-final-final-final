package dev.freerun.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.freerun.Freerun;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a car: painted body (tinted with the car's colour), trim, interior, tinted glass, lights that
 * glow while it's running, and four wheels that roll with the distance travelled (the fronts steer).
 */
public class CarRenderer extends EntityRenderer<CarEntity> {
    private static final ResourceLocation TEXTURE = Freerun.id("textures/entity/car.png");
    private static final ResourceLocation POLICE = Freerun.id("textures/entity/police_kit.png");
    private static final float MAX_STEER = 34.0f * Mth.DEG_TO_RAD;
    private final CarModel model;
    private final PoliceKitModel kit;

    public CarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.4f;
        shadowStrength = 0.8f;
        model = new CarModel(ctx.bakeLayer(CarModel.LAYER));
        kit = new PoliceKitModel(ctx.bakeLayer(PoliceKitModel.LAYER));
    }

    @Override
    public void render(CarEntity car, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        int overlay = OverlayTexture.NO_OVERLAY;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int paint = 0xFF000000 | car.color();
        // a car you're still building only shows the parts that are on it
        boolean body = car.has(dev.freerun.garage.CarPart.BODY);
        if (body) model.body.render(pose, vc, light, overlay, paint);
        model.trim.render(pose, vc, light, overlay);   // the frame, bumpers and sills: the chassis itself
        if (car.has(dev.freerun.garage.CarPart.SEATS)) model.interior.render(pose, vc, light, overlay);
        boolean running = car.isVehicle();
        int lamp = running ? LightTexture.FULL_BRIGHT : light;
        if (body) {
            model.lights_front.render(pose, vc, lamp, overlay);
            model.lights_rear.render(pose, vc, lamp, overlay);
        }

        float spin = car.wheelSpin(partialTick);
        float steer = car.steer(partialTick) * MAX_STEER;
        if (car.has(dev.freerun.garage.CarPart.WHEEL_FL)) wheel(model.wheel_fl, spin, steer, pose, vc, light, overlay);
        if (car.has(dev.freerun.garage.CarPart.WHEEL_FR)) wheel(model.wheel_fr, spin, steer, pose, vc, light, overlay);
        if (car.has(dev.freerun.garage.CarPart.WHEEL_RL)) wheel(model.wheel_rl, spin, 0.0f, pose, vc, light, overlay);
        if (car.has(dev.freerun.garage.CarPart.WHEEL_RR)) wheel(model.wheel_rr, spin, 0.0f, pose, vc, light, overlay);
        if (!body) bare(car, pose, buffers, light);

        if (car.isPolice()) {
            VertexConsumer pk = buffers.getBuffer(RenderType.entityCutoutNoCull(POLICE));
            kit.base.render(pose, pk, light, overlay);
            kit.doors.render(pose, pk, light, overlay);
            // the light bar: alternating red and blue while the siren's on
            boolean on = car.sirenOn();
            long phase = (car.level().getGameTime() + car.getId()) / 4 % 2;
            kit.red.render(pose, pk, on && phase == 0 ? LightTexture.FULL_BRIGHT : light, overlay, on && phase == 0 ? 0xFFFFFFFF : 0xFF707070);
            kit.blue.render(pose, pk, on && phase == 1 ? LightTexture.FULL_BRIGHT : light, overlay, on && phase == 1 ? 0xFFFFFFFF : 0xFF707070);
        }

        if (body && car.has(dev.freerun.garage.CarPart.GLASS)) {
            VertexConsumer glass = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
            model.glass.render(pose, glass, light, overlay);
            model.windshield.render(pose, glass, light, overlay);
            model.rear_window.render(pose, glass, light, overlay);
        }
        pose.popPose();
        super.render(car, entityYaw, partialTick, pose, buffers, light);
    }

    /**
     * With no body on, you can see the guts: the engine up front, the gearbox behind it, the
     * battery, the tank at the back and the steering column. Drawn last, in their own buffer.
     */
    private static void bare(CarEntity car, PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer b = buffers.getBuffer(dev.freerun.client.laser.Boxes.SOLID);
        float px = 1 / 16f;
        if (car.has(dev.freerun.garage.CarPart.ENGINE)) {
            dev.freerun.client.laser.Boxes.box(pose, b, -8 * px, 4 * px, 20 * px, 8 * px, 13 * px, 32 * px, 0xFF4A4E54, light);
            dev.freerun.client.laser.Boxes.box(pose, b, -6 * px, 13 * px, 21 * px, 6 * px, 15 * px, 31 * px, 0xFFB02020, light);   // valve cover
        }
        if (car.has(dev.freerun.garage.CarPart.TRANSMISSION))
            dev.freerun.client.laser.Boxes.box(pose, b, -4 * px, 3 * px, 8 * px, 4 * px, 9 * px, 20 * px, 0xFF6A6E74, light);
        if (car.has(dev.freerun.garage.CarPart.BATTERY))
            dev.freerun.client.laser.Boxes.box(pose, b, 9 * px, 4 * px, 26 * px, 13 * px, 9 * px, 32 * px, 0xFF1A1A1A, light);
        if (car.has(dev.freerun.garage.CarPart.FUEL_TANK))
            dev.freerun.client.laser.Boxes.box(pose, b, -10 * px, 3 * px, -30 * px, 10 * px, 8 * px, -20 * px, 0xFF8A8E94, light);
        if (car.has(dev.freerun.garage.CarPart.STEERING)) {
            dev.freerun.client.laser.Boxes.box(pose, b, 6.5f * px, 9 * px, 4 * px, 7.5f * px, 16 * px, 14 * px, 0xFF2A2A2A, light);
            dev.freerun.client.laser.Boxes.box(pose, b, 4 * px, 16 * px, 3 * px, 10 * px, 22 * px, 4 * px, 0xFF2A2A2A, light);
        }
    }

    private static void wheel(ModelPart part, float spin, float steer, PoseStack pose, VertexConsumer vc, int light, int overlay) {
        part.xRot = spin;
        part.yRot = steer;
        part.render(pose, vc, light, overlay);
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity car) {
        return TEXTURE;
    }
}
