package com.willr27.blocklings.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public class FriendlyByteBufUtils
{
    public static String readString(FriendlyByteBuf buf)
    {
        return buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString();
    }

    public static void writeString(FriendlyByteBuf buf, String string)
    {
        buf.writeInt(string.getBytes(StandardCharsets.UTF_8).length);
        buf.writeCharSequence(string, StandardCharsets.UTF_8);
    }

    /**
     * Writes an item stack that may be empty. {@link ItemStack#STREAM_CODEC} rejects empty stacks in 1.21+.
     */
    public static void writeItemStack(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull ItemStack stack)
    {
        if (stack.isEmpty())
        {
            buf.writeBoolean(false);
        }
        else
        {
            buf.writeBoolean(true);
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }
    }

    @Nonnull
    public static ItemStack readItemStack(@Nonnull RegistryFriendlyByteBuf buf)
    {
        return buf.readBoolean() ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
    }
}
