package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.network.Message;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * Server → client: re-applies full blockling spawn data (type, tasks, equipment, stats, skills).
 * Used after dimension changes and when a player starts tracking a blockling.
 */
public class BlocklingSpawnSyncMessage extends Message
{
    private int blocklingEntityId;
    private byte[] spawnData = new byte[0];

    public BlocklingSpawnSyncMessage()
    {
    }

    public BlocklingSpawnSyncMessage(@Nonnull BlocklingEntity blockling)
    {
        this.blocklingEntityId = blockling.getId();

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), blockling.level().registryAccess());
        try
        {
            blockling.writeSpawnData(buffer);
            spawnData = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), spawnData);
        }
        finally
        {
            buffer.release();
        }
    }

    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        buf.writeInt(blocklingEntityId);
        buf.writeByteArray(spawnData);
    }

    @Nonnull
    public static BlocklingSpawnSyncMessage decode(@Nonnull FriendlyByteBuf buf)
    {
        BlocklingSpawnSyncMessage message = new BlocklingSpawnSyncMessage();
        message.blocklingEntityId = buf.readInt();
        message.spawnData = buf.readByteArray();
        return message;
    }

    @Override
    public void handle(@Nonnull Player player)
    {
        if (!player.level().isClientSide())
        {
            return;
        }

        Entity entity = player.level().getEntity(blocklingEntityId);
        if (!(entity instanceof BlocklingEntity blockling))
        {
            return;
        }

        if (spawnData.length == 0)
        {
            return;
        }

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(spawnData), player.level().registryAccess());
        try
        {
            blockling.readSpawnData(buffer);
        }
        finally
        {
            buffer.release();
        }

        blockling.getEquipment().updateToolAttributes();
        blockling.getStats().updateTypeBonuses(false);
    }
}
