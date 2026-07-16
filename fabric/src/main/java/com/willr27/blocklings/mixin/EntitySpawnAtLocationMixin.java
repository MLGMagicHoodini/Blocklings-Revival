package com.willr27.blocklings.mixin;

import com.willr27.blocklings.platform.HuntLootCapture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Intercepts item spawns while a hunt-loot capture is active so drops go to the blockling instead.
 */
@Mixin(Entity.class)
public abstract class EntitySpawnAtLocationMixin
{
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void blocklings$captureHuntSpawn(ItemStack stack, CallbackInfoReturnable<ItemEntity> cir)
    {
        List<ItemStack> huntDrops = HuntLootCapture.drops();
        if (huntDrops == null || stack.isEmpty())
        {
            return;
        }

        huntDrops.add(stack.copy());
        cir.setReturnValue(null);
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void blocklings$captureHuntSpawnOffset(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir)
    {
        List<ItemStack> huntDrops = HuntLootCapture.drops();
        if (huntDrops == null || stack.isEmpty())
        {
            return;
        }

        huntDrops.add(stack.copy());
        cir.setReturnValue(null);
    }
}
