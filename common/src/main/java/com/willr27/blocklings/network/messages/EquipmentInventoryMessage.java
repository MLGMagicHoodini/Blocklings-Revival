package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.network.BlocklingMessage;
import com.willr27.blocklings.util.FriendlyByteBufUtils;
import com.willr27.blocklings.util.RegistryUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class EquipmentInventoryMessage extends BlocklingMessage<EquipmentInventoryMessage> {
    private int index;
    private ItemStack stack;

    public EquipmentInventoryMessage() {
        super(null);
    }

    public EquipmentInventoryMessage(@Nonnull BlocklingEntity blockling, int index, @Nonnull ItemStack stack) {
        super(blockling);
        this.index = index;
        this.stack = stack;
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf) {
        super.encode(buf);
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, RegistryUtil.registryAccess());
        buf.writeInt(index);
        FriendlyByteBufUtils.writeItemStack(registryBuf, stack);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf) {
        super.decode(buf);
        RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, RegistryUtil.registryAccess());
        index = buf.readInt();
        stack = FriendlyByteBufUtils.readItemStack(registryBuf);
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling) {
        blockling.getEquipment().setItem(index, stack);
    }
}
