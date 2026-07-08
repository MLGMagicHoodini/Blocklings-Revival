package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.network.Message;
import com.willr27.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import com.willr27.blocklings.loader.LoaderEnvironment;

import javax.annotation.Nonnull;

public class SetTypeCommandMessage extends Message
{
    private String type;
    private boolean natural;

    public SetTypeCommandMessage(@Nonnull String type, boolean natural)
    {
        this.type = type;
        this.natural = natural;
    }

    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        FriendlyByteBufUtils.writeString(buf, type);
        buf.writeBoolean(natural);
    }

    @Nonnull
    public static SetTypeCommandMessage decode(@Nonnull FriendlyByteBuf buf)
    {
        return new SetTypeCommandMessage(FriendlyByteBufUtils.readString(buf), buf.readBoolean());
    }

    @Override
    public void handle(@Nonnull Player player)
    {
        if (!LoaderEnvironment.isClient()) {
            return;
        }

        Entity entity = Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit
                ? entityHit.getEntity()
                : null;
        if (entity == null) {
            entity = Minecraft.getInstance().crosshairPickEntity;
        }

        if (entity instanceof BlocklingEntity blockling) {
            if (natural) {
                blockling.setNaturalBlocklingType(BlocklingType.find(type));
            } else {
                blockling.setBlocklingType(BlocklingType.find(type));
            }
        }
    }
}
