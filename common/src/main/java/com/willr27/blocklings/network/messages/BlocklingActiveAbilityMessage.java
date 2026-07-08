package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.ability.TypeFamily;
import com.willr27.blocklings.network.BlocklingMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * Syncs active ability cooldown after server activation.
 */
public class BlocklingActiveAbilityMessage extends BlocklingMessage<BlocklingActiveAbilityMessage>
{
    private TypeFamily family;
    private int cooldownTicks;

    public BlocklingActiveAbilityMessage()
    {
        super(null);
    }

    public BlocklingActiveAbilityMessage(@Nonnull BlocklingEntity blockling, @Nonnull TypeFamily family, int cooldownTicks)
    {
        super(blockling);
        this.family = family;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);
        buf.writeEnum(family);
        buf.writeVarInt(cooldownTicks);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);
        family = buf.readEnum(TypeFamily.class);
        cooldownTicks = buf.readVarInt();
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        // Cooldown is tracked server-side; client may use this later for HUD feedback.
    }
}
