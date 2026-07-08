package com.willr27.blocklings.client.gui.screen.screens;

import com.willr27.blocklings.client.gui.screen.BlocklingsScreen;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/** Dev test screen stub — full test harness not required for gameplay GUI. */
@OnlyIn(Dist.CLIENT)
public class TestScreen extends BlocklingsScreen
{
    public TestScreen(@Nonnull BlocklingEntity blockling)
    {
        super(blockling);
    }
}
