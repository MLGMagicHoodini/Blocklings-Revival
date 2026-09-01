package com.willr27.blocklings.mixin;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.experience.BlocklingXpHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fabric equivalent of NeoForge {@code LivingExperienceDropEvent}: convert kill XP to the
 * blockling instead of spawning world orbs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityExperienceMixin
{
    @ModifyArg(
            method = "dropExperience",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
            index = 2)
    private int blocklings$absorbKillXp(int xp)
    {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getLastHurtByMob() instanceof BlocklingEntity blockling)
        {
            BlocklingXpHandler.absorbKillExperience(blockling, xp);
            return 0;
        }

        return xp;
    }
}
