package com.willr27.blocklings.event;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingMobTargeting;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilityRegistry;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilitySupport;
import com.willr27.blocklings.entity.blockling.ability.TypeFamily;
import com.willr27.blocklings.entity.blockling.experience.BlocklingXpHandler;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.combat.HuntLootHandler;
import com.willr27.blocklings.item.BlocklingWhistleItem;
import com.willr27.blocklings.util.BlockUtil;
import com.willr27.blocklings.util.EntityUtil;
import com.willr27.blocklings.util.ToolUtil;
import com.willr27.blocklings.world.PlayerPlacedLogs;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;

@EventBusSubscriber(modid = Blocklings.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ForgeEventBusEvents
{
    @SubscribeEvent
    public static void onWorldLoad(@Nonnull LevelEvent.Load event)
    {
        if (event.getLevel() instanceof Level level) {
            EntityUtil.onWorldAvailable(level);
            BlockUtil.latestWorld = level;
        }

        BlocklingType.init();
        BlocklingAbilityRegistry.init();
        ToolUtil.init();

        BlocklingWhistleItem.BLOCKLINGS_TO_WHISTLES.clear();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(@Nonnull EntityJoinLevelEvent event)
    {
        BlocklingMobTargeting.tryAddBlocklingTargetGoal(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityDimensions(@Nonnull EntityEvent.Size event)
    {
        if (event.getEntity() instanceof BlocklingEntity blockling)
        {
            float scale = blockling.getBlocklingScale();
            if (scale <= 0.0f)
            {
                blockling.ensureBlocklingScale(false);
            }

            scale = blockling.getBlocklingScale();
            event.setNewSize(EntityDimensions.scalable(scale, scale));
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(@Nonnull LivingExperienceDropEvent event)
    {
        if (!(event.getEntity().getLastHurtByMob() instanceof BlocklingEntity blockling))
        {
            return;
        }

        BlocklingXpHandler.absorbKillExperience(blockling, event.getDroppedExperience());
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onProjectileImpact(@Nonnull ProjectileImpactEvent event)
    {
        if (!(event.getProjectile() instanceof ThrownExperienceBottle bottle))
        {
            return;
        }

        if (!BlocklingXpHandler.tryAbsorbThrownBottle(bottle))
        {
            return;
        }

        event.setCanceled(true);
        if (!bottle.level().isClientSide())
        {
            bottle.discard();
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(@Nonnull BlockEvent.EntityPlaceEvent event)
    {
        if (event.getEntity() instanceof Player player
                && event.getLevel() instanceof Level level)
        {
            PlayerPlacedLogs.markIfPlayerLog(level, event.getPos(), event.getPlacedBlock().getBlock(), player);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(@Nonnull BlockEvent.BreakEvent event)
    {
        if (event.getLevel() instanceof Level level)
        {
            PlayerPlacedLogs.unmark(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLivingDropsEvent(@Nonnull LivingDropsEvent event)
    {
        BlocklingEntity huntCollector = HuntLootHandler.findHuntCollector(event.getEntity(), event.getSource());
        if (huntCollector != null)
        {
            // Double the original drop counts first, then vacuum into inventory.
            // Legacy 1.18 doubled the addItem remainder (often empty) so Animal/Monster Hunter did almost nothing.
            HuntLootHandler.collectHuntDrops(huntCollector, event.getEntity(), event.getDrops());
            event.setCanceled(true);
        }

        if (event.isCanceled() || event.getDrops().isEmpty())
        {
            return;
        }

        if (!BlocklingsConfig.COMMON.abilities.enabled.get())
        {
            return;
        }

        double radius = BlocklingsConfig.COMMON.abilities.gold.radius.get();
        double chance = BlocklingsConfig.COMMON.abilities.gold.passiveChance.get();
        AABB searchBox = event.getEntity().getBoundingBox().inflate(radius);

        for (BlocklingEntity blockling : event.getEntity().level().getEntitiesOfClass(BlocklingEntity.class, searchBox))
        {
            if (!BlocklingAbilitySupport.hasFamily(blockling, TypeFamily.GOLD)
                    || !BlocklingsConfig.COMMON.abilities.gold.passiveEnabled.get()
                    || blockling.distanceToSqr(event.getEntity()) > radius * radius
                    || !BlocklingAbilitySupport.passChance(chance))
            {
                continue;
            }

            ItemEntity extraDrop = new ArrayList<>(event.getDrops()).get(blockling.getRandom().nextInt(event.getDrops().size()));
            ItemStack bonus = extraDrop.getItem().copy();
            bonus.setCount(1);
            event.getEntity().spawnAtLocation(bonus);
            break;
        }
    }
}


