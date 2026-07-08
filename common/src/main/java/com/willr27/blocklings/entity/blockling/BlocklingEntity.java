package com.willr27.blocklings.entity.blockling;

import com.google.common.collect.Iterables;
import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.client.gui.BlocklingGuiHandler;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilityController;
import com.willr27.blocklings.entity.blockling.action.BlocklingActions;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes;
import com.willr27.blocklings.entity.blockling.skill.BlocklingSkills;
import com.willr27.blocklings.entity.blockling.skill.skills.*;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.Task;
import com.willr27.blocklings.interop.TinkersConstructProxy;
import com.willr27.blocklings.inventory.EquipmentInventory;
import com.willr27.blocklings.item.BlocklingItem;
import com.willr27.blocklings.item.BlocklingWhistleItem;
import com.willr27.blocklings.network.messages.BlocklingAttackTargetMessage;
import com.willr27.blocklings.network.messages.BlocklingNameMessage;
import com.willr27.blocklings.network.messages.BlocklingScaleMessage;
import com.willr27.blocklings.network.messages.BlocklingTypeMessage;
import com.willr27.blocklings.util.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import com.willr27.blocklings.platform.Services;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;


/**
 * The blockling entity.
 */
public class BlocklingEntity extends TamableAnimal implements IReadWriteNBT
{
    private static final EntityDataAccessor<Integer> DATA_NATURAL_TYPE =
            SynchedEntityData.defineId(BlocklingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(BlocklingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(BlocklingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(BlocklingEntity.class, EntityDataSerializers.FLOAT);

    /**
     * The blockling type the blockling spawned as.
     */
    @Nonnull
    private BlocklingType naturalBlocklingType = BlocklingType.GRASS;

    /**
     * The blockling type the blockling has been changed to.
     */
    @Nonnull
    private BlocklingType blocklingType = BlocklingType.GRASS;

    /**
     * The variant used to determine how a blockling blends with its original blockling type.
     */
    private int blocklingTypeVariant = 0;

    /**
     * The blockling's attribute manager (called stats because attributes is already thing in vanilla).
     */
    @Nonnull
    private final BlocklingAttributes stats = new BlocklingAttributes(this);

    /**
     * The blockling's skills manager.
     */
    @Nonnull
    private final BlocklingSkills skills = new BlocklingSkills(this);

    /**
     * The blockling's task manager.
     */
    @Nonnull
    private final BlocklingTasks tasks = new BlocklingTasks(this);

    /**
     * The blockling's action manager.
     */
    @Nonnull
    private final BlocklingActions actions = new BlocklingActions(this);

    /**
     * The blockling's equipment inventory.
     */
    @Nonnull
    private final EquipmentInventory equipmentInv = new EquipmentInventory(this);

    /**
     * Handles opening screens and containers.
     */
    @Nonnull
    public final BlocklingGuiHandler guiHandler = new BlocklingGuiHandler(this);

    @Nonnull
    public GoalSelector getGoalSelector()
    {
        return goalSelector;
    }

    @Nonnull
    public GoalSelector getTargetSelector()
    {
        return targetSelector;
    }

    /**
     * The blockling's scale (size).
     */
    private float scale = .0f;

    /**
     * Tracks how many attacks have occurred within 100 ticks of each other.
     * Used by the momentum skill.
     */
    private int attacksRecently = 0;

    /**
     * Tracks how many ores have been mined within 100 ticks of each other.
     * Used by the momentum skill.
     */
    private int oresMinedRecently = 0;

    /**
     * Tracks how many logs have been chopped within 100 ticks of each other.
     * Used by the momentum skill.
     */
    private int logsChoppedRecently = 0;

    /**
     * Tracks how many crops have been harvested within 100 ticks of each other.
     * Used by the momentum skill.
     */
    private int cropsHarvestedRecently = 0;

    /**
     * Whether the last attack the blockling performed was via a hunt task.
     * NOT synced to the client/server.
     */
    public boolean wasLastAttackHunt = false;

    /**
     * Used to track whether the player has released crouch after interacting (changing blockling type).
     * This stops a player picking up a blockling immediately after changing its type by accident.
     * Should be replaced with a capability on the player to tell when they have stopped using an item.
     */
    private boolean hasPlayerResetCrouchBetweenInteractions = true;

    /**
     * Modular type ability controller (passives, actives, environment).
     */
    @Nonnull
    private final BlocklingAbilityController abilityController = new BlocklingAbilityController(this);

    /**
     * @param type the blockling entity type.
     * @param world the world the blockling is in.
     */
    public BlocklingEntity(@Nonnull EntityType<? extends BlocklingEntity> type, @Nonnull Level world)
    {
        super(type, world);

        stats.initUpdateCallbacks();

        // Set up any values that are determined randomly here.
        // So that we can sync them up using read/writeSpawnData.
        if (!level().isClientSide())
        {
            blocklingTypeVariant = getRandom().nextInt(3);
            setNaturalBlocklingType(BlocklingType.TYPES.get(getRandom().nextInt(BlocklingType.TYPES.size())), false);
            setBlocklingType(naturalBlocklingType, false);

            stats.init();
            // Random size is only applied for natural/chunk spawns (finalizeSpawn / checkSpawnRules).
            // Item/creative placements set a fixed scale themselves so they do not inherit constructor RNG.
            if (scale <= 0.0f)
            {
                setBlocklingScale(1.0f, false);
            }
        }

        actions.ticks20.addCallback(this::updatePassiveAbilities);

        equipmentInv.updateToolAttributes();

        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_NATURAL_TYPE, 0);
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_SCALE, 1.0f);
    }

    @Override
    public void onSyncedDataUpdated(@Nonnull EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);

        if (DATA_NATURAL_TYPE.equals(key))
        {
            naturalBlocklingType = BlocklingType.getTypeByIndex(entityData.get(DATA_NATURAL_TYPE));
        }
        else if (DATA_TYPE.equals(key))
        {
            blocklingType = BlocklingType.getTypeByIndex(entityData.get(DATA_TYPE));
            if (level().isClientSide())
            {
                stats.updateTypeBonuses(false);
            }
        }
        else if (DATA_VARIANT.equals(key))
        {
            blocklingTypeVariant = entityData.get(DATA_VARIANT);
        }
        else if (DATA_SCALE.equals(key))
        {
            scale = entityData.get(DATA_SCALE);
            refreshDimensions();
        }
    }

    private void syncAppearanceData()
    {
        if (!level().isClientSide())
        {
            entityData.set(DATA_NATURAL_TYPE, Math.max(0, BlocklingType.TYPES.indexOf(naturalBlocklingType)));
            entityData.set(DATA_TYPE, Math.max(0, BlocklingType.TYPES.indexOf(blocklingType)));
            entityData.set(DATA_VARIANT, blocklingTypeVariant);
            entityData.set(DATA_SCALE, scale > 0.0f ? scale : 1.0f);
        }
    }

    @Override
    public void remove(RemovalReason reason)
    {
        abilityController.onRemove();
        super.remove(reason);
    }

    @Nonnull
    public BlocklingAbilityController getAbilityController()
    {
        return abilityController;
    }

    /**
     * @return the additional attributes to add to the entity.
     */
    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.ATTACK_SPEED, 0.0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return BlocklingType.isFood(stack);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor level, @Nonnull DifficultyInstance difficulty,
                                        @Nonnull MobSpawnType spawnReason, @Nullable SpawnGroupData spawnData) {
        // Natural / chunk / egg-world gen: allow random sizes. Item placement bypasses finalizeSpawn for presets.
        boolean randomize = spawnReason == MobSpawnType.NATURAL
                || spawnReason == MobSpawnType.CHUNK_GENERATION
                || spawnReason == MobSpawnType.STRUCTURE
                || spawnReason == MobSpawnType.MOB_SUMMONED;
        // Creative/spawn-egg style reasons keep fixed 1.0 unless a scale is already present.
        if (spawnReason == MobSpawnType.SPAWN_EGG || spawnReason == MobSpawnType.SPAWNER || spawnReason == MobSpawnType.DISPENSER || spawnReason == MobSpawnType.COMMAND)
        {
            randomize = false;
        }
        // If constructor already set 1.0, re-randomize only for true natural spawns.
        if (randomize)
        {
            setBlocklingScale(getRandom().nextFloat() * 0.5f + 0.45f, false);
        }
        else
        {
            ensureBlocklingScale(false, false);
        }
        tasks.initDefaultTasks();
        BlocklingSpawnDiagnostics.onFinalizeSpawn(this, spawnReason);
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);

        CompoundTag blocklingTag = new CompoundTag();

        blocklingTag.putString("blocklings_version", Blocklings.VERSION.toString());

        writeToNBT(blocklingTag);

        tag.put("blockling", blocklingTag);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag blocklingTag)
    {
        blocklingTag.putString("original_type", naturalBlocklingType.key);
        blocklingTag.putString("type", blocklingType.key);
        blocklingTag.putInt("variant", blocklingTypeVariant);
        blocklingTag.putFloat("scale", scale);

        blocklingTag.put("equipment_inv", equipmentInv.writeToNBT());
        blocklingTag.put("attributes", stats.writeToNBT());
        blocklingTag.put("tasks", tasks.writeToNBT());
        blocklingTag.put("skills", skills.writeToNBT());
        abilityController.writeToNBT(blocklingTag);

        return blocklingTag;
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);

        CompoundTag blocklingTag = tag.getCompound("blockling");

        if (blocklingTag != null)
        {
            readFromNBT(blocklingTag, ObjectUtil.coalesce(new Version(blocklingTag.getString("blocklings_version")), Blocklings.VERSION));
        }
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag blocklingTag, @Nonnull Version tagVersion)
    {
        blocklingTypeVariant = blocklingTag.getInt("variant");
        naturalBlocklingType = BlocklingType.find(blocklingTag.getString("original_type"), tagVersion);
        blocklingType = BlocklingType.find(blocklingTag.getString("type"), tagVersion);
        setBlocklingScale(blocklingTag.getFloat("scale"), false);
        if (scale <= 0.0f && !level().isClientSide())
        {
            ensureBlocklingScale(false);
        }

        // Health can be overwritten when loading max health modifiers.
        float health = getHealth();

        CompoundTag equipmentInvTag = blocklingTag.getCompound("equipment_inv");

        if (equipmentInvTag != null)
        {
            equipmentInv.readFromNBT(equipmentInvTag, tagVersion);
        }

        CompoundTag statsTag = blocklingTag.getCompound("attributes");

        if (statsTag != null)
        {
            stats.readFromNBT(statsTag, tagVersion);
        }

        CompoundTag tasksTag = blocklingTag.getCompound("tasks");

        if (tasksTag != null)
        {
            tasks.readFromNBT(tasksTag, tagVersion);
        }

        CompoundTag skillsTag = blocklingTag.getCompound("skills");

        if (skillsTag != null)
        {
            skills.readFromNBT(skillsTag, tagVersion);
        }

        abilityController.readFromNBT(blocklingTag);

        equipmentInv.updateToolAttributes();
        stats.updateTypeBonuses(false);
        syncAppearanceData();

        // Set back to the saved health as this should be correct.
        setHealth(health);
    }

    public void writeSpawnData(RegistryFriendlyByteBuf buf)
    {
        syncAppearanceData();
        buf.writeInt(BlocklingType.TYPES.indexOf(naturalBlocklingType));
        buf.writeInt(BlocklingType.TYPES.indexOf(blocklingType));
        buf.writeInt(blocklingTypeVariant);
        buf.writeFloat(scale);

        equipmentInv.encode(buf);
        stats.encode(buf);
        tasks.encode(buf);
        skills.encode(buf);
    }

    public void readSpawnData(RegistryFriendlyByteBuf buf)
    {
        naturalBlocklingType = BlocklingType.getTypeByIndex(buf.readInt());
        blocklingType = BlocklingType.getTypeByIndex(buf.readInt());
        blocklingTypeVariant = buf.readInt();
        setBlocklingScale(buf.readFloat(), false);
        if (scale <= 0.0f && !level().isClientSide())
        {
            ensureBlocklingScale(false);
        }
        refreshDimensions();
        syncAppearanceData();

        equipmentInv.decode(buf);
        stats.decode(buf);
        tasks.decode(buf);
        skills.decode(buf);

        equipmentInv.updateToolAttributes();
        stats.updateTypeBonuses(false);
    }

    @Override
    public void tick()
    {
        super.tick();

        if (!level().isClientSide())
        {
            if (!hasPlayerResetCrouchBetweenInteractions)
            {
                hasPlayerResetCrouchBetweenInteractions = !isTame() || (getOwner() != null && !getOwner().isCrouching());
            }
        }

        skills.tick();
        actions.tick();
        abilityController.tick();

        checkAndUpdateCooldowns();
        
        equipmentInv.detectAndSendChanges();
    }

    @Override
    public void customServerAiStep()
    {
        super.customServerAiStep();

        // Tick the tasks just after the goal and target selectors have ticked.
        tasks.tick();
    }

    /**
     * Updates any generic passive abilities the blockling has (should be called every 20 ticks).
     */
    private void updatePassiveAbilities()
    {
        // Modular passives run from abilityController.tick() each entity tick.
    }

    /**
     * Checks and updates any cooldowns if required.
     */
    private void checkAndUpdateCooldowns()
    {
        actions.ticks20.tryStart();
        actions.regenerationCooldown.tryStart();

        if (actions.regenerationCooldown.isFinished())
        {
            if (skills.getSkill(CombatSkills.REGENERATION_3).isBought())
            {
                heal(5.0f);
            }
            else if (skills.getSkill(CombatSkills.REGENERATION_2).isBought())
            {
                heal(3.0f);
            }
            else if (skills.getSkill(CombatSkills.REGENERATION_1).isBought())
            {
                heal(1.0f);
            }
        }

        if (actions.attacksCooldown.isFinished())
        {
            attacksRecently = 0;
            stats.attackSpeedSkillMomentumModifier.setValue(0.0f);
        }

        if (actions.oresMinedCooldown.isFinished())
        {
            oresMinedRecently = 0;
            stats.miningSpeedSkillMomentumModifier.setValue(0.0f);
        }

        if (actions.logsChoppedCooldown.isFinished())
        {
            logsChoppedRecently = 0;
            stats.woodcuttingSpeedSkillMomentumModifier.setValue(0.0f);
        }

        if (actions.cropsHarvestedCooldown.isFinished())
        {
            cropsHarvestedRecently = 0;
            stats.farmingSpeedSkillMomentumModifier.setValue(0.0f);
        }
    }

    @Override
    public boolean doHurtTarget(@Nonnull Entity target)
    {
        BlocklingHand attackingHand = actions.attack.getRecentHand();
        ItemStack mainStack = getMainHandItem();
        ItemStack offStack = getOffhandItem();
        Item mainItem = mainStack.getItem();
        Item offItem = offStack.getItem();

        boolean attackingWithMainHand = attackingHand == BlocklingHand.MAIN || attackingHand == BlocklingHand.BOTH;
        boolean attackingWithOffHand = attackingHand == BlocklingHand.OFF || attackingHand == BlocklingHand.BOTH;

        boolean mainHandTinkersTool = ToolUtil.isTinkersTool(mainStack);
        boolean offHandTinkersTool = ToolUtil.isTinkersTool(offStack);

        boolean hasHurt = false;

        float tinkersDamage = 0.0f;
        float damage = 0.0f;
        float knockback = (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        int fireAspect = 0;

        if (target instanceof LivingEntity)
        {
            if (attackingWithMainHand)
            {
                if (mainHandTinkersTool && ToolUtil.isUseableTool(mainStack))
                {
                    if (TinkersConstructProxy.instance.attackEntity(mainStack, this, InteractionHand.MAIN_HAND, target, () -> 1.0, false))
                    {
                        tinkersDamage += stats.mainHandAttackDamage.getValue(); // This won't take into account Tinkers' modifiers but is good enough.
                        hasHurt = true;
                    }
                }
                else
                {
                    damage += stats.mainHandAttackDamage.getValue();
                    damage += ToolUtil.getToolEnchantmentDamage(mainStack, (LivingEntity) target);
                    knockback += ToolUtil.getToolKnockbackLevel(mainStack);
                    fireAspect += ToolUtil.getToolFireAspectLevel(mainStack);
                }
            }

            if (attackingWithOffHand)
            {
                if (offHandTinkersTool && ToolUtil.isUseableTool(offStack))
                {
                    if (TinkersConstructProxy.instance.attackEntity(offStack, this, InteractionHand.MAIN_HAND, target, () -> 1.0, false))
                    {
                        tinkersDamage += stats.offHandAttackDamage.getValue(); // This won't take into account Tinkers' modifiers but is good enough.
                        hasHurt = true;
                    }
                }
                else
                {
                    damage += stats.offHandAttackDamage.getValue();
                    damage += ToolUtil.getToolEnchantmentDamage(offStack, (LivingEntity) target);
                    knockback += ToolUtil.getToolKnockbackLevel(offStack);
                    fireAspect += ToolUtil.getToolFireAspectLevel(offStack);
                }
            }
        }

        if (fireAspect > 0)
        {
            target.igniteForSeconds(fireAspect * 4);
        }

        if (target instanceof LivingEntity)
        {
            LivingEntity livingTarget = (LivingEntity) target;

            if (skills.getSkill(CombatSkills.POISON_ATTACKS).isBought())
            {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            }
            else if (skills.getSkill(CombatSkills.WITHER_ATTACKS).isBought())
            {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 60));
            }
        }

        damage += (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (damage > 0)
        {
            int invulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            hasHurt = target.hurt(damageSources().mobAttack(this), damage);
            target.invulnerableTime = invulnerableTime;
        }

        if (hasHurt)
        {
            stats.combatXp.incrementValue((int) (damage + tinkersDamage) + 1);

            if (knockback > 0.0f)
            {
                ((LivingEntity) target).knockback(knockback * 0.5f, (double) Mth.sin(this.getYRot() * ((float) Math.PI / 180.0f)), (-Mth.cos(this.getYRot() * ((float) Math.PI / 180.0f))));
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }

            if (target instanceof Player)
            {
                Player player = (Player) target;
                maybeDisableShield(player, this.getMainHandItem(), player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY);
            }

            setLastHurtMob(target);

            if (attackingWithMainHand)
            {
                tryDamageToolOnAttack(mainStack);
            }

            if (attackingWithOffHand)
            {
                tryDamageToolOnAttack(offStack);
            }

            incAttacksRecently();
        }

        return hasHurt;
    }

    /**
     * Attempts to damage the given stack in the context of an attack.
     *
     * @param stack the stack to damage.
     */
    public void tryDamageToolOnAttack(@Nonnull ItemStack stack)
    {
        Item item = stack.getItem();

        int damage = getSkills().getSkill(CombatSkills.WRECKLESS).isBought() ? 2 : 1;

        if (ToolUtil.isTinkersTool(item))
        {
            // Tinkers' will already have applied tool damage, but won't take the wreckless skill into account.
            damage--;

            // If the tool is not a weapon then double the damage.
            if (!ToolUtil.isWeapon(item))
            {
                damage *= 2;
            }

            TinkersConstructProxy.instance.damageTool(stack, damage, this);
        }
        else
        {
            if (stack.isDamageableItem())
            {
                damage *= 2;
            }

            if (ToolUtil.damageTool(stack, this, damage))
            {
                stack.shrink(1);
            }
        }
    }

    /**
     * Copied from Mob as we need to run custom hurt target code but still need this functionality.
     */
    private void maybeDisableShield(Player p_233655_1_, ItemStack p_233655_2_, ItemStack p_233655_3_) {
        if (!p_233655_2_.isEmpty() && !p_233655_3_.isEmpty() && p_233655_2_.getItem() instanceof AxeItem && p_233655_3_.getItem() == Items.SHIELD) {
            float f = 0.25F + (float) EnchantmentHelper.getItemEnchantmentLevel(
                    level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.EFFICIENCY), getMainHandItem()) * 0.05F;
            if (this.random.nextFloat() < f) {
                p_233655_1_.getCooldowns().addCooldown(Items.SHIELD, 100);
                this.level().broadcastEntityEvent(p_233655_1_, (byte)30);
            }
        }
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float damage)
    {
        if (abilityController.onHurt(damageSource, damage) <= 0.0F)
        {
            return false;
        }

        boolean hurt = super.hurt(damageSource, damage);

        if (hurt)
        {
            abilityController.afterHurt(damageSource, damage);
        }

        if (isDeadOrDying())
        {
            BlocklingWhistleItem.onBlocklingDestroyed(this);
        }

        if (!level().isClientSide())
        {
            if (skills.getSkill(GeneralSkills.ARMADILLO).isBought())
            {
                if (isDeadOrDying())
                {
                    setHealth(1.0f);

                    dropItemStack(BlocklingItem.create(this));

                    setHealth(0.0f);

                    discard();
                }
            }
        }

        return hurt;
    }

    @Override
    @Nonnull
    public InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand InteractionHand)
    {
        InteractionResult result;

        if (InteractionHand == InteractionHand.MAIN_HAND)
        {
            result = mobInteractMainHand(player);
        }
        else
        {
            result = mobInteractOffHand(player);
        }

        if (result != InteractionResult.PASS)
        {
            return result;
        }

        return super.mobInteract(player, InteractionHand);
    }

    /**
     * Handles the player interacting with their main InteractionHand.
     *
     * @param player the interacting player.
     * @return the result of the interaction.
     */
    @Nonnull
    private InteractionResult mobInteractMainHand(@Nonnull Player player)
    {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        Item item = stack.getItem();

        if (item == BlocklingsRegistries.blocklingWhistle())
        {
            if (player == getOwner())
            {
                if (!level().isClientSide())
                {
                    BlocklingWhistleItem.setBlockling(stack, this);
                }

                return InteractionResult.sidedSuccess(level().isClientSide());
            }
        }

        InteractionResult evolveResult = tryEvolveWithFood(player, stack);

        if (evolveResult != InteractionResult.PASS)
        {
            return evolveResult;
        }

        InteractionResult primaryTypeResult = tryChangePrimaryTypeWithFood(player, stack);

        if (primaryTypeResult != InteractionResult.PASS)
        {
            return primaryTypeResult;
        }

        if (blocklingType.isFoodForType(item))
        {
            if (!level().isClientSide())
            {
                if (!isTame())
                {
                    tryTame((ServerPlayer) player, stack);

                    if (!player.getAbilities().instabuild)
                    {
                        stack.shrink(1);
                    }

                    return InteractionResult.SUCCESS;
                }
                else
                {
                    if (hasPlayerResetCrouchBetweenInteractions && skills.getSkill(GeneralSkills.PACKLING).isBought())
                    {
                        if (player == getOwner())
                        {
                            if (player.isCrouching())
                            {
                                ItemStack blocklingStack = BlocklingItem.create(this);

                                if (!player.getInventory().add(blocklingStack))
                                {
                                    dropItemStack(blocklingStack);
                                }

                                BlocklingWhistleItem.onBlocklingDestroyed(this);

                                discard();

                                if (!player.getAbilities().instabuild)
                                {
                                    stack.shrink(1);
                                }

                                return InteractionResult.SUCCESS;
                            }
                        }
                    }

                    if (hasPlayerResetCrouchBetweenInteractions && skills.getSkill(GeneralSkills.HEAL).isBought())
                    {
                        if (getHealth() < getMaxHealth())
                        {
                            heal(random.nextInt(3) + 3);

                            level().broadcastEntityEvent(this, (byte) 7);

                            if (!player.getAbilities().instabuild)
                            {
                                stack.shrink(1);
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }

        if (isTame() && player == getOwner())
        {
            if (isNameTagRenameAttempt(stack))
            {
                return InteractionResult.PASS;
            }

            if (stack.isEmpty() && player.isCrouching())
            {
                InteractionResult abilityResult = abilityController.tryActivate(player);
                if (abilityResult != InteractionResult.PASS)
                {
                    return abilityResult;
                }
            }

            if (stack.isEmpty() && !player.isCrouching())
            {
                if (!level().isClientSide())
                {
                    if (hasPlayerResetCrouchBetweenInteractions)
                    {
                        guiHandler.openGui(player);
                    }
                }

                return InteractionResult.sidedSuccess(level().isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * @return true if the player is trying to rename this blockling with a named name tag.
     */
    private static boolean isNameTagRenameAttempt(@Nonnull ItemStack stack)
    {
        return stack.is(Items.NAME_TAG) && stack.get(DataComponents.CUSTOM_NAME) != null;
    }

    /**
     * Handles the player interacting with their off InteractionHand.
     *
     * @param player the interacting player.
     * @return the result of the interaction.
     */
    @Nonnull
    private InteractionResult mobInteractOffHand(@Nonnull Player player)
    {
        ItemStack stack = player.getItemInHand(InteractionHand.OFF_HAND);
        Item item = stack.getItem();

        return InteractionResult.PASS;
    }

    /**
     * Attempts to change the blockling's type using food while the owner is crouching.
     */
    @Nonnull
    private InteractionResult tryEvolveWithFood(@Nonnull Player player, @Nonnull ItemStack stack)
    {
        Item item = stack.getItem();

        if (!isTame() || player != getOwner() || !player.isCrouching() || !BlocklingType.isFood(item))
        {
            return InteractionResult.PASS;
        }

        BlocklingType newType = BlocklingType.findTypeForFood(item);

        if (newType == null)
        {
            return InteractionResult.PASS;
        }

        if (level().isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        hasPlayerResetCrouchBetweenInteractions = false;

        BlocklingType previousNatural = naturalBlocklingType;

        if (random.nextInt(4) == 0)
        {
            setNaturalBlocklingType(newType);

            if (blocklingType == previousNatural)
            {
                setBlocklingType(newType);
            }

            level().broadcastEntityEvent(this, (byte) 7);
        }
        else
        {
            level().broadcastEntityEvent(this, (byte) 6);
        }

        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Attempts to change the blockling's primary type using food (without crouching).
     */
    @Nonnull
    private InteractionResult tryChangePrimaryTypeWithFood(@Nonnull Player player, @Nonnull ItemStack stack)
    {
        Item item = stack.getItem();

        if (!isTame() || player != getOwner() || player.isCrouching() || !BlocklingType.isFood(item))
        {
            return InteractionResult.PASS;
        }

        BlocklingType newType = BlocklingType.findTypeForFood(item);

        if (newType == null || newType == blocklingType)
        {
            return InteractionResult.PASS;
        }

        if (level().isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        hasPlayerResetCrouchBetweenInteractions = false;

        if (random.nextInt(4) == 0)
        {
            setBlocklingType(newType);
            level().broadcastEntityEvent(this, (byte) 7);
        }
        else
        {
            level().broadcastEntityEvent(this, (byte) 6);
        }

        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Attempts to tame the blockling with a 1 in 3 chance.
     *
     * @param player the player interacting with the blockling.
     * @param stack  the stack involved in the interaction.
     */
    private void tryTame(@Nonnull ServerPlayer player, @Nonnull ItemStack stack)
    {
        if (random.nextInt(3) == 0 && Services.PLATFORM.allowAnimalTame(this, player))
        {
            tame(player);
            level().broadcastEntityEvent(this, (byte) 7);
        }
        else
        {
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @Override
    public void tame(@Nonnull Player player)
    {
        super.tame(player);
        setPersistenceRequired();

        if (!hasCustomName())
        {
            setCustomName(Component.literal("Blockling"), true);
        }

        for (Task task : getTasks().getPrioritisedTasks())
        {
            if (task.isConfigured() && task.getType() == BlocklingTasks.WANDER)
            {
                task.setType(BlocklingTasks.FOLLOW, false);
            }
        }

        navigation.stop();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) { super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        for (int i = 0; i < equipmentInv.getContainerSize(); i++)
        {
            ItemStack stack = equipmentInv.getItem(i);

            if (!stack.isEmpty())
            {
                spawnAtLocation(stack);
            }
        }
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) { if (!skills.getSkill(GeneralSkills.ARMADILLO).isBought()) { super.dropAllDeathLoot(level, damageSource);
        }
    }

    @Override
    @Nonnull
    public ItemStack getMainHandItem()
    {
        return getItemInHand(InteractionHand.MAIN_HAND);
    }

    @Override
    @Nonnull
    public ItemStack getOffhandItem()
    {
        return getItemInHand(InteractionHand.OFF_HAND);
    }

    @Override
    @Nonnull
    public ItemStack getItemInHand(@Nonnull InteractionHand InteractionHand)
    {
        return equipmentInv.getHandStack(InteractionHand);
    }

    @Override
    public void setItemInHand(@Nonnull InteractionHand InteractionHand, @Nonnull ItemStack stack)
    {
        equipmentInv.setHandStack(InteractionHand, stack);
    }

    @Override
    @Nonnull
    public Iterable<ItemStack> getHandSlots()
    {
        BlocklingHand attackingHand = actions.attack.getRecentHand();

        if (attackingHand == BlocklingHand.MAIN)
        {
            return Collections.singletonList(getMainHandItem());
        }
        else if (attackingHand == BlocklingHand.OFF)
        {
            return Collections.singletonList(getOffhandItem());
        }

        return Arrays.asList(getMainHandItem(), getOffhandItem());
    }

    @Override
    @Nonnull
    public Iterable<ItemStack> getArmorSlots()
    {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    public Iterable<ItemStack> getAllSlots()
    {
        return Iterables.concat(getHandSlots(), getArmorSlots());
    }

    @Override
    public boolean hasItemInSlot(@Nonnull EquipmentSlot slotType)
    {
        if (slotType == EquipmentSlot.MAINHAND)
        {
            return !getMainHandItem().isEmpty();
        }
        else if (slotType == EquipmentSlot.OFFHAND)
        {
            return !getOffhandItem().isEmpty();
        }

        return false;
    }

    @Override
    @Nonnull
    public ItemStack getItemBySlot(@Nonnull EquipmentSlot slotType)
    {
        if (slotType == EquipmentSlot.MAINHAND)
        {
            return getMainHandItem();
        }
        else if (slotType == EquipmentSlot.OFFHAND)
        {
            return getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@Nonnull EquipmentSlot slotType, @Nonnull ItemStack stack)
    {
        if (slotType == EquipmentSlot.MAINHAND)
        {
            setItemInHand(InteractionHand.MAIN_HAND, stack);
        }
        else if (slotType == EquipmentSlot.OFFHAND)
        {
            setItemInHand(InteractionHand.OFF_HAND, stack);
        }
    }

    /**
     * @return true as the blockling needs to be created before it can decide whether it can spawn.
     */
    public static boolean checkBlocklingSpawnRules(EntityType<BlocklingEntity> entityType, net.minecraft.world.level.ServerLevelAccessor world, MobSpawnType reason, BlockPos pos, RandomSource random)
    {
        // Always allow placement probe; real filtering is in checkSpawnRules (needs entity instance).
        BlocklingSpawnDiagnostics.onPlacementCheck(world, reason, pos, true);
        return true;
    }

    /**
     * Picks a blockling type that is valid for the current world position.
     * Used by natural spawning and generic (untyped) item placement.
     * Accepts LevelAccessor because chunk generation passes a WorldGenRegion, not a Level.
     *
     * @return true if a valid type was applied
     */
    public boolean chooseSpawnTypeForLocation(@Nonnull net.minecraft.world.level.LevelAccessor world, @Nonnull MobSpawnType reason)
    {
        List<BlocklingType> candidates = new ArrayList<>();
        int rolledOut = 0;
        int predicateFailed = 0;

        for (BlocklingType type : BlocklingType.TYPES)
        {
            if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
            {
                if (random.nextInt(type.spawnRateReduction) != 0)
                {
                    if (!(type == BlocklingType.GRASS && isGrassPreferredBiome(world) && random.nextInt(100) < 5))
                    {
                        rolledOut++;
                        continue;
                    }
                }
            }

            BlocklingType previousNatural = this.naturalBlocklingType;
            BlocklingType previousPrimary = this.blocklingType;
            this.naturalBlocklingType = type;
            this.blocklingType = type;

            boolean matches = true;
            for (BiPredicate<BlocklingEntity, net.minecraft.world.level.LevelAccessor> predicate : type.spawnPredicates)
            {
                if (!predicate.test(this, world))
                {
                    matches = false;
                    break;
                }
            }

            this.naturalBlocklingType = previousNatural;
            this.blocklingType = previousPrimary;

            if (matches)
            {
                candidates.add(type);
            }
            else
            {
                predicateFailed++;
            }
        }

        if (candidates.isEmpty())
        {
            BlocklingSpawnDiagnostics.onChooseTypeEmpty(world, reason, blockPosition(), rolledOut, predicateFailed);
            return false;
        }

        BlocklingType chosen = candidates.get(random.nextInt(candidates.size()));
        setNaturalBlocklingType(chosen, false);
        setBlocklingType(chosen, false);
        stats.updateTypeBonuses(false);
        setHealth(getMaxHealth());
        return true;
    }

    @Override
    public boolean checkSpawnRules(@Nonnull net.minecraft.world.level.LevelAccessor world, @Nonnull MobSpawnType reason)
    {
        // Do NOT require instanceof Level — chunk generation uses WorldGenRegion (LevelAccessor only).
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
        {
            BlockPos support = blockPosition().below();
            if (!world.getBlockState(support).canOcclude())
            {
                BlocklingSpawnDiagnostics.onRulesRejectedNoSupport(world, reason, support);
                return false;
            }

            List<BlocklingEntity> nearbyBlocklings = List.of();
            if (world instanceof Level level)
            {
                final int radius = 64;
                AABB area = new AABB(
                        support.getX() - radius, level.getMinBuildHeight(), support.getZ() - radius,
                        support.getX() + radius, level.getMaxBuildHeight(), support.getZ() + radius);
                nearbyBlocklings = new ArrayList<>(level.getEntitiesOfClass(BlocklingEntity.class, area));
                nearbyBlocklings.removeIf(other -> other == this);

                if (nearbyBlocklings.size() >= 3)
                {
                    BlocklingSpawnDiagnostics.onRulesRejectedNearbyCap(world, reason, nearbyBlocklings.size());
                    return false;
                }
            }

            if (!chooseSpawnTypeForLocation(world, reason))
            {
                BlocklingSpawnDiagnostics.onRulesRejectedNoType(world, reason, blockPosition());
                return false;
            }

            if (!nearbyBlocklings.isEmpty()
                    && nearbyBlocklings.stream().anyMatch(blockling -> blockling.getBlocklingType() == blocklingType))
            {
                BlocklingSpawnDiagnostics.onRulesRejectedDupType(world, reason, blocklingType.key);
                return false;
            }

            BlocklingSpawnDiagnostics.onRulesAccepted(world, reason, blocklingType.key, blockPosition());
        }

        return true;
    }

    private static final TagKey<Biome> GRASS_PREFERRED_BIOMES_PLAINS = TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("is_plains"));
    private static final TagKey<Biome> GRASS_PREFERRED_BIOMES_FOREST = TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("is_forest"));

    private boolean isGrassPreferredBiome(@Nonnull net.minecraft.world.level.LevelAccessor world)
    {
        var biome = world.getBiome(blockPosition());
        return biome.is(GRASS_PREFERRED_BIOMES_PLAINS) || biome.is(GRASS_PREFERRED_BIOMES_FOREST);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return !isTame();
    }

    @Override
    public boolean canChangeDimensions(@Nonnull Level fromLevel, @Nonnull Level toLevel)
    {
        return isTame();
    }

    @Override
    public EntityDimensions getDefaultDimensions(@Nonnull Pose pose)
    {
        float s = getBlocklingScale();
        if (s <= 0.0f)
        {
            s = 1.0f;
        }
        return EntityDimensions.scalable(s, s).withEyeHeight(s * 0.45f);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel world, @Nonnull AgeableMob entity)
    {
        return null;
    }

    /**
     * Drops the given stack at the blockling's location.
     *
     * @param stack the stack to drop.
     */
    public void dropItemStack(@Nonnull ItemStack stack)
    {
        level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.2f, getZ(), stack));
    }

    @Override
    public boolean fireImmune()
    {
        return abilityController.isFireImmune();
    }

    /**
     * Sets the name of the blockling.
     * Does NOT sync to client/server.
     *
     * @param name the new name.
     */
    @Override
    public void setCustomName(@Nullable Component name)
    {
        if (name != null)
        {
            name = Component.literal(name.getString());
        }

        setCustomName((Component) name, false);
    }

    /**
     * Sets the name of the blockling.
     * Syncs to the server if set from the client and sync is true.
     *
     * @param name the new name.
     * @param sync whether to sync to the server from the client.
     */
    public void setCustomName(@Nullable Component name, boolean sync)
    {
        super.setCustomName(name);

        if (level().isClientSide() && sync)
        {
            new BlocklingNameMessage(this, name).sync();
        }
    }

    /**
     * Sets the current target to the given entity.
     * Syncs to the client/server.
     *
     * @param target the new target.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target)
    {
        setTarget(target, true);
    }

    /**
     * Sets the current target to the given entity.
     * Syncs to the client/server if sync is true.
     *
     * @param target the new target.
     * @param sync whether to sync to the client/server.
     */
    public void setTarget(@Nullable LivingEntity target, boolean sync)
    {
        super.setTarget(target);

        if (sync)
        {
            new BlocklingAttackTargetMessage(this, target).sync();
        }
    }

    /**
     * @return the original blockling type.
     */
    @Nonnull
    public BlocklingType getNaturalBlocklingType()
    {
        return naturalBlocklingType;
    }

    /**
     * Sets the natural blockling type to the given blockling type.
     * Syncs to the client/server.
     *
     * @param blocklingType the new blockling type.
     */
    public void setNaturalBlocklingType(@Nonnull BlocklingType blocklingType)
    {
        setNaturalBlocklingType(blocklingType, true);
    }

    /**
     * Sets the natural blockling type to the given blockling type.
     * Syncs to the client/server if sync is true.
     *
     * @param blocklingType the new blockling type.
     * @param sync whether to sync to the client/server.
     */
    public void setNaturalBlocklingType(@Nonnull BlocklingType blocklingType, boolean sync)
    {
        this.naturalBlocklingType = blocklingType;
        syncAppearanceData();

        stats.updateTypeBonuses(sync);
        equipmentInv.updateToolAttributes();

        if (sync)
        {
            new BlocklingTypeMessage(this, blocklingType, true).sync();
        }
    }

    /**
     * @return the current blockling type.
     */
    @Nonnull
    public BlocklingType getBlocklingType()
    {
        return blocklingType;
    }

    /**
     * Sets the current blockling type to the given blockling type.
     * Syncs to the client/server.
     *
     * @param blocklingType the new blockling type.
     */
    public void setBlocklingType(@Nonnull BlocklingType blocklingType)
    {
        setBlocklingType(blocklingType, true);
    }

    /**
     * Sets the current blockling type to the given blockling type.
     * Syncs to the client/server if sync is true.
     *
     * @param blocklingType the new blockling type.
     * @param sync whether to sync to the client/server.
     */
    public void setBlocklingType(@Nonnull BlocklingType blocklingType, boolean sync)
    {
        this.blocklingType = blocklingType;
        syncAppearanceData();

        stats.updateTypeBonuses(sync);
        equipmentInv.updateToolAttributes();

        if (sync)
        {
            new BlocklingTypeMessage(this, blocklingType, false).sync();
        }
    }

    /**
     * @return the current variant.
     */
    public int getBlocklingTypeVariant()
    {
        return blocklingTypeVariant;
    }

    /**
     * @return the blockling's attribute manager.
     */
    @Nonnull
    public BlocklingAttributes getStats()
    {
        return stats;
    }

    /**
     * @return the blockling's skill manager.
     */
    @Nonnull
    public BlocklingSkills getSkills()
    {
        return skills;
    }

    /**
     * @return the blockling's task manager.
     */
    @Nonnull
    public BlocklingTasks getTasks()
    {
        return tasks;
    }

    /**
     * @return the blockling's action manager.
     */
    @Nonnull
    public BlocklingActions getActions()
    {
        return actions;
    }

    /**
     * @return the blockling's equipment inventory.
     */
    public EquipmentInventory getEquipment()
    {
        return equipmentInv;
    }

    /**
     * @return the blockling's size scale. Do not override {@link Entity#getScale()} — that is used by vanilla rendering.
     */
    public float getBlocklingScale()
    {
        return scale > 0.0f ? scale : 1.0f;
    }

    /**
     * Sets the blockling's size scale.
     * Syncs to the client/server.
     *
     * @param scale the new scale.
     */
    public void setBlocklingScale(float scale)
    {
        setBlocklingScale(scale, true);
    }

    /**
     * Sets the blockling's size scale.
     * Syncs to the client/server if sync is true.
     *
     * @param scale the new scale.
     * @param sync whether to sync to the client/server.
     */
    public void setBlocklingScale(float scale, boolean sync)
    {
        this.scale = scale;

        refreshDimensions();
        syncAppearanceData();

        if (sync)
        {
            new BlocklingScaleMessage(this, scale).sync();
        }
    }

    /**
     * Assigns a random spawn scale when none has been set yet (natural/chunk spawns).
     * Prefer {@link #ensureBlocklingScale(boolean, boolean)} when the spawn reason is known.
     */
    public void ensureBlocklingScale(boolean sync)
    {
        ensureBlocklingScale(sync, true);
    }

    /**
     * Ensures the blockling has a valid scale.
     *
     * @param sync whether to sync the scale packet
     * @param randomize if true (natural spawn), pick a random size; if false (items/eggs), use size 1.0
     */
    public void ensureBlocklingScale(boolean sync, boolean randomize)
    {
        if (scale <= 0.0f && !level().isClientSide())
        {
            float value = randomize ? (getRandom().nextFloat() * 0.5f + 0.45f) : 1.0f;
            setBlocklingScale(value, sync);
        }
    }

    /**
     * Increments the count of attacks recently and resets the cooldown.
     */
    public void incAttacksRecently()
    {
        attacksRecently++;
        actions.attacksCooldown.start();

        if (skills.getSkill(CombatSkills.MOMENTUM).isBought())
        {
            int cappedCount = Math.min(attacksRecently, 20);
            stats.attackSpeedSkillMomentumModifier.setValue((float) cappedCount / 2.0f);
        }
    }

    /**
     * Increments the count of ores mined recently and resets the cooldown.
     */
    public void incOresMinedRecently()
    {
        oresMinedRecently++;
        actions.oresMinedCooldown.start();

        if (skills.getSkill(MiningSkills.MOMENTUM).isBought())
        {
            int cappedCount = Math.min(oresMinedRecently, 20);
            stats.miningSpeedSkillMomentumModifier.setValue((float) cappedCount);
        }
    }

    /**
     * Increments the count of logs chopped recently and resets the cooldown.
     */
    public void incLogsChoppedRecently()
    {
        logsChoppedRecently++;
        actions.logsChoppedCooldown.start();

        if (skills.getSkill(WoodcuttingSkills.MOMENTUM).isBought())
        {
            int cappedCount = Math.min(logsChoppedRecently, 20);
            stats.woodcuttingSpeedSkillMomentumModifier.setValue((float) cappedCount);
        }
    }

    /**
     * Increments the count of crops harvested recently and resets the cooldown.
     */
    public void incCropsHarvestedRecently()
    {
        cropsHarvestedRecently++;
        actions.cropsHarvestedCooldown.start();

        if (skills.getSkill(FarmingSkills.MOMENTUM).isBought())
        {
            int cappedCount = Math.min(cropsHarvestedRecently, 20);
            stats.farmingSpeedSkillMomentumModifier.setValue((float) cappedCount);
        }
    }
}
