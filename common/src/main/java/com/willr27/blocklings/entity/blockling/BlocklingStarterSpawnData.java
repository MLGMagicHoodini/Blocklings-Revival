package com.willr27.blocklings.entity.blockling;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players already received the one-time near-player starter pack.
 * Stored on the overworld so it survives reconnects and dimension changes.
 */
public final class BlocklingStarterSpawnData extends SavedData
{
    public static final String DATA_ID = "blocklings_starter_spawn";

    @Nonnull
    private final Set<UUID> completedPlayers = new HashSet<>();

    public BlocklingStarterSpawnData()
    {
    }

    @Nonnull
    public static BlocklingStarterSpawnData load(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider provider)
    {
        BlocklingStarterSpawnData data = new BlocklingStarterSpawnData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("id"))
            {
                data.completedPlayers.add(entry.getUUID("id"));
            }
        }
        return data;
    }

    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        for (UUID id : completedPlayers)
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            list.add(entry);
        }
        tag.put("players", list);
        return tag;
    }

    @Nonnull
    public static BlocklingStarterSpawnData get(@Nonnull ServerLevel anyLevel)
    {
        ServerLevel overworld = anyLevel.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null)
        {
            overworld = anyLevel;
        }
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        BlocklingStarterSpawnData::new,
                        BlocklingStarterSpawnData::load,
                        null),
                DATA_ID);
    }

    public boolean hasReceived(@Nonnull UUID playerId)
    {
        return completedPlayers.contains(playerId);
    }

    public void markReceived(@Nonnull UUID playerId)
    {
        if (completedPlayers.add(playerId))
        {
            setDirty();
        }
    }

    public boolean clear(@Nonnull UUID playerId)
    {
        if (completedPlayers.remove(playerId))
        {
            setDirty();
            return true;
        }
        return false;
    }
}
