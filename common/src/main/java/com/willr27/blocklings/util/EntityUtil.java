package com.willr27.blocklings.util;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.willr27.blocklings.util.Memoized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class EntityUtil {
    @Nullable
    public static Level latestWorld;

    @Nonnull
    public static final Memoized<Map<ResourceLocation, Entity>> VALID_ATTACK_TARGETS = Memoized.of(EntityUtil::createValidAttackTargetsMap);

    @Nonnull
    public static Map<ResourceLocation, Entity> createValidAttackTargetsMap() {
        Blocklings.LOGGER.info("Creating valid attack targets map.");
        if (latestWorld == null) {
            Blocklings.LOGGER.error("Tried to initialise valid attack targets list before a world was loaded!");
            return new TreeMap<>();
        }

        Map<ResourceLocation, Entity> validAttackTargets = new TreeMap<>();
        for (ResourceLocation entry : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            Entity entity = BuiltInRegistries.ENTITY_TYPE.get(entry).create(latestWorld);
            if (entity != null) {
                if (isValidAttackTarget(entity)) {
                    validAttackTargets.put(entry, entity);
                }
            } else {
                Blocklings.LOGGER.warn("Failed to create entity: {}", entry);
            }
        }
        return validAttackTargets;
    }

    @Nonnull
    public static Entity create(@Nonnull ResourceLocation type, @Nonnull Level world) {
        return Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.get(type).create(world));
    }

    public static boolean isValidAttackTarget(@Nonnull Entity entity) {
        if (!(entity instanceof Mob)) {
            return false;
        }
        if (entity instanceof FlyingMob) {
            return false;
        }
        return !(entity instanceof WaterAnimal);
    }

    public static boolean canSee(@Nonnull LivingEntity entity, @Nonnull BlockPos blockPos) {
        Vec3 entityPos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
        for (double x = 0.05; x < 1.0; x += 0.9) {
            for (double y = 0.05; y < 1.0; y += 0.9) {
                for (double z = 0.05; z < 1.0; z += 0.9) {
                    Vec3 targetPos = new Vec3(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
                    BlockHitResult result = entity.level().clip(new ClipContext(entityPos, targetPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, entity));
                    if (result.getType() != HitResult.Type.MISS && result.getBlockPos().equals(blockPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInRange(@Nonnull LivingEntity entity, @Nonnull BlockPos blockPos, float rangeSq) {
        return (float) BlockUtil.distanceSq(entity.blockPosition(), blockPos) < rangeSq;
    }

    @Nullable
    public static Path createPathTo(@Nonnull Mob entity, @Nonnull BlockPos blockPos) {
        return createPathTo(entity, blockPos, 0);
    }

    @Nullable
    public static Path createPathTo(@Nonnull Mob entity, @Nonnull BlockPos blockPos, float stopDistanceSq) {
        Path closestPath = null;
        double closestDistanceSq = Double.MAX_VALUE;

        Path path = entity.getNavigation().createPath(blockPos, 0);
        if (path != null) {
            closestPath = path;
            closestDistanceSq = BlockUtil.distanceSq(blockPos, path.getTarget());
            if (!path.getTarget().equals(blockPos.above()) && closestDistanceSq < stopDistanceSq) {
                return closestPath;
            } else if (stopDistanceSq != 0) {
                closestDistanceSq = stopDistanceSq;
            }
        }

        for (BlockPos adjacentPos : BlockUtil.getSurroundingBlockPositions(blockPos)) {
            path = entity.getNavigation().createPath(adjacentPos, 0);
            if (path != null) {
                double distanceSq = BlockUtil.distanceSq(blockPos, path.getTarget());
                if (distanceSq < closestDistanceSq) {
                    closestPath = path;
                    closestDistanceSq = distanceSq;
                    if (closestDistanceSq < stopDistanceSq) {
                        return closestPath;
                    }
                }
            }
        }
        return stopDistanceSq > 0 ? null : closestPath;
    }
}
