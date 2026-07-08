package com.willr27.blocklings.entity.blockling;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fabric-only spawn diagnostics. NeoForge leaves this disabled (no-op until {@link #enable()}).
 * Look for log lines starting with {@code [SpawnDebug]}.
 */
public final class BlocklingSpawnDiagnostics {
    public static final Logger LOG = LogManager.getLogger("blocklings/spawn");

    private static volatile boolean enabled = false;
    @Nullable
    private static EntityType<?> trackedType = null;

    private static final AtomicLong placementChecks = new AtomicLong();
    private static final AtomicLong placementPass = new AtomicLong();
    private static final AtomicLong rulesChecks = new AtomicLong();
    private static final AtomicLong rulesPass = new AtomicLong();
    private static final AtomicLong failNoSupport = new AtomicLong();
    private static final AtomicLong failNearbyCap = new AtomicLong();
    private static final AtomicLong failNoType = new AtomicLong();
    private static final AtomicLong failDupType = new AtomicLong();
    private static final AtomicLong finalizeOk = new AtomicLong();
    private static final AtomicLong typePickOk = new AtomicLong();

    private static long lastSummaryMs = 0L;
    private static final long SUMMARY_INTERVAL_MS = 10_000L;

    private BlocklingSpawnDiagnostics() {
    }

    public static void enable(@Nonnull EntityType<?> blocklingType) {
        enabled = true;
        trackedType = blocklingType;
        LOG.info("[SpawnDebug] ENABLED for {} — grep latest.log for [SpawnDebug]",
                BuiltInRegistries.ENTITY_TYPE.getKey(blocklingType));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void onPlacementCheck(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull BlockPos pos, boolean passed) {
        if (!enabled) {
            return;
        }
        placementChecks.incrementAndGet();
        if (passed) {
            placementPass.incrementAndGet();
        }
        maybeSummarize();
    }

    public static void onRulesRejectedNoSupport(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull BlockPos support) {
        if (!enabled) {
            return;
        }
        rulesChecks.incrementAndGet();
        failNoSupport.incrementAndGet();
        sampleReject(world, reason, "no_opaque_support", support, String.valueOf(world.getBlockState(support).getBlock()));
        maybeSummarize();
    }

    public static void onRulesRejectedNearbyCap(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, int nearby) {
        if (!enabled) {
            return;
        }
        rulesChecks.incrementAndGet();
        failNearbyCap.incrementAndGet();
        sampleReject(world, reason, "nearby_cap", BlockPos.ZERO, "nearby=" + nearby);
        maybeSummarize();
    }

    public static void onRulesRejectedNoType(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull BlockPos pos) {
        if (!enabled) {
            return;
        }
        rulesChecks.incrementAndGet();
        failNoType.incrementAndGet();
        String below = String.valueOf(world.getBlockState(pos.below()).getBlock());
        sampleReject(world, reason, "no_matching_type", pos, "below=" + below + " biome=" + biomeId(world, pos)
                + " grass=" + world.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK));
        maybeSummarize();
    }

    public static void onRulesRejectedDupType(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull String typeKey) {
        if (!enabled) {
            return;
        }
        rulesChecks.incrementAndGet();
        failDupType.incrementAndGet();
        sampleReject(world, reason, "duplicate_type", BlockPos.ZERO, "type=" + typeKey);
        maybeSummarize();
    }

    public static void onRulesAccepted(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull String typeKey, @Nonnull BlockPos pos) {
        if (!enabled) {
            return;
        }
        rulesChecks.incrementAndGet();
        rulesPass.incrementAndGet();
        typePickOk.incrementAndGet();
        LOG.info("[SpawnDebug] ACCEPT reason={} type={} pos={} biome={} below={}",
                reason, typeKey, pos, biomeId(world, pos), world.getBlockState(pos.below()).getBlock());
        maybeSummarize();
    }

    public static void onFinalizeSpawn(@Nonnull BlocklingEntity blockling, @Nonnull MobSpawnType reason) {
        if (!enabled) {
            return;
        }
        finalizeOk.incrementAndGet();
        BlockPos pos = blockling.blockPosition();
        String typeKey = blockling.getBlocklingType() != null ? blockling.getBlocklingType().key : "?";
        LOG.info("[SpawnDebug] FINALIZE id={} reason={} type={} pos={} biome={}",
                blockling.getId(), reason, typeKey, pos, biomeId(blockling.level(), pos));
    }

    public static void onChooseTypeEmpty(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason, @Nonnull BlockPos pos,
                                         int rolledOut, int predicateFailed) {
        if (!enabled) {
            return;
        }
        long n = failNoType.get();
        if (n <= 5 || n % 100 == 0) {
            LOG.info("[SpawnDebug] type_pick_empty reason={} pos={} biome={} below={} rolledOut={} predicFail={} grassBelow={}",
                    reason, pos, biomeId(world, pos), world.getBlockState(pos.below()).getBlock(),
                    rolledOut, predicateFailed, world.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK));
        }
    }

    /**
     * Dumps whether blocklings appear in CREATURE spawn lists for diagnostic biomes.
     */
    public static void dumpBiomeRegistrations(@Nonnull MinecraftServer server) {
        if (!enabled || trackedType == null) {
            return;
        }

        ServerLevel overworld = server.overworld();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(trackedType);
        LOG.info("[SpawnDebug] ===== BIOME SPAWN DUMP (CREATURE) type={} =====", typeId);
        dumpOneBiome(overworld, ResourceLocation.withDefaultNamespace("plains"));
        dumpOneBiome(overworld, ResourceLocation.withDefaultNamespace("forest"));
        dumpOneBiome(overworld, ResourceLocation.withDefaultNamespace("stony_shore"));
        dumpOneBiome(overworld, ResourceLocation.withDefaultNamespace("beach"));
        dumpOneBiome(overworld, ResourceLocation.withDefaultNamespace("ocean"));
        LOG.info("[SpawnDebug] =======================================");
    }

    private static void dumpOneBiome(@Nonnull ServerLevel level, @Nonnull ResourceLocation biomeId) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, biomeId);
        var reg = level.registryAccess().registryOrThrow(Registries.BIOME);
        var holder = reg.getHolder(key);
        if (holder.isEmpty()) {
            LOG.info("[SpawnDebug] biome {} NOT FOUND in registry", biomeId);
            return;
        }

        Biome biome = holder.get().value();
        MobSpawnSettings settings = biome.getMobSettings();
        float creatureProb = settings.getCreatureProbability();
        List<MobSpawnSettings.SpawnerData> creatures = settings.getMobs(MobCategory.CREATURE).unwrap();
        boolean hasBlockling = false;
        int weight = -1;
        int min = -1;
        int max = -1;
        for (MobSpawnSettings.SpawnerData data : creatures) {
            if (data.type == trackedType) {
                hasBlockling = true;
                weight = data.getWeight().asInt();
                min = data.minCount;
                max = data.maxCount;
                break;
            }
        }

        LOG.info("[SpawnDebug] biome={} creatureProb={} creatureEntries={} hasBlockling={} weight={} group={}-{}",
                biomeId, creatureProb, creatures.size(), hasBlockling, weight, min, max);
    }

    private static void sampleReject(@Nonnull LevelAccessor world, @Nonnull MobSpawnType reason,
                                     @Nonnull String cause, @Nonnull BlockPos pos, @Nullable String detail) {
        long totalFails = failNoSupport.get() + failNearbyCap.get() + failNoType.get() + failDupType.get();
        if (totalFails <= 8 || totalFails % 100 == 0) {
            LOG.info("[SpawnDebug] REJECT cause={} reason={} pos={} detail={} worldClass={}",
                    cause, reason, pos, detail, world.getClass().getSimpleName());
        }
    }

    private static void maybeSummarize() {
        long now = System.currentTimeMillis();
        if (now - lastSummaryMs < SUMMARY_INTERVAL_MS) {
            return;
        }
        lastSummaryMs = now;
        LOG.info("[SpawnDebug] SUMMARY place={}/{} rules={}/{} fail[support={} nearby={} noType={} dupType={}] chooseOk={} finalize={}",
                placementPass.get(), placementChecks.get(),
                rulesPass.get(), rulesChecks.get(),
                failNoSupport.get(), failNearbyCap.get(), failNoType.get(), failDupType.get(),
                typePickOk.get(), finalizeOk.get());
    }

    @Nonnull
    private static String biomeId(@Nonnull LevelAccessor world, @Nonnull BlockPos pos) {
        try {
            return world.getBiome(pos).unwrapKey()
                    .map(ResourceKey::location)
                    .map(ResourceLocation::toString)
                    .orElse("?");
        }
        catch (Exception e) {
            Blocklings.LOGGER.debug("SpawnDebug biome lookup failed", e);
            return "?";
        }
    }
}
