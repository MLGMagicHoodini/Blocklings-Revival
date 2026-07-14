package com.willr27.blocklings.util;

import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilitySupport;
import com.willr27.blocklings.entity.blockling.ability.TypeFamily;
import com.willr27.blocklings.interop.TinkersConstructProxy;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToolUtil {
    private static final List<Item> WEAPONS = new ArrayList<>();
    private static final List<Item> PICKAXES = new ArrayList<>();
    private static final List<Item> AXES = new ArrayList<>();
    private static final List<Item> HOES = new ArrayList<>();
    private static final List<Item> TOOLS = new ArrayList<>();

    public static void init() {
        WEAPONS.clear();
        PICKAXES.clear();
        AXES.clear();
        HOES.clear();

        WEAPONS.addAll(findAllWeapons());
        PICKAXES.addAll(tagItems(ItemTags.PICKAXES));
        AXES.addAll(tagItems(ItemTags.AXES));
        HOES.addAll(tagItems(ItemTags.HOES));

        TOOLS.clear();
        TOOLS.addAll(WEAPONS);
        TOOLS.addAll(PICKAXES);
        TOOLS.addAll(AXES);
        TOOLS.addAll(HOES);
    }

    private static List<Item> tagItems(net.minecraft.tags.TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag)
                .map(holders -> holders.stream().map(Holder::value).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Nonnull
    private static List<Item> findAllWeapons() {
        List<Item> weapons = tagItems(ItemTags.SWORDS);
        weapons.addAll(TinkersConstructProxy.instance.findAllWeapons());
        return weapons;
    }

    public static boolean isWeapon(@Nonnull ItemStack stack) {
        return isWeapon(stack.getItem());
    }

    public static boolean isWeapon(@Nonnull Item item) {
        return WEAPONS.contains(item);
    }

    public static boolean isPickaxe(@Nonnull ItemStack stack) {
        return isPickaxe(stack.getItem());
    }

    public static boolean isPickaxe(@Nonnull Item item) {
        return PICKAXES.contains(item);
    }

    public static boolean isAxe(@Nonnull ItemStack stack) {
        return isAxe(stack.getItem());
    }

    public static boolean isAxe(@Nonnull Item item) {
        return AXES.contains(item);
    }

    public static boolean isHoe(@Nonnull ItemStack stack) {
        return isHoe(stack.getItem());
    }

    public static boolean isHoe(@Nonnull Item item) {
        return HOES.contains(item);
    }

    public static boolean isTool(@Nonnull ItemStack stack) {
        return isTool(stack.getItem());
    }

    public static boolean isTool(@Nonnull Item item) {
        return TOOLS.contains(item);
    }

    public static boolean isTinkersTool(@Nonnull ItemStack stack) {
        return isTinkersTool(stack.getItem());
    }

    public static boolean isTinkersTool(@Nonnull Item item) {
        return TinkersConstructProxy.instance.isTinkersTool(item);
    }

    public static boolean isUseableTool(@Nonnull ItemStack stack) {
        if (!isTool(stack)) {
            return false;
        }
        if (isTinkersTool(stack)) {
            return !TinkersConstructProxy.instance.isToolBroken(stack);
        }
        return true;
    }

    public static float getDefaultToolAttackSpeed(@Nonnull ItemStack stack) {
        return getToolAttackSpeed(stack, null);
    }

    public static float getToolAttackSpeed(@Nonnull ItemStack stack, @Nullable LivingEntity entity) {
        if (!isUseableTool(stack)) {
            return 4.0f;
        }
        return getAttributeAmount(stack, Attributes.ATTACK_SPEED) + 4.0f;
    }

    public static float getDefaultToolBaseDamage(@Nonnull ItemStack stack) {
        return getToolBaseDamage(stack, null);
    }

    public static float getToolBaseDamage(@Nonnull ItemStack stack, @Nullable LivingEntity entity) {
        if (!isUseableTool(stack)) {
            return 0.0f;
        }
        return getAttributeAmount(stack, Attributes.ATTACK_DAMAGE);
    }

    private static float getAttributeAmount(ItemStack stack, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null || modifiers.modifiers().isEmpty()) {
            modifiers = stack.getItem().components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        }
        if (modifiers == null) {
            return 0.0f;
        }

        double total = 0.0D;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(attribute)) {
                total += entry.modifier().amount();
            }
        }
        return (float) total;
    }

    /**
     * Extra melee damage from weapon enchantments (Sharpness, Smite, Apotheosis, …).
     * Must use a real attacker — {@code mobAttack(null)} crashes on 1.21+.
     */
    public static float getToolEnchantmentDamage(@Nonnull ItemStack stack, @Nonnull LivingEntity attacker, @Nonnull LivingEntity target)
    {
        return EnchantmentCompat.modifyAttackDamage(stack, attacker, target);
    }

    public static float getToolKnockbackLevel(@Nonnull ItemStack stack)
    {
        return EnchantmentCompat.getLevel(stack, Enchantments.KNOCKBACK);
    }

    /**
     * Knockback with vanilla Knockback + any modded knockback effects on the weapon.
     */
    public static float getToolKnockback(@Nonnull ItemStack stack, @Nonnull LivingEntity attacker, @Nonnull Entity target, float baseKnockback)
    {
        return EnchantmentCompat.modifyKnockback(stack, attacker, target, baseKnockback);
    }

    public static float getToolFireAspectLevel(@Nonnull ItemStack stack)
    {
        return EnchantmentCompat.getFireAspectLevel(stack);
    }

    /** Efficiency enchantment level (0 if missing / unreadable). */
    public static float getToolEfficiencyLevel(@Nonnull ItemStack stack)
    {
        return EnchantmentCompat.getLevel(stack, Enchantments.EFFICIENCY);
    }

    public static float getDefaultToolMiningSpeedWithEnchantments(@Nonnull ItemStack stack) {
        return getToolHarvestSpeedWithEnchantments(stack, Blocks.STONE.defaultBlockState());
    }

    public static float getDefaultToolWoodcuttingSpeedWithEnchantments(@Nonnull ItemStack stack) {
        return getToolHarvestSpeedWithEnchantments(stack, Blocks.OAK_LOG.defaultBlockState());
    }

    public static float getDefaultToolFarmingSpeedWithEnchantments(@Nonnull ItemStack stack) {
        return getToolHarvestSpeedWithEnchantments(stack, Blocks.HAY_BLOCK.defaultBlockState());
    }

    public static float getToolHarvestSpeedWithEnchantments(@Nonnull ItemStack stack, @Nonnull BlockState blockState) {
        // Already includes Efficiency / destroy-speed bonuses — do not add Efficiency twice.
        return getToolHarvestSpeed(stack, blockState);
    }

    public static float getDefaultToolMiningSpeed(@Nonnull ItemStack stack) {
        return getToolHarvestSpeed(stack, Blocks.STONE.defaultBlockState());
    }

    public static float getDefaultToolWoodcuttingSpeed(@Nonnull ItemStack stack) {
        return getToolHarvestSpeed(stack, Blocks.OAK_LOG.defaultBlockState());
    }

    public static float getDefaultToolFarmingSpeed(@Nonnull ItemStack stack) {
        return getToolHarvestSpeed(stack, Blocks.HAY_BLOCK.defaultBlockState());
    }

    public static float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState blockState) {
        if (!isUseableTool(stack)) {
            return 0.0f;
        }
        try {
            if (isTinkersTool(stack) && canToolHarvest(stack, blockState)) {
                return TinkersConstructProxy.instance.getToolHarvestSpeed(stack, blockState);
            }
            return EnchantmentCompat.getHarvestSpeed(stack, blockState);
        } catch (Throwable ignored) {
            return 1.0f;
        }
    }

    public static float getDefaultToolSpeed(@Nonnull ItemStack stack, @Nonnull com.willr27.blocklings.util.ToolType toolType) {
        return switch (toolType) {
            case WEAPON -> getDefaultToolAttackSpeed(stack);
            case PICKAXE -> getDefaultToolMiningSpeed(stack);
            case AXE -> getDefaultToolWoodcuttingSpeed(stack);
            case HOE -> getDefaultToolFarmingSpeed(stack);
            default -> 0.0f;
        };
    }

    public static float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull ToolContext context) {
        return switch (context.toolType) {
            case WEAPON -> getToolAttackSpeed(stack, context.entity);
            case PICKAXE, AXE, HOE -> getToolHarvestSpeed(stack, context.blockState);
            default -> 0.0f;
        };
    }

    public static float getToolEnchantmentHarvestSpeed(@Nonnull ItemStack stack)
    {
        return EnchantmentCompat.enchantmentHarvestSpeedBonus(stack);
    }

    public static boolean canToolHarvest(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        try
        {
            if (BlockUtil.isCrop(blockState.getBlock()) && isHoe(stack))
            {
                return true;
            }
            if (BlockUtil.isOre(blockState.getBlock()) && !isPickaxe(stack))
            {
                return false;
            }
            if (BlockUtil.isLog(blockState.getBlock()) && !isAxe(stack))
            {
                return false;
            }
            if (isTinkersTool(stack))
            {
                return TinkersConstructProxy.instance.canToolHarvest(stack, blockState);
            }
            return stack.isCorrectToolForDrops(blockState);
        }
        catch (Throwable ignored)
        {
            // Enchanted / modded tools must never crash harvest checks.
            return isHoe(stack) || isPickaxe(stack) || isAxe(stack) || isWeapon(stack);
        }
    }

    @Nonnull
    public static List<Enchantment> findToolEnchantments(@Nonnull ItemStack stack)
    {
        List<Enchantment> enchantments = new ArrayList<>();
        try
        {
            var component = stack.get(DataComponents.ENCHANTMENTS);
            if (component == null)
            {
                return enchantments;
            }
            component.entrySet().forEach(entry ->
            {
                if (entry.getKey() != null && entry.getKey().isBound())
                {
                    enchantments.add(entry.getKey().value());
                }
            });
        }
        catch (Throwable ignored)
        {
        }
        return enchantments;
    }

    public static boolean damageTool(@Nonnull ItemStack stack, @Nonnull BlocklingEntity blockling, int damage)
    {
        if (stack.isEmpty() || damage <= 0)
        {
            return false;
        }

        if (BlocklingsConfig.COMMON.abilities.enabled.get()
                && BlocklingsConfig.COMMON.abilities.diamond.passiveEnabled.get()
                && BlocklingAbilitySupport.hasFamily(blockling, TypeFamily.DIAMOND))
        {
            return false;
        }

        if (!stack.isDamageableItem())
        {
            return false;
        }

        // Unbreaking + Apotheosis / any modded durability enchantments.
        int actualDamage = EnchantmentCompat.processDurabilityDamage(stack, blockling, damage);
        if (actualDamage <= 0)
        {
            return false;
        }

        int newDamage = stack.getDamageValue() + actualDamage;
        if (newDamage >= stack.getMaxDamage())
        {
            stack.setCount(0);
            return true;
        }

        stack.setDamageValue(newDamage);
        return false;
    }

    private static float enchantLevel(ItemStack stack, ResourceKey<Enchantment> enchantment)
    {
        return EnchantmentCompat.getLevel(stack, enchantment);
    }

    private static void addEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment, int level, ServerLevel levelAccess)
    {
        try
        {
            Holder<Enchantment> holder = levelAccess.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
            EnchantmentHelper.updateEnchantments(stack, map -> map.set(holder, level));
        }
        catch (Throwable ignored)
        {
        }
    }
}
