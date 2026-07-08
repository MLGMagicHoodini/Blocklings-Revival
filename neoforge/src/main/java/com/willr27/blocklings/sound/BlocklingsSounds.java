package com.willr27.blocklings.sound;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlocklingsSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Blocklings.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCKLING_WHISTLE = SOUNDS.register("blockling_whistle",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_whistle")));

    private BlocklingsSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
