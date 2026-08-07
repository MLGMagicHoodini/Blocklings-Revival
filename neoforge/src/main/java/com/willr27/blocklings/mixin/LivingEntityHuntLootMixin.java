package com.willr27.blocklings.mixin;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.combat.HuntLootHandler;
import com.willr27.blocklings.platform.HuntLootCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric equivalent of NeoForge {@code LivingDropsEvent} for hunt loot vacuum + doubling.
 * Works with {@link EntitySpawnAtLocationMixin} because vanilla has no {@code captureDrops} API.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHuntLootMixin
{
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void blocklings$beginHuntCapture(ServerLevel level, DamageSource damageSource, CallbackInfo ci)
    {
        HuntLootCapture.clear();

        if (!(damageSource.getEntity() instanceof BlocklingEntity blockling))
        {
            return;
        }

        if (!HuntLootHandler.shouldHandleHuntLoot(blockling))
        {
            return;
        }

        HuntLootCapture.begin(blockling, new ArrayList<>());
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void blocklings$finishHuntCapture(ServerLevel level, DamageSource damageSource, CallbackInfo ci)
    {
        List<ItemStack> drops = HuntLootCapture.drops();
        BlocklingEntity blockling = HuntLootCapture.killer();
        HuntLootCapture.clear();

        if (drops == null || blockling == null || drops.isEmpty())
        {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        HuntLootHandler.collectHuntStacks(blockling, self, drops);
    }
}
