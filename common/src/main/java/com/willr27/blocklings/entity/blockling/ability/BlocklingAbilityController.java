package com.willr27.blocklings.entity.blockling.ability;

import com.willr27.blocklings.config.BlocklingAbilityConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.network.messages.BlocklingActiveAbilityMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Per-entity orchestrator for modular type abilities. Server authoritative for multiplayer.
 */
public final class BlocklingAbilityController
{
    private final BlocklingEntity blockling;
    private int activeCooldownTicks;
    private int buffEndTick;
    @Nonnull
    private String activeBuff = "";
    @Nonnull
    private final List<TemporaryBlock> temporaryBlocks = new ArrayList<>();

    @Nullable
    private BlockPos glowstoneLightPos;

    public BlocklingAbilityController(@Nonnull BlocklingEntity blockling)
    {
        this.blockling = blockling;
    }

    public void tick()
    {
        if (blockling.level().isClientSide() || !BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return;
        }

        if (activeCooldownTicks > 0)
        {
            activeCooldownTicks--;
        }

        tickTemporaryBlocks();
        tickActiveFamilies(BlocklingAbilityHandler::tickEnvironmental);

        if (blockling.tickCount % 20 == 0)
        {
            tickActiveFamilies(BlocklingAbilityHandler::tickPassive);
        }

        if (blockling.tickCount % 200 == 0)
        {
            tickActiveFamilies(BlocklingAbilityHandler::tickPassiveSlow);
        }
    }

    private void tickActiveFamilies(@Nonnull TickConsumer consumer)
    {
        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null)
            {
                consumer.accept(handler, blockling);
            }
        }
    }

    @FunctionalInterface
    private interface TickConsumer
    {
        void accept(@Nonnull BlocklingAbilityHandler handler, @Nonnull BlocklingEntity blockling);
    }

    @Nonnull
    private List<TypeFamily> activeFamilies()
    {
        List<TypeFamily> families = new ArrayList<>();
        TypeFamily natural = TypeFamily.from(blockling.getNaturalBlocklingType());
        TypeFamily current = TypeFamily.from(blockling.getBlocklingType());
        families.add(natural);
        if (current != natural)
        {
            families.add(current);
        }
        return families;
    }

    public float onHurt(@Nonnull DamageSource source, float damage)
    {
        if (!BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return damage;
        }

        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null && handler.tryNegateDamage(blockling, source))
            {
                return 0.0F;
            }
        }

        return damage;
    }

    public void afterHurt(@Nonnull DamageSource source, float damage)
    {
        if (!BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return;
        }

        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null)
            {
                handler.onHurt(blockling, source, damage);
            }
        }
    }

    public boolean isFireImmune()
    {
        if (!BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return false;
        }

        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null && handler.isFireImmune(blockling))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isKnockbackImmune()
    {
        if (!BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return false;
        }

        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null && handler.isKnockbackImmune(blockling))
            {
                return true;
            }
        }

        return false;
    }

    public void onRemove()
    {
        for (TypeFamily family : activeFamilies())
        {
            BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
            if (handler != null)
            {
                handler.onRemove(blockling);
            }
        }

        restoreAllTemporaryBlocks();
    }

    @Nonnull
    public InteractionResult tryActivate(@Nonnull Player player)
    {
        if (!BlocklingsConfig.COMMON.abilities.enabled.get() || blockling.level().isClientSide())
        {
            return InteractionResult.PASS;
        }

        if (!blockling.isTame() || blockling.getOwnerUUID() == null || !blockling.getOwnerUUID().equals(player.getUUID()))
        {
            return InteractionResult.PASS;
        }

        if (activeCooldownTicks > 0)
        {
            if (player instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.displayClientMessage(Component.translatable("blocklings.ability.cooldown", activeCooldownTicks / 20), true);
            }
            return InteractionResult.FAIL;
        }

        TypeFamily family = TypeFamily.from(blockling.getBlocklingType());
        BlocklingAbilityHandler handler = BlocklingAbilityRegistry.get(family);
        if (handler == null || !familyConfig(family).activeEnabled.get())
        {
            return InteractionResult.PASS;
        }

        if (handler.activate(blockling))
        {
            activeCooldownTicks = handler.activeCooldownTicks(blockling);
            new BlocklingActiveAbilityMessage(blockling, family, activeCooldownTicks).sync();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public boolean hasActiveBuff(@Nonnull String buffId)
    {
        return activeBuff.equals(buffId) && blockling.level().getGameTime() < buffEndTick;
    }

    public void startBuff(@Nonnull String buffId, int durationTicks)
    {
        activeBuff = buffId;
        buffEndTick = (int) (blockling.level().getGameTime() + durationTicks);
    }

    public int getActiveCooldownTicks()
    {
        return activeCooldownTicks;
    }

    public void scheduleTemporaryBlock(@Nonnull TemporaryBlock entry)
    {
        temporaryBlocks.add(entry);
    }

    private void tickTemporaryBlocks()
    {
        Iterator<TemporaryBlock> iterator = temporaryBlocks.iterator();
        long gameTime = blockling.level().getGameTime();

        while (iterator.hasNext())
        {
            TemporaryBlock entry = iterator.next();
            if (gameTime >= entry.restoreTick())
            {
                entry.restore(blockling.level());
                iterator.remove();
            }
        }
    }

    private void restoreAllTemporaryBlocks()
    {
        temporaryBlocks.forEach(entry -> entry.restore(blockling.level()));
        temporaryBlocks.clear();
    }

    @Nonnull
    private static BlocklingAbilityConfig.FamilyConfig familyConfig(@Nonnull TypeFamily family)
    {
        return switch (family)
        {
            case GRASS -> BlocklingsConfig.COMMON.abilities.grass;
            case DIRT -> BlocklingsConfig.COMMON.abilities.dirt;
            case WOOD -> BlocklingsConfig.COMMON.abilities.wood;
            case STONE -> BlocklingsConfig.COMMON.abilities.stone;
            case IRON -> BlocklingsConfig.COMMON.abilities.iron;
            case GOLD -> BlocklingsConfig.COMMON.abilities.gold;
            case DIAMOND -> BlocklingsConfig.COMMON.abilities.diamond;
            case EMERALD -> BlocklingsConfig.COMMON.abilities.emerald;
            case LAPIS -> BlocklingsConfig.COMMON.abilities.lapis;
            case OBSIDIAN -> BlocklingsConfig.COMMON.abilities.obsidian;
            case GLOWSTONE -> BlocklingsConfig.COMMON.abilities.glowstone;
            case QUARTZ -> BlocklingsConfig.COMMON.abilities.quartz;
            case NETHERITE -> BlocklingsConfig.COMMON.abilities.netherite;
        };
    }

    @Nullable
    public BlockPos getGlowstoneLightPos()
    {
        return glowstoneLightPos;
    }

    public void setGlowstoneLightPos(@Nullable BlockPos pos)
    {
        this.glowstoneLightPos = pos;
    }

    public void writeToNBT(@Nonnull CompoundTag tag)
    {
        tag.putInt("ability_cooldown", activeCooldownTicks);
        tag.putInt("ability_buff_end", buffEndTick);
        tag.putString("ability_buff", activeBuff);
    }

    public void readFromNBT(@Nonnull CompoundTag tag)
    {
        activeCooldownTicks = tag.getInt("ability_cooldown");
        buffEndTick = tag.getInt("ability_buff_end");
        activeBuff = tag.getString("ability_buff");
    }

    public record TemporaryBlock(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState previousState, long restoreTick)
    {
        public void restore(net.minecraft.world.level.Level level)
        {
            if (level.isLoaded(pos))
            {
                level.setBlock(pos, previousState, 3);
            }
        }
    }
}
