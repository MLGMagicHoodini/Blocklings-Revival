package com.willr27.blocklings.mixin;

import com.willr27.blocklings.entity.blockling.experience.BlocklingXpHandler;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric: give Bottle o' Enchanting XP to a nearby owned blockling instead of spawning orbs.
 */
@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin
{
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void blocklings$absorbBottle(HitResult result, CallbackInfo ci)
    {
        ThrownExperienceBottle self = (ThrownExperienceBottle) (Object) this;
        if (self.level().isClientSide())
        {
            return;
        }

        if (!BlocklingXpHandler.tryAbsorbThrownBottle(self))
        {
            return;
        }

        self.discard();
        ci.cancel();
    }
}
