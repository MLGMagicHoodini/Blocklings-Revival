package com.willr27.blocklings.item;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.Task;
import com.willr27.blocklings.util.BlocklingsResourceLocation;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import com.willr27.blocklings.util.ObjectUtil;
import com.willr27.blocklings.util.Version;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.DeferredWorkQueue;
/*import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.DeferredWorkQueue;*/

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * An item used to spawn blocklings with data preserved.
 */
public class BlocklingItem extends Item
{
    /**
     * The default constructor.
     */
    public BlocklingItem()
    {
        super(new Item.Properties()
                .tab(ItemGroup.TAB_MISC)
                .stacksTo(1));
    }

    /**
     * Creates a blockling item from a blockling.
     *
     * @param blockling the blockling to create the item from.
     * @return the blockling item.
     */
    @Nonnull
    public static ItemStack create(@Nonnull BlocklingEntity blockling)
    {
        ItemStack stack = new ItemStack(BlocklingsItems.BLOCKLING.get(), 1);
        stack.setHoverName(new StringTextComponent(TextFormatting.GOLD + blockling.getCustomName().getString()));

        CompoundTag stackTag = stack.getOrCreateTag();

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

        return stack;
    }

    @Nonnull
    @Override
    public ActionResultType useOn(ItemUseContext context)
    {
        Level level = context.getLevel();

        if (!world.isClientSide)
        {
            ItemStack stack = context.getItemInHand();
            BlockPos blockpos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);

            if (!blockstate.getCollisionShape(level, blockpos).isEmpty())
            {
                blockpos = blockpos.relative(direction);
            }

            BlocklingEntity blockling = new BlocklingEntity(BlocklingsEntityTypes.BLOCKLING.get(), level);

            CompoundTag stackTag = stack.getTag();
            CompoundTag entityTag = null;

            if (stackTag != null && stackTag.contains("entity"))
            {
                entityTag = stackTag.getCompound("entity");
            }

            blockling.finalizeSpawn((IServerWorld) level, level.getCurrentDifficultyAt(blockpos), MobSpawnType.SPAWN_EGG, null, entityTag);

            if (entityTag == null || !entityTag.contains("blockling"))
            {
                for (Task task : blockling.getTasks().getPrioritisedTasks())
                {
                    if (task.isConfigured() && task.getType() == BlocklingTasks.WANDER)
                    {
                        task.setType(BlocklingTasks.FOLLOW, false);
                    }
                }
            }

            blockling.setPos(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5);
            blockling.tame(context.getPlayer());

            if (stack.getTag() != null)
            {
                if (stack.getTag().contains("custom_name"))
                {
                    blockling.setCustomName(new StringTextComponent(stack.getTag().getString("custom_name")));
                }
            }

            level.addFreshEntity(blockling);

            if (!context.getPlayer().abilities.instabuild)
            {
                stack.shrink(1);
            }
        }

        return ActionResultType.PASS;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag)
    {
        CompoundTag stackTag = stack.getTag();

        if (stackTag != null && stackTag.contains("entity"))
        {
            tooltip.add(new StringTextComponent(TextFormatting.GREEN + new BlocklingsTranslationTextComponent("attribute.health.name").getString() + ": " + stackTag.getInt("health") + "/" + stackTag.getInt("max_health")));
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + new BlocklingsTranslationTextComponent("attribute.combat_level.name").getString() + ": " + stackTag.getInt("combat_level")));
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + new BlocklingsTranslationTextComponent("attribute.mining_level.name").getString() + ": " + stackTag.getInt("mining_level")));
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + new BlocklingsTranslationTextComponent("attribute.woodcutting_level.name").getString() + ": " + stackTag.getInt("woodcutting_level")));
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + new BlocklingsTranslationTextComponent("attribute.farming_level.name").getString() + ": " + stackTag.getInt("farming_level")));
            tooltip.add(new StringTextComponent(TextFormatting.GRAY + new BlocklingsTranslationTextComponent("attribute.total_level.name").getString() + ": " + stackTag.getInt("total_level")));
            tooltip.add(new StringTextComponent(""));
        }

        super.appendHoverText(stack, world, tooltip, flag);
    }

    public static void registerItemModelsProperties()
    {
        DeferredWorkQueue.runLater(() ->
        {
            ItemModelsProperties.register(BlocklingsItems.BLOCKLING.get(), new BlocklingsResourceLocation("type"), (stack, world, entity) ->
            {
                CompoundTag stackTag = stack.getTag();

                if (stackTag != null)
                {
                    CompoundTag entityTag = stackTag.getCompound("entity");

                    if (entityTag != null)
                    {
                        CompoundTag blocklingTag = entityTag.getCompound("blockling");

                        if (blocklingTag != null)
                        {
                            return BlocklingType.TYPES.indexOf(BlocklingType.find(blocklingTag.getString("type"), ObjectUtil.coalesce(new Version(blocklingTag.getString("blocklings_version")), Blocklings.VERSION)));
                        }
                    }
                }

                return 0;
            });
        });
    }
}
