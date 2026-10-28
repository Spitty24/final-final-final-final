package dev.freerun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.freerun.FreerunConfig;
import dev.freerun.ParkourState;
import dev.freerun.client.evil.CaughtEffects;
import dev.freerun.client.evil.GlitchOverlay;
import dev.freerun.client.evil.PursuerRenderer;
import dev.freerun.evil.EvilRegistry;
import dev.freerun.net.CaughtPayload;
import dev.freerun.net.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class FreerunClient implements ClientModInitializer {
    public static final ParkourController CONTROLLER = new ParkourController();
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.freerun.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.freerun"));

        EntityRendererRegistry.register(EvilRegistry.PURSUER, PursuerRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.US_SOLDIER,
                dev.freerun.client.mobs.SoldierRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.FRENCH_SOLDIER,
                dev.freerun.client.mobs.SoldierRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.GRUMBUS,
                dev.freerun.client.mobs.GrumbusRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.WingusModel.LAYER, dev.freerun.client.mobs.WingusModel::createBodyLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.WINGUS,
                dev.freerun.client.mobs.WingusRenderer::new);
        EntityRendererRegistry.register(dev.freerun.item.ItemRegistry.NUKE_PROJECTILE,
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        net.minecraft.client.gui.screens.MenuScreens.register(
                dev.freerun.menu.MenuRegistry.BIG_CRAFTING, dev.freerun.client.menu.BigCraftingScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(CaughtPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CaughtEffects.handle(payload.kind())));

        // Draw the glitch overlay on top of the HUD.
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> GlitchOverlay.render(guiGraphics));

        // Runs before the player ticks, so velocities we set are used for this tick's movement.
        ClientTickEvents.START_CLIENT_TICK.register(CONTROLLER::tick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) ClientClock.tick();
            while (toggleKey.consumeClick()) {
                FreerunConfig cfg = FreerunConfig.get();
                cfg.enabled = !cfg.enabled;
                FreerunConfig.save();
                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.literal("Parkour " + (cfg.enabled ? "enabled" : "disabled")), true);
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            if (context.client().level == null) return;
            Entity e = context.client().level.getEntity(payload.entityId());
            if (e instanceof Player p) {
                AnimState.get(p.getUUID()).set(ParkourState.byId(payload.state()), ClientClock.now());
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AnimState.clearAll();
            CONTROLLER.reset();
        });
    }
}
