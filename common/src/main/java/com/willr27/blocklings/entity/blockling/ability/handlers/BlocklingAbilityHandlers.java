package com.willr27.blocklings.entity.blockling.ability.handlers;

import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.ability.*;
import com.willr27.blocklings.util.BlockUtil;
import com.willr27.blocklings.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete ability handlers. Register via {@link BlocklingAbilityRegistry#init()}.
 */
public final class BlocklingAbilityHandlers
{
    private BlocklingAbilityHandlers()
    {
    }

    public static final BlocklingAbilityHandler GRASS = new GrassHandler();
    public static final BlocklingAbilityHandler DIRT = new DirtHandler();
    public static final BlocklingAbilityHandler WOOD = new WoodHandler();
    public static final BlocklingAbilityHandler STONE = new StoneHandler();
    public static final BlocklingAbilityHandler IRON = new IronHandler();
    public static final BlocklingAbilityHandler GOLD = new GoldHandler();
    public static final BlocklingAbilityHandler DIAMOND = new DiamondHandler();
    public static final BlocklingAbilityHandler EMERALD = new EmeraldHandler();
    public static final BlocklingAbilityHandler LAPIS = new LapisHandler();
    public static final BlocklingAbilityHandler OBSIDIAN = new ObsidianHandler();
    public static final BlocklingAbilityHandler GLOWSTONE = new GlowstoneHandler();
    public static final BlocklingAbilityHandler QUARTZ = new QuartzHandler();
    public static final BlocklingAbilityHandler NETHERITE = new NetheriteHandler();

    private static final class GrassHandler extends AbstractFamilyHandler
    {
        GrassHandler()
        {
            super(TypeFamily.GRASS, new BlocklingTypeProfile(TypeFamily.GRASS, "nature", "nature_pulse",
                    BlocklingSpecialty.FARMING, BlocklingSpecialty.TERRAFORM, BlocklingSpecialty.TERRAFORM));
        }

        @Override
        public void tickEnvironmental(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            if (TypeFamily.from(blockling.getNaturalBlocklingType()) != TypeFamily.GRASS)
            {
                return;
            }

            if (!blockling.onGround())
            {
                return;
            }

            BlockPos below = blockling.getOnPos();
            BlockState belowState = blockling.level().getBlockState(below);

            if (!BlocklingAbilitySupport.isBareDirt(belowState))
            {
                return;
            }

            if (blockling.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6D)
            {
                return;
            }

            if (BlocklingAbilitySupport.passChance(blockling.getRandom(), passiveChance() * 0.15D))
            {
                BlocklingAbilitySupport.setBlock(blockling.level(), below, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlockPos below = blockling.getOnPos();
            if (BlocklingAbilitySupport.passChance(blockling.getRandom(), passiveChance())
                    && BlocklingAbilitySupport.isBareDirt(blockling.level().getBlockState(below)))
            {
                BlocklingAbilitySupport.setBlock(blockling.level(), below, Blocks.GRASS_BLOCK.defaultBlockState());
            }

            BlockPos center = blockling.blockPosition();
            BlocklingAbilitySupport.forEachBlockInRadius(center, radius(), pos ->
            {
                if (BlocklingAbilitySupport.passChance(blockling.getRandom(), passiveChance() * 0.15D)
                        && BlocklingAbilitySupport.isBareDirt(blockling.level().getBlockState(pos)))
                {
                    BlocklingAbilitySupport.setBlock(blockling.level(), pos, Blocks.GRASS_BLOCK.defaultBlockState());
                }
            });

            if (blockling.level() instanceof ServerLevel serverLevel)
            {
                BlocklingAbilitySupport.forEachBlockInRadius(center, 2, pos ->
                {
                    BlockState state = blockling.level().getBlockState(pos);
                    if (state.is(BlockTags.FLOWERS) && BlocklingAbilitySupport.passChance(0.05D))
                    {
                        BlocklingAbilitySupport.spawnParticlesAt(serverLevel, pos);
                    }
                });
            }
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            BlockPos center = blockling.blockPosition();
            BlocklingAbilitySupport.forEachBlockInRadius(center, radius(), pos ->
            {
                if (blockling.level().getBlockState(pos).is(Blocks.DIRT))
                {
                    BlocklingAbilitySupport.setBlock(blockling.level(), pos, Blocks.GRASS_BLOCK.defaultBlockState());
                }
            });

            BlocklingAbilitySupport.forEachBlockInRadius(center, radius(), pos ->
            {
                BlockState above = blockling.level().getBlockState(pos.above());
                if (above.isAir() && blockling.level().getBlockState(pos).is(Blocks.GRASS_BLOCK))
                {
                    if (blockling.getRandom().nextBoolean())
                    {
                        BlocklingAbilitySupport.setBlock(blockling.level(), pos.above(), Blocks.TALL_GRASS.defaultBlockState());
                    }
                    else if (blockling.getRandom().nextInt(3) == 0)
                    {
                        BlocklingAbilitySupport.setBlock(blockling.level(), pos.above(), Blocks.POPPY.defaultBlockState());
                    }
                }
            });

            blockling.level().broadcastEntityEvent(blockling, (byte) 7);
            return true;
        }
    }

    private static final class DirtHandler extends AbstractFamilyHandler
    {
        DirtHandler()
        {
            super(TypeFamily.DIRT, new BlocklingTypeProfile(TypeFamily.DIRT, "earth", "earth_shift",
                    BlocklingSpecialty.TERRAFORM, BlocklingSpecialty.TANK, BlocklingSpecialty.TERRAFORM));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            if (TypeFamily.from(blockling.getNaturalBlocklingType()) != TypeFamily.DIRT)
            {
                return;
            }

            BlockPos below = blockling.getOnPos();
            if (BlocklingAbilitySupport.passChance(blockling.getRandom(), passiveChance())
                    && blockling.level().getBlockState(below).is(Blocks.GRASS_BLOCK))
            {
                BlocklingAbilitySupport.setBlock(blockling.level(), below, Blocks.DIRT.defaultBlockState());
            }
        }

        @Override
        public void tickPassiveSlow(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlockPos center = blockling.blockPosition();
            BlocklingAbilitySupport.forEachBlockInRadius(center, radius(), pos ->
            {
                BlockState state = blockling.level().getBlockState(pos);
                if (state.is(Blocks.DIRT) && blockling.level().getBlockState(pos.above()).isAir()
                        && BlocklingAbilitySupport.passChance(blockling.getRandom(), passiveChance() * 0.25D))
                {
                    BlocklingAbilitySupport.setBlock(blockling.level(), pos, Blocks.FARMLAND.defaultBlockState());
                }
            });
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            Direction facing = blockling.getDirection();
            Direction left = facing.getCounterClockWise();
            // Build the wall two blocks ahead so it never intersects the blockling's hitbox.
            BlockPos wallOrigin = blockling.blockPosition().relative(facing, 2);
            int wallHeight = 3;
            int halfWidth = 1;

            for (int y = 0; y < wallHeight; y++)
            {
                for (int w = -halfWidth; w <= halfWidth; w++)
                {
                    BlockPos pos = wallOrigin.relative(left, w).above(y);
                    if (blockling.getBoundingBox().inflate(0.05D).intersects(new AABB(pos)))
                    {
                        continue;
                    }

                    BlockState previous = blockling.level().getBlockState(pos);
                    if (previous.isAir() || previous.canBeReplaced())
                    {
                        BlocklingAbilitySupport.setBlock(blockling.level(), pos, Blocks.DIRT.defaultBlockState());
                        controller(blockling).scheduleTemporaryBlock(
                                new BlocklingAbilityController.TemporaryBlock(pos, previous, blockling.level().getGameTime() + activeDurationTicks()));
                    }
                }
            }

            // Unstick if somehow still overlapping solid dirt.
            BlockPos feet = blockling.blockPosition();
            if (!blockling.level().getBlockState(feet).isAir() && !blockling.level().getBlockState(feet).getCollisionShape(blockling.level(), feet).isEmpty())
            {
                blockling.teleportTo(blockling.getX(), blockling.getY() + 1.1D, blockling.getZ());
            }

            blockling.level().getEntitiesOfClass(Monster.class, blockling.getBoundingBox().inflate(radius()))
                    .forEach(monster -> monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)));

            return true;
        }
    }

    private static final class WoodHandler extends AbstractFamilyHandler
    {
        WoodHandler()
        {
            super(TypeFamily.WOOD, new BlocklingTypeProfile(TypeFamily.WOOD, "forest", "forest_blessing",
                    BlocklingSpecialty.WOODCUTTING, BlocklingSpecialty.TERRAFORM, BlocklingSpecialty.WOODCUTTING));
        }

        @Override
        public void tickPassiveSlow(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            growRandomSapling(blockling, passiveChance());

            if (BlocklingsConfig.COMMON.abilities.wood.passiveChance.get() > 0)
            {
                BlocklingAbilitySupport.forEachBlockInRadius(blockling.blockPosition(), radius(), pos ->
                {
                    BlockState state = blockling.level().getBlockState(pos);
                    if (state.getBlock() instanceof RotatedPillarBlock && state.getBlock().getDescriptionId().contains("stripped"))
                    {
                        // optional stripped log repair left to config chance via sapling growth path
                    }
                });
            }

            healNearTrees(blockling);
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            return growRandomSapling(blockling, 1.0D);
        }

        private boolean growRandomSapling(@Nonnull BlocklingEntity blockling, double chance)
        {
            if (!BlocklingAbilitySupport.passChance(chance))
            {
                return false;
            }

            List<BlockPos> saplings = new ArrayList<>();
            BlocklingAbilitySupport.forEachBlockInRadius(blockling.blockPosition(), radius(), pos ->
            {
                if (blockling.level().getBlockState(pos).is(BlockTags.SAPLINGS))
                {
                    saplings.add(pos.immutable());
                }
            });

            if (saplings.isEmpty())
            {
                return false;
            }

            BlockPos target = saplings.get(blockling.getRandom().nextInt(saplings.size()));
            Block block = blockling.level().getBlockState(target).getBlock();
            if (block instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(blockling.level(), target, blockling.level().getBlockState(target)))
            {
                bonemealable.performBonemeal((ServerLevel) blockling.level(), blockling.getRandom(), target, blockling.level().getBlockState(target));
                return true;
            }

            return false;
        }

        private void healNearTrees(@Nonnull BlocklingEntity blockling)
        {
            final int radius = 8;
            final float healAmount = 2.0f;

            for (int i = -radius; i <= radius; i++)
            {
                for (int j = -radius; j <= radius; j++)
                {
                    for (int k = -radius; k <= radius; k++)
                    {
                        BlockPos testPos = blockling.blockPosition().offset(i, j, k);
                        if (!BlockUtil.isLog(blockling.level().getBlockState(testPos).getBlock()))
                        {
                            continue;
                        }

                        WorldUtil.Tree tree = WorldUtil.findTreeFromPos(blockling.level(), testPos, 40, t -> true, t -> true);
                        if (!tree.isValid(BlocklingsConfig.COMMON.defaultMinLeavesToLogRatio.get().floatValue()))
                        {
                            continue;
                        }

                        if (blockling.getHealth() < blockling.getMaxHealth())
                        {
                            blockling.heal(healAmount);
                        }

                        LivingEntity owner = blockling.getOwner();
                        if (owner != null && owner.distanceToSqr(blockling) <= radius * radius && owner.getHealth() < owner.getMaxHealth())
                        {
                            owner.heal(healAmount);
                        }

                        BlocklingAbilitySupport.forEachAllyInRange(blockling, radius, ally ->
                        {
                            if (ally.getHealth() < ally.getMaxHealth())
                            {
                                ally.heal(healAmount);
                            }
                        });
                        return;
                    }
                }
            }
        }
    }

    private static final class StoneHandler extends AbstractFamilyHandler
    {
        StoneHandler()
        {
            super(TypeFamily.STONE, new BlocklingTypeProfile(TypeFamily.STONE, "stone", "stone_skin",
                    BlocklingSpecialty.MINING, BlocklingSpecialty.TANK, BlocklingSpecialty.TANK));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false, true), 419);
        }

        @Override
        public void tickEnvironmental(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlockPos below = blockling.getOnPos().below();
            BlockState belowState = blockling.level().getBlockState(below);
            if (belowState.is(Blocks.LAVA))
            {
                BlocklingAbilitySupport.setBlock(blockling.level(), below, Blocks.COBBLESTONE.defaultBlockState());
                controller(blockling).scheduleTemporaryBlock(new BlocklingAbilityController.TemporaryBlock(
                        below, belowState, blockling.level().getGameTime() + 200));
            }
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("stone_skin", activeDurationTicks());
            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, activeDurationTicks(), 2, false, false, true),
                    activeDurationTicks());
            blockling.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, activeDurationTicks(), 2, false, false, true));
            return true;
        }
    }

    private static final class IronHandler extends AbstractFamilyHandler
    {
        IronHandler()
        {
            super(TypeFamily.IRON, new BlocklingTypeProfile(TypeFamily.IRON, "magnet", "magnetic_field",
                    BlocklingSpecialty.MINING, BlocklingSpecialty.MAGNET, BlocklingSpecialty.MAGNET));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlocklingAbilitySupport.collectNearbyOres(blockling, radius() * 0.5D);
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            BlocklingAbilitySupport.pullItems(blockling, radius(), 0.35D);
            return true;
        }
    }

    private static final class GoldHandler extends AbstractFamilyHandler
    {
        GoldHandler()
        {
            super(TypeFamily.GOLD, new BlocklingTypeProfile(TypeFamily.GOLD, "lucky", "lucky_day",
                    BlocklingSpecialty.LOOT, BlocklingSpecialty.LOOT, BlocklingSpecialty.LOOT));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            LivingEntity owner = blockling.getOwner();
            if (owner != null && owner.distanceToSqr(blockling) <= radius() * radius())
            {
                owner.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 0, false, false, true));
            }
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("lucky_day", activeDurationTicks());
            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.LUCK, activeDurationTicks(), 1, false, false, true),
                    activeDurationTicks());
            return true;
        }
    }

    private static final class DiamondHandler extends AbstractFamilyHandler
    {
        DiamondHandler()
        {
            super(TypeFamily.DIAMOND, new BlocklingTypeProfile(TypeFamily.DIAMOND, "diamond", "diamond_rush",
                    BlocklingSpecialty.MINING, BlocklingSpecialty.MINING, BlocklingSpecialty.MINING));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled() || !(blockling.level() instanceof ServerLevel serverLevel))
            {
                return;
            }

            BlocklingAbilitySupport.forEachBlockInRadius(blockling.blockPosition(), radius(), pos ->
            {
                BlockState state = blockling.level().getBlockState(pos);
                if (BlockUtil.isOre(state.getBlock()) && blockling.getRandom().nextInt(10) == 0)
                {
                    serverLevel.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                }
            });
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("diamond_rush", activeDurationTicks());
            blockling.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, activeDurationTicks(), 3, false, false, true));
            return true;
        }
    }

    private static final class EmeraldHandler extends AbstractFamilyHandler
    {
        EmeraldHandler()
        {
            super(TypeFamily.EMERALD, new BlocklingTypeProfile(TypeFamily.EMERALD, "merchant", "merchants_blessing",
                    BlocklingSpecialty.TRADE, BlocklingSpecialty.LOOT, BlocklingSpecialty.TRADE));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.LUCK, 100, 0, false, false, true), 419);

            if (BlocklingAbilitySupport.passChance(passiveChance()))
            {
                blockling.level().getEntitiesOfClass(Villager.class, blockling.getBoundingBox().inflate(radius()))
                        .forEach(villager -> villager.spawnAtLocation(new ItemStack(Items.EMERALD)));
            }
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("merchants_blessing", activeDurationTicks());
            LivingEntity owner = blockling.getOwner();
            if (owner != null)
            {
                owner.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, activeDurationTicks(), 1, false, false, true));
            }
            return true;
        }
    }

    private static final class LapisHandler extends AbstractFamilyHandler
    {
        LapisHandler()
        {
            super(TypeFamily.LAPIS, new BlocklingTypeProfile(TypeFamily.LAPIS, "wisdom", "wisdom_aura",
                    BlocklingSpecialty.ENCHANTING, BlocklingSpecialty.ENCHANTING, BlocklingSpecialty.ENCHANTING));
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("wisdom_aura", activeDurationTicks());
            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.LUCK, activeDurationTicks(), 1, false, false, true),
                    activeDurationTicks());
            return true;
        }
    }

    private static final class ObsidianHandler extends AbstractFamilyHandler
    {
        ObsidianHandler()
        {
            super(TypeFamily.OBSIDIAN, new BlocklingTypeProfile(TypeFamily.OBSIDIAN, "fortress", "obsidian_fortress",
                    BlocklingSpecialty.FORTRESS, BlocklingSpecialty.TANK, BlocklingSpecialty.FORTRESS));
        }

        @Override
        public boolean isFireImmune(@Nonnull BlocklingEntity blockling)
        {
            return passiveEnabled();
        }

        @Override
        public void tickEnvironmental(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            BlockPos below = blockling.getOnPos().below();
            if (blockling.level().getBlockState(below).is(Blocks.LAVA))
            {
                BlocklingAbilitySupport.setBlock(blockling.level(), below, Blocks.OBSIDIAN.defaultBlockState());
            }
        }

        @Override
        public float onHurt(@Nonnull BlocklingEntity blockling, @Nonnull net.minecraft.world.damagesource.DamageSource source, float damage)
        {
            if (source.getEntity() instanceof LivingEntity attacker)
            {
                attacker.knockback(0.5f, Mth.sin(blockling.getYRot() * ((float) Math.PI / 180F)), -Mth.cos(blockling.getYRot() * ((float) Math.PI / 180F)));
            }
            return damage;
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, activeDurationTicks(), 2, false, false, true),
                    activeDurationTicks());
            BlocklingAbilitySupport.applyEffectToAllies(blockling, radius(),
                    new MobEffectInstance(MobEffects.ABSORPTION, activeDurationTicks(), 3, false, false, true),
                    activeDurationTicks());
            return true;
        }
    }

    private static final class GlowstoneHandler extends AbstractFamilyHandler
    {
        GlowstoneHandler()
        {
            super(TypeFamily.GLOWSTONE, new BlocklingTypeProfile(TypeFamily.GLOWSTONE, "light", "solar_burst",
                    BlocklingSpecialty.LIGHT, BlocklingSpecialty.LIGHT, BlocklingSpecialty.LIGHT));
        }

        @Override
        public void tickEnvironmental(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled() || blockling.level().isClientSide())
            {
                return;
            }

            BlockPos existing = controller(blockling).getGlowstoneLightPos();
            if (existing != null)
            {
                blockling.level().removeBlock(existing, false);
                controller(blockling).setGlowstoneLightPos(null);
            }

            BlockPos blockPos = BlockPos.containing(blockling.position().add(0.0, 0.5 * blockling.getBlocklingScale(), 0.0));
            for (BlockPos testPos : List.of(blockPos, blockPos.above(), blockPos.below(), blockPos.north(), blockPos.south(), blockPos.east(), blockPos.west()))
            {
                BlockState state = blockling.level().getBlockState(testPos);
                if (state.isAir() || state.is(BlocklingsRegistries.LIGHT_BLOCK.get()))
                {
                    blockling.level().setBlock(testPos, BlocklingsRegistries.LIGHT_BLOCK.get().defaultBlockState(), 3);
                    controller(blockling).setGlowstoneLightPos(testPos);
                    break;
                }
            }

            blockling.level().getEntitiesOfClass(Monster.class, blockling.getBoundingBox().inflate(radius()))
                    .forEach(mob -> mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true)));
        }

        @Override
        public void onRemove(@Nonnull BlocklingEntity blockling)
        {
            BlockPos lightPos = controller(blockling).getGlowstoneLightPos();
            if (lightPos != null && !blockling.level().isClientSide())
            {
                blockling.level().removeBlock(lightPos, false);
                controller(blockling).setGlowstoneLightPos(null);
            }
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            blockling.level().getEntitiesOfClass(Mob.class, blockling.getBoundingBox().inflate(radius()))
                    .forEach(mob ->
                    {
                        if (mob instanceof Monster)
                        {
                            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        }
                    });

            LivingEntity owner = blockling.getOwner();
            if (owner != null)
            {
                owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, activeDurationTicks(), 0, false, false, true));
            }
            return true;
        }
    }

    private static final class QuartzHandler extends AbstractFamilyHandler
    {
        QuartzHandler()
        {
            super(TypeFamily.QUARTZ, new BlocklingTypeProfile(TypeFamily.QUARTZ, "crystal", "crystal_reflection",
                    BlocklingSpecialty.REFLECT, BlocklingSpecialty.REFLECT, BlocklingSpecialty.REFLECT));
        }

        @Override
        public void tickPassive(@Nonnull BlocklingEntity blockling)
        {
            if (!passiveEnabled())
            {
                return;
            }

            blockling.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false, true));
        }

        @Override
        public float onHurt(@Nonnull BlocklingEntity blockling, @Nonnull net.minecraft.world.damagesource.DamageSource source, float damage)
        {
            if (source.getEntity() instanceof LivingEntity attacker)
            {
                attacker.hurt(blockling.damageSources().mobAttack(blockling), damage * 0.15F);
            }
            return damage;
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("crystal_reflection", activeDurationTicks());
            blockling.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, activeDurationTicks(), 1, false, false, true));

            blockling.level().getEntitiesOfClass(Projectile.class, blockling.getBoundingBox().inflate(radius()))
                    .forEach(projectile ->
                    {
                        Vec3 motion = projectile.getDeltaMovement();
                        projectile.setDeltaMovement(motion.scale(-1.0D));
                    });
            return true;
        }
    }

    private static final class NetheriteHandler extends AbstractFamilyHandler
    {
        NetheriteHandler()
        {
            super(TypeFamily.NETHERITE, new BlocklingTypeProfile(TypeFamily.NETHERITE, "ancient", "ancient_guardian",
                    BlocklingSpecialty.ANCIENT, BlocklingSpecialty.ANCIENT, BlocklingSpecialty.ANCIENT));
        }

        @Override
        public boolean isFireImmune(@Nonnull BlocklingEntity blockling)
        {
            return passiveEnabled();
        }

        @Override
        public boolean isKnockbackImmune(@Nonnull BlocklingEntity blockling)
        {
            return passiveEnabled();
        }

        @Override
        public boolean tryNegateDamage(@Nonnull BlocklingEntity blockling, @Nonnull net.minecraft.world.damagesource.DamageSource source)
        {
            return passiveEnabled() && blockling.getRandom().nextInt(10) == 0;
        }

        @Override
        public boolean activate(@Nonnull BlocklingEntity blockling)
        {
            if (!activeEnabled())
            {
                return false;
            }

            controller(blockling).startBuff("ancient_guardian", activeDurationTicks());
            blockling.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, activeDurationTicks(), 2, false, false, true));
            blockling.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, activeDurationTicks(), 2, false, false, true));
            blockling.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, activeDurationTicks(), 4, false, false, true));
            return true;
        }
    }
}
