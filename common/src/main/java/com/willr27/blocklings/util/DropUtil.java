package com.willr27.blocklings.util;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilitySupport;
import com.willr27.blocklings.entity.blockling.ability.TypeFamily;
import com.willr27.blocklings.entity.blockling.skill.skills.MiningSkills;
import net.minecraft.core.Holder;
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
        if (!(world instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        try {
            ItemStack mergedStack = EnchantmentCompat.mergeEnchantmentsPreservingComponents(mainStack, offStack);
            applyLapisFortuneBonus(blockling, mergedStack);

            var blockState = world.getBlockState(blockPos);

            // Farming: empty tool for loot so Silk Touch on hoes doesn't suppress crop products
            // (vanilla wheat loot skips the wheat pool when Silk Touch is present).
            ItemStack lootTool = context == Context.FARMING ? ItemStack.EMPTY : mergedStack;

            LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                    .withParameter(LootContextParams.TOOL, lootTool)
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
        } catch (Throwable t) {
            Blocklings.LOGGER.warn("Enchanted tool loot failed at {}; falling back without tool enchants.", blockPos, t);
            try {
                var blockState = world.getBlockState(blockPos);
                LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, blockling)
                        .withParameter(LootContextParams.BLOCK_STATE, blockState);
                return blockState.getDrops(lootParams);
            } catch (Throwable ignored) {
                return List.of();
            }
        }
    }

    /**
     * Keeps Apotheosis gems/affixes on the tool copy; only merges higher enchant levels from the other hand.
     */

    private static void applyLapisFortuneBonus(@Nonnull BlocklingEntity blockling, @Nonnull ItemStack mergedStack) {
        if (mergedStack.isEmpty()) {
            return;
        }

        if (!(BlocklingsConfig.COMMON.abilities.enabled.get()
                && BlocklingsConfig.COMMON.abilities.lapis.passiveEnabled.get()
                && BlocklingAbilitySupport.hasFamily(blockling, TypeFamily.LAPIS))) {
            return;
        }

        try {
            List<Holder<Enchantment>> holders = new ArrayList<>();
            for (Holder<Enchantment> holder : mergedStack.getEnchantments().keySet()) {
                if (holder != null && holder.is(Enchantments.FORTUNE)) {
                    holders.add(holder);
                }
            }
            for (Holder<Enchantment> holder : holders) {
                int level = mergedStack.getEnchantments().getLevel(holder) + 1;
                EnchantmentHelper.updateEnchantments(mergedStack, map -> map.set(holder, level));
            }
        } catch (Throwable ignored) {
        }
    }

    public enum Context {
        MINING,
        WOODCUTTING,
        FARMING
    }
}
