package com.willr27.blocklings.util;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilitySupport;
import com.willr27.blocklings.entity.blockling.ability.TypeFamily;
import com.willr27.blocklings.entity.blockling.skill.skills.MiningSkills;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DropUtil {
    @Nonnull
    public static List<ItemStack> getDrops(@Nonnull Context context, @Nonnull BlocklingEntity blockling, @Nonnull BlockPos blockPos,
                                           @Nonnull ItemStack mainStack, @Nonnull ItemStack offStack) {
        Level world = blockling.level();
        ItemStack mergedStack = mainStack.copy();

        for (Holder<Enchantment> holder : offStack.getEnchantments().keySet()) {
            int mainLevel = mergedStack.getEnchantments().getLevel(holder);
            int offLevel = offStack.getEnchantments().getLevel(holder);
            EnchantmentHelper.updateEnchantments(mergedStack, map -> map.set(holder, Math.max(mainLevel, offLevel)));
        }

        if (BlocklingsConfig.COMMON.abilities.enabled.get()
                && BlocklingsConfig.COMMON.abilities.lapis.passiveEnabled.get()
                && BlocklingAbilitySupport.hasFamily(blockling, TypeFamily.LAPIS))
        {
            for (Holder<Enchantment> holder : mergedStack.getEnchantments().keySet()) {
                if (holder.is(Enchantments.FORTUNE)) {
                    int level = mergedStack.getEnchantments().getLevel(holder) + 1;
                    EnchantmentHelper.updateEnchantments(mergedStack, map -> map.set(holder, level));
                }
            }
        }

        if (!(world instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        var blockState = world.getBlockState(blockPos);
        LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                .withParameter(LootContextParams.TOOL, mergedStack)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, blockling)
                .withParameter(LootContextParams.BLOCK_STATE, blockState);
        List<ItemStack> drops = blockState.getDrops(lootParams);

        if (blockling.getSkills().getSkill(MiningSkills.HOT_HANDS).isBought()) {
            List<ItemStack> smeltedDrops = new ArrayList<>();
            for (ItemStack drop : drops) {
                SingleRecipeInput input = new SingleRecipeInput(drop);
                RecipeHolder<SmeltingRecipe> recipe = serverLevel.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, input, serverLevel)
                        .orElse(null);
                if (recipe != null) {
                    smeltedDrops.add(recipe.value().getResultItem(serverLevel.registryAccess()));
                } else {
                    smeltedDrops.add(drop);
                }
            }
            return smeltedDrops;
        }

        return drops;
    }

    public enum Context {
        MINING,
        WOODCUTTING,
        FARMING
    }
}
