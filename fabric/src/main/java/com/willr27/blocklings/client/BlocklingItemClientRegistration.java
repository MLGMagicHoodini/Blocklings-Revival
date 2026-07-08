package com.willr27.blocklings.client;

import com.willr27.blocklings.Blocklings;

/**
 * Creative / dropped blockling items use {@code custom_model_data} (see BlocklingItem.applyTypeModel).
 * Kept as a client hook in case future predicates are needed.
 */
public final class BlocklingItemClientRegistration {
    private BlocklingItemClientRegistration() {
    }

    public static void registerItemModelsProperties() {
        Blocklings.LOGGER.debug("Blockling item models use CustomModelData overrides");
    }
}
