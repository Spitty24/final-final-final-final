package dev.freerun.mixin.client;

import dev.freerun.client.StreamChat;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the stream chat's on/off and size to Video Settings. */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {
    private VideoSettingsScreenMixin(Screen last, Options options, Component title) {
        super(last, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void freerun$streamChat(CallbackInfo ci) {
        if (this.list != null) this.list.addSmall(StreamChat.options());
    }
}
