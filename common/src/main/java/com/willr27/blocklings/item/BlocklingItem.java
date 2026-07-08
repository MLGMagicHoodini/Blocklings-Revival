package com.willr27.blocklings.item;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.Task;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import com.willr27.blocklings.util.ObjectUtil;
import com.willr27.blocklings.util.Version;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlocklingItem extends Item {
    public BlocklingItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nonnull
    public static CompoundTag getDataTag(@Nonnull ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    @Nonnull
    private static ItemStack withDataTag(@Nonnull ItemStack stack, @Nonnull CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Nonnull
    public static ItemStack createPreview(@Nonnull BlocklingType type)
    {
        ItemStack stack = BlocklingsRegistries.blocklingItemStack();
        stack.set(DataComponents.CUSTOM_NAME, type.name.copy().withStyle(ChatFormatting.GOLD));

        // Creative-tab previews: show baseline fresh-blockling stats (levels start at 1).
        int combatLevel = 1;
        int miningLevel = 1;
        int woodcuttingLevel = 1;
        int farmingLevel = 1;
        int maxHealth = previewMaxHealth(type, combatLevel);

        CompoundTag stackTag = new CompoundTag();
        CompoundTag blocklingTag = new CompoundTag();
        blocklingTag.putString("blocklings_version", Blocklings.VERSION.toString());
        blocklingTag.putString("original_type", type.key);
        blocklingTag.putString("type", type.key);
        blocklingTag.putInt("variant", 0);
        blocklingTag.putFloat("scale", 1.0f);
        stackTag.put("entity", new CompoundTag());
        stackTag.getCompound("entity").put("blockling", blocklingTag);

        stackTag.putInt("health", maxHealth);
        stackTag.putInt("max_health", maxHealth);
        stackTag.putInt("combat_level", combatLevel);
        stackTag.putInt("mining_level", miningLevel);
        stackTag.putInt("woodcutting_level", woodcuttingLevel);
        stackTag.putInt("farming_level", farmingLevel);
        stackTag.putInt("total_level", combatLevel + miningLevel + woodcuttingLevel + farmingLevel);

        withDataTag(stack, stackTag);
        applyTypeModel(stack, type);
        return stack;
    }

    /** custom_model_data = type index + 1 (0 = generic fallback texture). */
    public static void applyTypeModel(@Nonnull ItemStack stack, @Nonnull BlocklingType type)
    {
        int index = BlocklingType.TYPES.indexOf(type);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.max(0, index) + 1));
    }

    /**
     * Matches {@link com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes}:
     * base 10 + type bonus + combat-level bonus curve.
     */
    private static int previewMaxHealth(@Nonnull BlocklingType type, int combatLevel)
    {
        float combatBonus = (float) (50.0f * Math.tan((combatLevel / (float) BlocklingAttributes.Level.MAX) * (Math.PI / 4.0f)));
        return (int) Math.ceil(10.0f + type.getMaxHealth() + combatBonus);
    }

    @Nonnull
    public static ItemStack create(@Nonnull BlocklingEntity blockling) {
        ItemStack stack = BlocklingsRegistries.blocklingItemStack();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(blockling.getCustomName().getString()).withStyle(ChatFormatting.GOLD));

        CompoundTag stackTag = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        blockling.addAdditionalSaveData(entityTag);
        stackTag.put("entity", entityTag);

        stackTag.putString("custom_name", blockling.getCustomName().getString());
        stackTag.putInt("health", blockling.getStats().getHealth());
        stackTag.putInt("max_health", blockling.getStats().getMaxHealth());
        stackTag.putInt("combat_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.COMBAT).getValue());
        stackTag.putInt("mining_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.MINING).getValue());
        stackTag.putInt("woodcutting_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.WOODCUTTING).getValue());
        stackTag.putInt("farming_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.FARMING).getValue());
        stackTag.putInt("total_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.TOTAL).getValue());

        withDataTag(stack, stackTag);
        applyTypeModel(stack, blockling.getBlocklingType());
        return stack;
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();

        if (!world.isClientSide()) {
            ItemStack stack = context.getItemInHand();
            BlockPos blockpos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            BlockState blockstate = world.getBlockState(blockpos);

            if (!blockstate.getCollisionShape(world, blockpos).isEmpty()) {
                blockpos = blockpos.relative(direction);
            }

            BlocklingEntity blockling = new BlocklingEntity(BlocklingsRegistries.blocklingEntity(), world);

            CompoundTag stackTag = getDataTag(stack);
            CompoundTag entityTag = stackTag.contains("entity") ? stackTag.getCompound("entity") : null;
            boolean hasPresetData = entityTag != null && entityTag.contains("blockling");

            if (world instanceof ServerLevel serverLevel) {
                if (hasPresetData) {
                    // Apply type first so a creative Dirt preview never stays as constructor grass/random type.
                    CompoundTag blocklingTag = entityTag.getCompound("blockling");
                    Version tagVersion = ObjectUtil.coalesce(new Version(blocklingTag.getString("blocklings_version")), Blocklings.VERSION);
                    BlocklingType type = BlocklingType.find(blocklingTag.getString("type"), tagVersion);
                    BlocklingType natural = BlocklingType.find(blocklingTag.getString("original_type"), tagVersion);
                    blockling.setNaturalBlocklingType(natural, false);
                    blockling.setBlocklingType(type, false);
                    blockling.readAdditionalSaveData(entityTag);
                    // Creative typed items should match the preview size (1.0), not random natural spawn sizes.
                    CompoundTag bl = entityTag.getCompound("blockling");
                    if (!bl.contains("scale") || bl.getFloat("scale") <= 0.0f)
                    {
                        blockling.setBlocklingScale(1.0f, false);
                    }

                    if (blockling.getTasks().getPrioritisedTasks().isEmpty()) {
                        blockling.getTasks().initDefaultTasks();
                    }
                }
                else {
                    blockling.finalizeSpawn(serverLevel, world.getCurrentDifficultyAt(blockpos), MobSpawnType.SPAWN_EGG, null);
                    blockling.chooseSpawnTypeForLocation(serverLevel, MobSpawnType.SPAWN_EGG);
                    blockling.setBlocklingScale(1.0f, false);

                    for (Task task : blockling.getTasks().getPrioritisedTasks()) {
                        if (task.isConfigured() && task.getType() == BlocklingTasks.WANDER) {
                            task.setType(BlocklingTasks.FOLLOW, false);
                        }
                    }
                }
            }

            blockling.setPos(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5);

            if (context.getPlayer() != null)
            {
                blockling.tame(context.getPlayer());
            }

            if (stackTag.contains("custom_name")) {
                blockling.setCustomName(Component.literal(stackTag.getString("custom_name")));
            }
            else if (hasPresetData)
            {
                // Keep type name if the stack itself had a custom display name.
                Component stackName = stack.get(DataComponents.CUSTOM_NAME);
                if (stackName != null)
                {
                    blockling.setCustomName(stackName.copy().withStyle(style -> style.withItalic(false)));
                }
            }

            world.addFreshEntity(blockling);

            if (!context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        CompoundTag stackTag = getDataTag(stack);

        if (stackTag.contains("entity")) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + BlocklingsTranslationTextComponent.of("attribute.health.name").getString() + ": " + stackTag.getInt("health") + "/" + stackTag.getInt("max_health")));
            tooltip.add(Component.literal(ChatFormatting.GRAY + BlocklingsTranslationTextComponent.of("attribute.combat_level.name").getString() + ": " + stackTag.getInt("combat_level")));
            tooltip.add(Component.literal(ChatFormatting.GRAY + BlocklingsTranslationTextComponent.of("attribute.mining_level.name").getString() + ": " + stackTag.getInt("mining_level")));
            tooltip.add(Component.literal(ChatFormatting.GRAY + BlocklingsTranslationTextComponent.of("attribute.woodcutting_level.name").getString() + ": " + stackTag.getInt("woodcutting_level")));
            tooltip.add(Component.literal(ChatFormatting.GRAY + BlocklingsTranslationTextComponent.of("attribute.farming_level.name").getString() + ": " + stackTag.getInt("farming_level")));
            tooltip.add(Component.literal(ChatFormatting.GRAY + BlocklingsTranslationTextComponent.of("attribute.total_level.name").getString() + ": " + stackTag.getInt("total_level")));
            tooltip.add(Component.literal(""));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
