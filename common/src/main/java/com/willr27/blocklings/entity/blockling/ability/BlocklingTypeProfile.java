package com.willr27.blocklings.entity.blockling.ability;

import javax.annotation.Nonnull;

/**
 * Describes the ability identity of a blockling family.
 */
public record BlocklingTypeProfile(
        @Nonnull TypeFamily family,
        @Nonnull String passiveKey,
        @Nonnull String activeKey,
        @Nonnull BlocklingSpecialty gatheringSpecialty,
        @Nonnull BlocklingSpecialty combatSpecialty,
        @Nonnull BlocklingSpecialty environmentalSpecialty
)
{
}
