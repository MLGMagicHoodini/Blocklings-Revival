package com.willr27.blocklings.entity.blockling;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.loader.BlocklingsRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Spawns a one-time pack of wild blocklings near a player on first join
 * (does not repeat on reconnect).
 */
public final class BlocklingStarterSpawn
{
    private BlocklingStarterSpawn()
    {
    }

    /**
     * Schedules the starter pack after a short delay so chunks around the player are ready.
     */
    public static void onPlayerLogin(@Nonnull ServerPlayer player)
    {
        if (!BlocklingsConfig.COMMON.spawn.starterEnabled())
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (BlocklingStarterSpawnData.get(level).hasReceived(player.getUUID()))
        {
            return;
        }

        MinecraftServer server = level.getServer();
        int delay = Math.max(1, BlocklingsConfig.COMMON.spawn.starterDelayTicks());
        server.tell(new TickTask(server.getTickCount() + delay, () ->
        {
            if (!player.hasDisconnected())
            {
                trySpawnPack(player);
            }
        }));
    }

    /**
     * Attempts to spawn the starter pack. Marks the player as done only if at least one
     * blockling was placed (so a failed attempt can retry next login).
     *
     * @return number of blocklings spawned
     */
    public static int trySpawnPack(@Nonnull ServerPlayer player)
    {
        if (!BlocklingsConfig.COMMON.spawn.starterEnabled())
        {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlocklingStarterSpawnData data = BlocklingStarterSpawnData.get(level);
        if (data.hasReceived(player.getUUID()))
        {
            return 0;
        }

        int wanted = BlocklingsConfig.COMMON.spawn.starterCount();
        int radius = BlocklingsConfig.COMMON.spawn.starterRadius();
        if (wanted <= 0)
        {
            data.markReceived(player.getUUID());
            return 0;
        }

        int spawned = 0;
        RandomSource random = player.getRandom();
        for (int attempt = 0; attempt < wanted * 8 && spawned < wanted; attempt++)
        {
            BlockPos pos = findNearbySpawnPos(level, player.position(), radius, random);
            if (pos == null)
            {
                continue;
            }

            if (spawnOne(level, pos, random, player))
            {
                spawned++;
            }
        }

        if (spawned > 0)
        {
            data.markReceived(player.getUUID());
            Blocklings.LOGGER.info("Starter spawn: {} blockling(s) near {} ({})",
                    spawned, player.getGameProfile().getName(), player.getUUID());
        }
        else
        {
            Blocklings.LOGGER.warn("Starter spawn failed for {} — will retry next login",
                    player.getGameProfile().getName());
        }

        return spawned;
    }

    private static boolean spawnOne(@Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random,
                                    @Nonnull ServerPlayer owner)
    {
        BlocklingEntity blockling = BlocklingsRegistries.blocklingEntity().create(level);
        if (blockling == null)
        {
            return false;
        }

        blockling.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                random.nextFloat() * 360.0f, 0.0f);

        // EVENT bypasses nearbyCap; MOB_SUMMONED also randomizes size in finalizeSpawn.
        if (!blockling.chooseSpawnTypeForLocation(level, MobSpawnType.EVENT))
        {
            blockling.discard();
            return false;
        }

        blockling.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        if (!level.addFreshEntity(blockling))
        {
            blockling.discard();
            return false;
        }

        // Starter blocklings are a gift: tame them to the player so they obey commands
        // (follow, tasks, skills) immediately instead of roaming as unmanageable wild mobs.
        // tame() also sets persistence, names them and switches their default task to FOLLOW.
        blockling.tame(owner);
        return true;
    }

    @Nullable
    private static BlockPos findNearbySpawnPos(@Nonnull ServerLevel level, @Nonnull Vec3 origin,
                                               int radius, @Nonnull RandomSource random)
    {
        int r = Math.max(2, radius);
        for (int i = 0; i < 12; i++)
        {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = 3.0D + random.nextDouble() * Math.max(1.0D, r - 2);
            int x = Mth.floor(origin.x + Math.cos(angle) * dist);
            int z = Mth.floor(origin.z + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos support = feet.below();

            if (!level.getWorldBorder().isWithinBounds(feet))
            {
                continue;
            }
            if (!level.getBlockState(support).canOcclude())
            {
                continue;
            }
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty())
            {
                continue;
            }
            if (!level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty())
            {
                continue;
            }
            if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty())
            {
                continue;
            }
            return feet;
        }
        return null;
    }
}
