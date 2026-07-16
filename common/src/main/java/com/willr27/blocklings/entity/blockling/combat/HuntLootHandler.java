package com.willr27.blocklings.entity.blockling.combat;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.skill.skills.CombatSkills;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Collects hunt kills into the blockling inventory and applies Animal/Monster Hunter doubling.
 * <p>
 * Doubling must happen on the original drop counts before {@code addItem}, otherwise a full
 * inventory insert returns an empty remainder and the bonus is lost (legacy 1.18 bug).
 */
public final class HuntLootHandler
{
    private HuntLootHandler()
    {
    }

    public static boolean shouldHandleHuntLoot(@Nonnull BlocklingEntity blockling)
    {
        return blockling.getSkills().getSkill(CombatSkills.HUNTER).isBought() && blockling.wasLastAttackHunt;
    }

    public static boolean shouldDoubleLoot(@Nonnull BlocklingEntity blockling, @Nonnull LivingEntity killed)
    {
        if (blockling.getSkills().getSkill(CombatSkills.ANIMAL_HUNTER).isBought() && killed instanceof Animal)
        {
            return true;
        }

        return blockling.getSkills().getSkill(CombatSkills.MONSTER_HUNTER).isBought() && killed instanceof Monster;
    }

    /**
     * Vacuums hunt drops into the blockling. Caller must suppress vanilla world drops afterwards.
     */
    public static void collectHuntDrops(@Nonnull BlocklingEntity blockling, @Nonnull LivingEntity killed,
                                        @Nonnull Collection<ItemEntity> drops)
    {
        for (ItemEntity itemEntity : drops)
        {
            collectHuntDrop(blockling, killed, itemEntity.getItem());
        }
    }

    /**
     * Vacuums hunt item stacks into the blockling. Caller must suppress vanilla world drops afterwards.
     */
    public static void collectHuntStacks(@Nonnull BlocklingEntity blockling, @Nonnull LivingEntity killed,
                                         @Nonnull Collection<ItemStack> drops)
    {
        for (ItemStack drop : drops)
        {
            collectHuntDrop(blockling, killed, drop);
        }
    }

    private static void collectHuntDrop(@Nonnull BlocklingEntity blockling, @Nonnull LivingEntity killed,
                                        @Nonnull ItemStack drop)
    {
        ItemStack stack = drop.copy();
        if (stack.isEmpty())
        {
            return;
        }

        if (shouldDoubleLoot(blockling, killed))
        {
            addDoubled(blockling, stack);
        }
        else
        {
            addOrDrop(blockling, stack);
        }
    }

    private static void addDoubled(@Nonnull BlocklingEntity blockling, @Nonnull ItemStack stack)
    {
        long doubled = (long) stack.getCount() * 2L;
        int max = Math.max(1, stack.getMaxStackSize());

        while (doubled > 0L)
        {
            ItemStack part = stack.copy();
            int partCount = (int) Math.min(doubled, max);
            part.setCount(partCount);
            addOrDrop(blockling, part);
            doubled -= partCount;
        }
    }

    private static void addOrDrop(@Nonnull BlocklingEntity blockling, @Nonnull ItemStack stack)
    {
        ItemStack remainder = blockling.getEquipment().addItem(stack);
        if (!remainder.isEmpty())
        {
            blockling.dropItemStack(remainder);
        }
    }
}
