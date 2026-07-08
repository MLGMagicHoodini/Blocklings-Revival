package com.willr27.blocklings.entity.blockling.ability.handlers;

import com.willr27.blocklings.config.BlocklingAbilityConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.ability.*;

import javax.annotation.Nonnull;

abstract class AbstractFamilyHandler implements BlocklingAbilityHandler
{
    private final TypeFamily family;
    private final BlocklingTypeProfile profile;

    protected AbstractFamilyHandler(@Nonnull TypeFamily family, @Nonnull BlocklingTypeProfile profile)
    {
        this.family = family;
        this.profile = profile;
    }

    @Override
    @Nonnull
    public TypeFamily family()
    {
        return family;
    }

    @Override
    @Nonnull
    public BlocklingTypeProfile profile()
    {
        return profile;
    }

    @Nonnull
    protected BlocklingAbilityConfig.FamilyConfig config()
    {
        return BlocklingsConfig.COMMON.abilities.forKey(switch (family)
        {
            case WOOD -> "wood";
            default -> family.name().toLowerCase();
        });
    }

    protected boolean passiveEnabled()
    {
        return BlocklingsConfig.COMMON.abilities.enabled.get() && config().passiveEnabled.get();
    }

    protected boolean activeEnabled()
    {
        return BlocklingsConfig.COMMON.abilities.enabled.get() && config().activeEnabled.get();
    }

    @Override
    public int activeCooldownTicks(@Nonnull com.willr27.blocklings.entity.blockling.BlocklingEntity blockling)
    {
        return config().activeCooldownSeconds.get() * 20;
    }

    protected int activeDurationTicks()
    {
        return config().activeDurationSeconds.get() * 20;
    }

    protected int radius()
    {
        return config().radius.get();
    }

    protected double passiveChance()
    {
        return config().passiveChance.get();
    }

    @Nonnull
    protected BlocklingAbilityController controller(@Nonnull com.willr27.blocklings.entity.blockling.BlocklingEntity blockling)
    {
        return blockling.getAbilityController();
    }
}
