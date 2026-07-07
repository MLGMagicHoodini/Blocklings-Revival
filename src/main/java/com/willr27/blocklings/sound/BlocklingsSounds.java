package com.willr27.blocklings.sound;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.util.BlocklingsResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
/*import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;*/

public class BlocklingsSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(NeoForgeRegistries.SOUND_EVENTS, Blocklings.MODID);

    public static final DeferredRegister<SoundEvent> BLOCKLING_WHISTLE = SOUNDS.register("blockling_whistle",
            () -> new SoundEvent(new BlocklingsResourceLocation("blockling_whistle")));

    public static void register(IEventBus modEventBus)
    {
        SOUNDS.register(modEventBus);
    }
}
