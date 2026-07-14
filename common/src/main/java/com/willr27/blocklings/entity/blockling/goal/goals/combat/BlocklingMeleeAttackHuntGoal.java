package com.willr27.blocklings.entity.blockling.goal.goals.combat;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Attacks the nearest entity to the blockling using melee.
 */
public class BlocklingMeleeAttackHuntGoal extends BlocklingMeleeAttackGoal
{
    /**
     * How far (in blocks, horizontally) a hunt target may be from the owner when the blockling
     * also has an enabled Follow task. Keeps a following blockling hunting around its owner
     * instead of chasing prey across the map and never coming back.
     */
    private static final double HUNT_LEASH_FROM_OWNER = 12.0;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingMeleeAttackHuntGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        whitelists.get(0).setEntry(com.willr27.blocklings.util.RegistryUtil.entityTypeId(EntityType.VILLAGER), false, false);
    }

    @Override
    public boolean tryRecalcTarget()
    {
        if (!blockling.isTame())
        {
            return false;
        }

        if (isTargetValid() && isWithinOwnerHuntLeash(getTarget()))
        {
            return true;
        }

        for (Entity entity : world.getEntities(blockling, new AABB(blockling.position().add(-10.0, -10.0, -10.0), blockling.position().add(10.0, 10.0, 10.0))))
        {
            if (entity instanceof LivingEntity)
            {
                LivingEntity livingEntity = (LivingEntity) entity;

                if (isValidTarget(livingEntity) && isWithinOwnerHuntLeash(livingEntity))
                {
                    setTarget(livingEntity);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return true if the entity is close enough to the owner to hunt, or if there is no active
     *         Follow task (in which case the blockling is free to roam and hunt anywhere).
     */
    private boolean isWithinOwnerHuntLeash(@Nullable LivingEntity entity)
    {
        if (entity == null)
        {
            return false;
        }

        LivingEntity owner = blockling.getOwner();
        if (owner == null || !blockling.getTasks().hasEnabledFollowTask())
        {
            return true;
        }

        double dx = owner.getX() - entity.getX();
        double dz = owner.getZ() - entity.getZ();
        return (dx * dx + dz * dz) <= HUNT_LEASH_FROM_OWNER * HUNT_LEASH_FROM_OWNER;
    }

    @Override
    protected void attack(@Nonnull LivingEntity target, @Nonnull BlocklingHand attackingHand)
    {
        blockling.wasLastAttackHunt = true;

        super.attack(target, attackingHand);
    }
}
