package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes.Level;
import com.willr27.blocklings.entity.blockling.attribute.attributes.numbers.IntAttribute;
import com.willr27.blocklings.network.Message;
import com.willr27.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import com.willr27.blocklings.loader.LoaderEnvironment;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SetXpCommandMessage extends Message
{
    private Level level;
    private int value;

    public SetXpCommandMessage(@Nonnull Level level, int value)
    {
        this.level = level;
        this.value = value;
    }

    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        FriendlyByteBufUtils.writeString(buf, level.name());
        buf.writeInt(value);
    }

    @Nonnull
    public static SetXpCommandMessage decode(@Nonnull FriendlyByteBuf buf)
    {
        return new SetXpCommandMessage(Level.valueOf(FriendlyByteBufUtils.readString(buf)), buf.readInt());
    }

    @Override
    public void handle(@Nonnull Player player)
    {
        if (!LoaderEnvironment.isClient()) {
            return;
        }

        Entity entity = Minecraft.getInstance().crosshairPickEntity;
        if (!(entity instanceof BlocklingEntity blockling)) {
            return;
        }

        if (level == Level.TOTAL) {
            for (Level lvl : Arrays.stream(Level.values()).filter(l -> l != Level.TOTAL).collect(Collectors.toList())) {
                ((IntAttribute) blockling.getStats().getLevelXpAttribute(lvl)).setValue(value);
            }
        } else {
            ((IntAttribute) blockling.getStats().getLevelXpAttribute(level)).setValue(value);
        }
    }
}
