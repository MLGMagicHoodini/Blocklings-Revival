package com.willr27.blocklings.platform.services;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

public interface IMenuHelper
{
    void openEquipmentMenu(@Nonnull ServerPlayer player, @Nonnull BlocklingEntity blockling);
}
