package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class BlocklingsSounds {
    public static final SoundEvent BLOCKLING_WHISTLE = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_whistle"),
            SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_whistle")));

    private BlocklingsSounds() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blocklings sounds");
    }
}
