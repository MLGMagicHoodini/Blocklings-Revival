package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.network.BlocklingMessage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Syncs a single equipment slot. Item stacks are sent as NBT using the world's
 * full {@link RegistryAccess} (includes enchantments) — never via
 * {@code BuiltInRegistries}-only access, which crashes on enchanted tools.
 */
public class EquipmentInventoryMessage extends BlocklingMessage<EquipmentInventoryMessage> {
    private int index;
    /** {@code null} means empty stack. */
    @Nullable
    private CompoundTag stackTag;

    public EquipmentInventoryMessage() {
        super(null);
    }

    public EquipmentInventoryMessage(@Nonnull BlocklingEntity blockling, int index, @Nonnull ItemStack stack) {
        super(blockling);
        this.index = index;
        this.stackTag = saveStack(blockling.level().registryAccess(), stack);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf) {
        super.encode(buf);
        buf.writeInt(index);
        buf.writeNbt(stackTag);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf) {
        super.decode(buf);
        index = buf.readInt();
        stackTag = buf.readNbt();
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling) {
        blockling.getEquipment().setItem(index, loadStack(blockling.level().registryAccess(), stackTag));
    }

    @Nullable
    private static CompoundTag saveStack(@Nonnull RegistryAccess access, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Tag saved = stack.save(access);
        return saved instanceof CompoundTag compound ? compound : null;
    }

    @Nonnull
    private static ItemStack loadStack(@Nonnull RegistryAccess access, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(access, tag);
    }
}
