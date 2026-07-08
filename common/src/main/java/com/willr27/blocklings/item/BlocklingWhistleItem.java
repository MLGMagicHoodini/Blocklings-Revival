package com.willr27.blocklings.item;

import com.willr27.blocklings.entity.blockling.BlocklingDimensionTravel;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.loader.LoaderEnvironment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlocklingWhistleItem extends Item {
    @Nonnull
    public static final String BLOCKLING_UUID_KEY = "blockling_uuid";
    @Nonnull
    public static final String BLOCKLING_NAME_KEY = "blockling_name";
    @Nonnull
    public static final Map<UUID, Set<ItemStack>> BLOCKLINGS_TO_WHISTLES = new ConcurrentHashMap<>();

    public BlocklingWhistleItem() {
        super(new Item.Properties().stacksTo(1).durability(64));
    }

    private static CompoundTag getTag(@Nonnull ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setTag(@Nonnull ItemStack stack, @Nonnull CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setBlockling(@Nonnull ItemStack stack, @Nonnull BlocklingEntity blockling) {
        CompoundTag tag = getTag(stack);
        tag.putUUID(BLOCKLING_UUID_KEY, blockling.getUUID());
        setTag(stack, tag);
        addStackToMap(blockling, stack);
    }

    public static void addStackToMap(@Nonnull BlocklingEntity blockling, @Nonnull ItemStack stack) {
        BLOCKLINGS_TO_WHISTLES.computeIfAbsent(blockling.getUUID(), ignored -> ConcurrentHashMap.newKeySet()).add(stack);
    }

    public static void onBlocklingDestroyed(@Nonnull BlocklingEntity blockling) {
        Set<ItemStack> stacks = BLOCKLINGS_TO_WHISTLES.remove(blockling.getUUID());
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                CompoundTag tag = getTag(stack);
                tag.remove(BLOCKLING_UUID_KEY);
                tag.remove(BLOCKLING_NAME_KEY);
                setTag(stack, tag);
            }
        }
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world instanceof ServerLevel serverLevel) {
            CompoundTag tag = getTag(stack);
            if (tag.hasUUID(BLOCKLING_UUID_KEY)) {
                UUID blocklingUuid = tag.getUUID(BLOCKLING_UUID_KEY);
                BlocklingEntity blockling = BlocklingDimensionTravel.findBlocklingOnServer(serverLevel, blocklingUuid);
                if (blockling != null && player == blockling.getOwner()) {
                    if (blockling.level() != serverLevel) {
                        BlocklingDimensionTravel.transferTo(blockling, serverLevel, player.getX(), player.getY(), player.getZ());
                    }
                    else {
                        blockling.teleportTo(player.getX(), player.getY(), player.getZ());
                    }

                    stack.hurtAndBreak(1, serverLevel, (ServerPlayer) player, item -> {
                    });
                    serverLevel.playSound(null, player.blockPosition(), BlocklingsRegistries.blocklingWhistleSound(), SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level world, @Nonnull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        CompoundTag tag = getTag(stack);
        if (!tag.hasUUID(BLOCKLING_UUID_KEY)) {
            return;
        }
        if (world.isClientSide()) {
            BlocklingEntity blockling = findBlockling(stack, (ClientLevel) world);
            if (blockling != null) {
                addStackToMap(blockling, stack);
                String name = findBlocklingName(stack, (ClientLevel) world);
                if (name != null) {
                    tag.putString(BLOCKLING_NAME_KEY, name);
                    setTag(stack, tag);
                }
            }
        }         else if (world instanceof ServerLevel serverLevel) {
            BlocklingEntity blockling = BlocklingDimensionTravel.findBlocklingOnServer(serverLevel, tag.getUUID(BLOCKLING_UUID_KEY));
            if (blockling != null) {
                addStackToMap(blockling, stack);
                tag.putString(BLOCKLING_NAME_KEY, blockling.getCustomName().getString());
                setTag(stack, tag);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private BlocklingEntity findBlockling(@Nonnull ItemStack stack, @Nonnull ClientLevel world) {
        CompoundTag tag = getTag(stack);
        if (!tag.hasUUID(BLOCKLING_UUID_KEY)) {
            return null;
        }
        UUID id = tag.getUUID(BLOCKLING_UUID_KEY);
        for (Entity entity : world.entitiesForRendering()) {
            if (entity.getUUID().equals(id) && entity instanceof BlocklingEntity blockling) {
                return blockling;
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private String findBlocklingName(@Nonnull ItemStack stack, @Nonnull ClientLevel world) {
        BlocklingEntity blockling = findBlockling(stack, world);
        return blockling != null && blockling.getCustomName() != null ? blockling.getCustomName().getString() : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private String findBlocklingLocation(@Nonnull ItemStack stack, @Nonnull ClientLevel world) {
        BlocklingEntity blockling = findBlockling(stack, world);
        return blockling != null ? String.format("%d %d %d", (int) blockling.getX(), (int) blockling.getY(), (int) blockling.getZ()) : null;
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag.contains(BLOCKLING_NAME_KEY)) {
            return Component.literal(super.getName(stack).getString() + " (" + tag.getString(BLOCKLING_NAME_KEY) + ")")
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        if (LoaderEnvironment.isClient()) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level instanceof ClientLevel clientLevel) {
                String location = findBlocklingLocation(stack, clientLevel);
                if (location != null) {
                    tooltip.add(Component.translatable(getDescriptionId() + ".location", location).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
