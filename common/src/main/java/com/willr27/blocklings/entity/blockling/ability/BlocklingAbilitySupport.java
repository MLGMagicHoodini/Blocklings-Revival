package com.willr27.blocklings.entity.blockling.ability;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.util.RandomSource;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Consumer;

public final class BlocklingAbilitySupport
{
    private BlocklingAbilitySupport()
    {
    }

    public static boolean hasFamily(@Nonnull BlocklingEntity blockling, @Nonnull TypeFamily family)
    {
        return TypeFamily.from(blockling.getNaturalBlocklingType()) == family
                || TypeFamily.from(blockling.getBlocklingType()) == family;
    }

    public static boolean passChance(double chance)
    {
        return Math.random() < chance;
    }

    public static boolean passChance(@Nonnull RandomSource random, double chance)
    {
        return random.nextDouble() < chance;
    }

    public static boolean isBareDirt(@Nonnull BlockState state)
    {
        return state.is(BlockTags.DIRT) && !state.is(Blocks.GRASS_BLOCK);
    }

    public static void forEachAllyInRange(@Nonnull BlocklingEntity blockling, double range, @Nonnull Consumer<LivingEntity> action)
    {
        UUID ownerId = blockling.getOwnerUUID();
        AABB box = AABB.ofSize(blockling.position(), range * 2, range * 2, range * 2);

        for (LivingEntity entity : blockling.level().getEntitiesOfClass(LivingEntity.class, box))
        {
            if (entity == blockling)
            {
                continue;
            }

            if (ownerId != null && entity instanceof Player player && ownerId.equals(player.getUUID()))
            {
                action.accept(entity);
                continue;
            }

            if (entity instanceof BlocklingEntity ally && ownerId != null && ownerId.equals(ally.getOwnerUUID()))
            {
                action.accept(entity);
            }
        }
    }

    public static void applyEffectToAllies(@Nonnull BlocklingEntity blockling, double range, @Nonnull MobEffectInstance effect, int ownerDuration)
    {
        blockling.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(), false, false, true));

        LivingEntity owner = blockling.getOwner();
        if (owner != null && owner.distanceToSqr(blockling) <= range * range)
        {
            owner.addEffect(new MobEffectInstance(effect.getEffect(), ownerDuration, effect.getAmplifier(), false, false, true));
        }

        forEachAllyInRange(blockling, range, ally ->
        {
            if (ally != owner)
            {
                ally.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(), false, false, true));
            }
        });
    }

    public static void forEachBlockInRadius(@Nonnull BlockPos center, int radius, @Nonnull Consumer<BlockPos> action)
    {
        for (int x = -radius; x <= radius; x++)
        {
            for (int y = -radius; y <= radius; y++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    action.accept(center.offset(x, y, z));
                }
            }
        }
    }

    public static boolean setBlock(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state)
    {
        if (!level.isLoaded(pos))
        {
            return false;
        }

        return level.setBlock(pos, state, 3);
    }

    public static void pullItems(@Nonnull BlocklingEntity blockling, double range, double speed)
    {
        AABB box = blockling.getBoundingBox().inflate(range);

        for (ItemEntity item : blockling.level().getEntitiesOfClass(ItemEntity.class, box))
        {
            if (!item.isAlive() || item.hasPickUpDelay())
            {
                continue;
            }

            Vec3 delta = blockling.position().subtract(item.position()).normalize().scale(speed);
            item.setDeltaMovement(item.getDeltaMovement().add(delta));
        }
    }

    public static void collectNearbyOres(@Nonnull BlocklingEntity blockling, double range)
    {
        AABB box = blockling.getBoundingBox().inflate(range);

        for (ItemEntity item : blockling.level().getEntitiesOfClass(ItemEntity.class, box))
        {
            if (!item.isAlive() || item.hasPickUpDelay())
            {
                continue;
            }

            ItemStack stack = item.getItem();
            if (!BlockUtil.isOre(stack.getItem()))
            {
                continue;
            }

            ItemStack remainder = blockling.getEquipment().addItem(stack.copy());
            if (remainder.isEmpty())
            {
                item.discard();
            }
            else
            {
                item.setItem(remainder);
            }
        }
    }

    public static void spawnParticlesAt(@Nonnull ServerLevel level, @Nonnull BlockPos pos)
    {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                3, 0.2, 0.2, 0.2, 0.01);
    }

    public static boolean isWoodType(@Nonnull BlocklingType type)
    {
        return TypeFamily.from(type) == TypeFamily.WOOD;
    }
}
