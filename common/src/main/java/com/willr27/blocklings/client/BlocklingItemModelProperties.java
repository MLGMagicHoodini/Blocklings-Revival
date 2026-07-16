package com.willr27.blocklings.client;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.item.BlocklingItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shared helpers for the {@code blocklings:type} item model property.
 * Registration must happen in each loader module (AT / access widener for {@code ItemProperties.register}).
 */
public final class BlocklingItemModelProperties
{
    public static final ResourceLocation TYPE_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "type");

    private BlocklingItemModelProperties()
    {
    }

    /**
     * Unclamped property: overrides {@link ClampedItemPropertyFunction#call} so values are not forced into 0–1.
     */
    @Nonnull
    public static ClampedItemPropertyFunction createTypeProperty()
    {
        return new UnclampedTypeProperty();
    }

    /**
     * Type index for model overrides (0 = grass … 17 = glowstone).
     */
    public static float readTypeIndex(@Nonnull ItemStack stack)
    {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null && data.value() > 0)
        {
            return Math.max(0, data.value() - 1);
        }

        CompoundTag tag = BlocklingItem.getDataTag(stack);
        if (tag.contains("entity"))
        {
            CompoundTag entity = tag.getCompound("entity");
            if (entity.contains("blockling"))
            {
                String key = entity.getCompound("blockling").getString("type");
                if (!key.isEmpty())
                {
                    int index = BlocklingType.TYPES.indexOf(BlocklingType.find(key));
                    return Math.max(0, index);
                }
            }
        }

        return 0.0F;
    }

    private static final class UnclampedTypeProperty implements ClampedItemPropertyFunction
    {
        @Override
        public float call(@Nonnull ItemStack stack, @Nullable ClientLevel level,
                          @Nullable LivingEntity entity, int seed)
        {
            return readTypeIndex(stack);
        }

        @Override
        public float unclampedCall(@Nonnull ItemStack stack, @Nullable ClientLevel level,
                                   @Nullable LivingEntity entity, int seed)
        {
            return readTypeIndex(stack);
        }
    }
}
