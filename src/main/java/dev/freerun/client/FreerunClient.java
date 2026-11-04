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
    private static KeyMapping forksUpKey;
    private static KeyMapping forksDownKey;
    private static int lastForkDir;
    private static KeyMapping skillsKey;
    private static boolean skillsKeyWasDown;
    private static boolean hornWasDown;
    private static boolean pickerPending;

    /** Whether a key event is the skill tree key (used to close the screen with the same key). */
    public static boolean skillsKeyMatches(int keyCode, int scanCode) {
        return skillsKey != null && skillsKey.matches(keyCode, scanCode);
    }

    /**
     * Raw key check for the skill tree key. P is also vanilla's Social Interactions key, and when two
     * bindings share a key only one of them receives presses, so we read the key directly as well.
     */
    private static boolean skillsKeyDown(net.minecraft.client.Minecraft client) {
        if (skillsKey.isDown()) return true;
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(skillsKey);
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(client.getWindow().getWindow(), key.getValue());
    }

    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                dev.freerun.food.FoodRegistry.COFFEE_PLANT, net.minecraft.client.renderer.RenderType.cutout());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                dev.freerun.food.FoodRegistry.COFFEE_MAKER, net.minecraft.client.renderer.RenderType.translucent());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                dev.freerun.economy.EconomyRegistry.SLUSHIE_MACHINE, net.minecraft.client.renderer.RenderType.translucent());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(net.minecraft.client.renderer.RenderType.cutout(),
                dev.freerun.factory.FactoryRegistry.PRESS_BRAKE, dev.freerun.factory.FactoryRegistry.PRESS_BRAKE_FRAME,
                dev.freerun.economy.EconomyRegistry.GAS_PUMP, dev.freerun.economy.EconomyRegistry.ROLLER_GRILL,
                dev.freerun.food.FoodRegistry.STORE_SHELF);

        // Guns, cars, stores, and the new folks at the factory
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(
                dev.freerun.item.ItemRegistry.MUZZLE_FLASH, dev.freerun.client.gun.MuzzleFlashParticle.Provider::new);
        EntityRendererRegistry.register(dev.freerun.item.ItemRegistry.BULLET_ENTITY, dev.freerun.client.gun.BulletRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.vehicle.CarModel.LAYER, dev.freerun.client.vehicle.CarModel::createLayer);
        EntityRendererRegistry.register(dev.freerun.vehicle.VehicleRegistry.CAR, dev.freerun.client.vehicle.CarRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.BigBoudinModel.LAYER, dev.freerun.client.mobs.BigBoudinModel::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.JohnnyJamesPatrickRenderer.LAYER, dev.freerun.client.mobs.EthanModel::createLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.BIG_BOUDIN, dev.freerun.client.mobs.BigBoudinRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.JOHNNY_JAMES_PATRICK, dev.freerun.client.mobs.JohnnyJamesPatrickRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.JOHNNY_SIM, dev.freerun.client.mobs.JohnnySimRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.COOP, dev.freerun.client.mobs.CoopRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(dev.freerun.net.ShopPayloads.Open.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new dev.freerun.client.economy.ShopScreen(
                        payload.entityId(), dev.freerun.economy.Shops.Shop.byId(payload.shop())))));
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> dev.freerun.client.economy.CashHud.render(guiGraphics));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.freerun.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.freerun"));
        forksUpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.freerun.forks_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.freerun"));
        skillsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.freerun.skills", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.freerun"));
        forksDownKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.freerun.forks_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.freerun"));

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
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.FlavusModel.LAYER, dev.freerun.client.mobs.FlavusModel::createBodyLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.FLAVUS,
                dev.freerun.client.mobs.FlavusRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.NpcHairLayer.SLATER, dev.freerun.client.mobs.NpcHairLayer::slaterLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.NpcHairLayer.NICK, dev.freerun.client.mobs.NpcHairLayer::nickLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.ROADHOUSE_NPC,
                dev.freerun.client.mobs.RoadhouseNpcRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.MeowbeanModel.LAYER, dev.freerun.client.mobs.MeowbeanModel::createBodyLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.MEOWBEAN,
                dev.freerun.client.mobs.MeowbeanRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.SoldierRegistry.SEAT,
                net.minecraft.client.renderer.entity.NoopRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.SteelPartRenderer.SHEET, dev.freerun.client.factory.SteelPartRenderer::sheetLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.SteelPartRenderer.PLATE, dev.freerun.client.factory.SteelPartRenderer::plateLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.SteelPartRenderer.BRACKET, dev.freerun.client.factory.SteelPartRenderer::bracketLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.SteelPartRenderer.CHANNEL, dev.freerun.client.factory.SteelPartRenderer::channelLayer);
        EntityRendererRegistry.register(dev.freerun.factory.FactoryRegistry.STEEL_PART,
                dev.freerun.client.factory.SteelPartRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.ForkliftModel.LAYER, dev.freerun.client.factory.ForkliftModel::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.CraneModel.LAYER, dev.freerun.client.factory.CraneModel::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.factory.ForkliftDriverModel.LAYER, dev.freerun.client.factory.ForkliftDriverModel::createLayer);
        EntityRendererRegistry.register(dev.freerun.factory.FactoryRegistry.FORKLIFT,
                dev.freerun.client.factory.ForkliftRenderer::new);
        EntityRendererRegistry.register(dev.freerun.factory.FactoryRegistry.CRANE,
                dev.freerun.client.factory.CraneRenderer::new);
        EntityRendererRegistry.register(dev.freerun.factory.FactoryRegistry.FORKLIFT_DRIVER,
                dev.freerun.client.factory.ForkliftDriverRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.LivingStoneModel.LAYER, dev.freerun.client.mobs.LivingStoneModel::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
                dev.freerun.client.mobs.EthanModel.LAYER, dev.freerun.client.mobs.EthanModel::createLayer);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.LIVING_STONE, dev.freerun.client.mobs.LivingStoneRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.ETHAN, dev.freerun.client.mobs.EthanRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.GANGSTER, dev.freerun.client.mobs.GangsterRenderer::new);
        EntityRendererRegistry.register(dev.freerun.mobs.MoreMobs.GANG_TURF, net.minecraft.client.renderer.entity.NoopRenderer::new);
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
            // Skill tree (P), and the career picker the server asks for on your first visit.
            boolean skillsDown = client.player != null && skillsKeyDown(client);
            if (skillsDown && !skillsKeyWasDown && client.screen == null) {
                client.setScreen(dev.freerun.career.CareerClientState.career == 0
                        ? new dev.freerun.client.career.CareerSelectScreen(null)
                        : new dev.freerun.client.career.SkillTreeScreen());
            }
            skillsKeyWasDown = skillsDown;
            while (skillsKey.consumeClick()) { }
            if (pickerPending && client.screen == null && client.player != null) {
                pickerPending = false;
                if (dev.freerun.career.CareerClientState.career == 0) {
                    client.setScreen(new dev.freerun.client.career.CareerSelectScreen(null));
                }
            }
            dev.freerun.client.vehicle.CarEngineSound.tickAll(client);
            // In a car, the forks-up key is the horn.
            boolean inCar = client.player != null && client.player.getVehicle() instanceof dev.freerun.vehicle.CarEntity;
            boolean horn = inCar && forksUpKey.isDown();
            if (horn && !hornWasDown && ClientPlayNetworking.canSend(dev.freerun.net.ForkPayload.TYPE)) {
                ClientPlayNetworking.send(new dev.freerun.net.ForkPayload(1));
            }
            hornWasDown = horn;
            // Forklift fork controls: tell the server whenever the held direction changes.
            if (client.player != null && client.player.getVehicle() instanceof dev.freerun.factory.ForkliftEntity) {
                int dir = (forksUpKey.isDown() ? 1 : 0) - (forksDownKey.isDown() ? 1 : 0);
                if (dir != lastForkDir && ClientPlayNetworking.canSend(dev.freerun.net.ForkPayload.TYPE)) {
                    ClientPlayNetworking.send(new dev.freerun.net.ForkPayload(dir));
                    lastForkDir = dir;
                }
            } else {
                lastForkDir = 0;
            }
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

        ClientPlayNetworking.registerGlobalReceiver(dev.freerun.net.CareerPayloads.Sync.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    int oldLevel = dev.freerun.career.CareerClientState.level;
                    boolean known = dev.freerun.career.CareerClientState.owner != null;
                    dev.freerun.career.CareerClientState.owner =
                            context.client().player != null ? context.client().player.getUUID() : null;
                    dev.freerun.career.CareerClientState.career = payload.career() & 0xFF;
                    dev.freerun.career.CareerClientState.completed = payload.career() >>> 8;
                    dev.freerun.career.CareerClientState.level = payload.level();
                    dev.freerun.career.CareerClientState.xp = payload.xp();
                    dev.freerun.career.CareerClientState.skills = java.util.List.copyOf(payload.skills());
                    if (payload.gained() > 0) {
                        dev.freerun.career.CareerClientState.lastGain = payload.gained();
                        dev.freerun.career.CareerClientState.lastReason = payload.reason();
                        dev.freerun.career.CareerClientState.lastGainMillis = System.currentTimeMillis();
                    }
                    if (known && payload.level() > oldLevel) {
                        dev.freerun.career.CareerClientState.levelUpMillis = System.currentTimeMillis();
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(dev.freerun.net.CareerPayloads.OpenSelect.TYPE, (payload, context) ->
                context.client().execute(() -> pickerPending = true));
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> dev.freerun.client.career.CareerHud.render(guiGraphics));
        // Chud-Maxing: a much bigger body drawn over affected players.
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(ChudLayer.LAYER, ChudLayer::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                        helper.register(new ChudLayer(playerRenderer, context.bakeLayer(ChudLayer.LAYER)));
                    }
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            dev.freerun.career.CareerClientState.owner = null;
            dev.freerun.career.CareerClientState.career = 0;
            dev.freerun.career.CareerClientState.completed = 0;
            dev.freerun.career.CareerClientState.level = 1;
            dev.freerun.career.CareerClientState.xp = 0;
            dev.freerun.career.CareerClientState.skills = java.util.List.of();
            pickerPending = false;
            AnimState.clearAll();
            CONTROLLER.reset();
        });
    }
}
