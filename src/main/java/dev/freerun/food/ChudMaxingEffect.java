package dev.freerun.food;

import dev.freerun.Freerun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Chud-Maxing, from the Chud Sausage. You get visibly, dramatically heavier (the client draws a
 * much bigger body on anyone who has it), a bit slower, harder to knock around, and a little
 * tougher. The speed modifier doubles as the signal other players' clients use to draw you big,
 * since movement speed is synced to everyone who can see you.
 */
public class ChudMaxingEffect extends MobEffect {
    public static final ResourceLocation SPEED_MOD = Freerun.id("effect.chud_maxing.speed");

    public ChudMaxingEffect() {
        super(MobEffectCategory.NEUTRAL, 0xB98B5E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MOD, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Freerun.id("effect.chud_maxing.knockback"), 0.4,
                AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MAX_HEALTH, Freerun.id("effect.chud_maxing.health"), 4.0,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
