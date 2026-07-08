package com.willr27.blocklings.entity.blockling.skill.info;

import com.willr27.blocklings.entity.blockling.skill.Skill;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

/**
 * Info regarding the general properties of a skill.
 */
public class SkillGeneralInfo
{
    @Nonnull
    public final Skill.Type type;

    @Nonnull
    public final Component name;

    @Nonnull
    public final Component desc;

    public SkillGeneralInfo(@Nonnull Skill.Type type, @Nonnull String key)
    {
        this.type = type;
        this.name = BlocklingsTranslationTextComponent.of("skill." + key + ".name");
        this.desc = BlocklingsTranslationTextComponent.of("skill." + key + ".desc");
    }
}
