package com.willr27.blocklings.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.willr27.blocklings.config.BlocklingSpawnConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingSpawnDiagnostics;
import com.willr27.blocklings.entity.blockling.BlocklingStarterSpawn;
import com.willr27.blocklings.entity.blockling.BlocklingStarterSpawnData;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.loader.BlocklingsRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OP-only spawn / world diagnostics for Blocklings (shared by NeoForge + Fabric).
 * <p>
 * Commands:
 * <ul>
 *   <li>{@code /blocklingdevtool} — summary</li>
 *   <li>{@code /blocklingdevtool count}</li>
 *   <li>{@code /blocklingdevtool nearby [radius]}</li>
 *   <li>{@code /blocklingdevtool list [radius] [limit]} — world counts + nearby list with coords</li>
 *   <li>{@code /blocklingdevtool here}</li>
 *   <li>{@code /blocklingdevtool config}</li>
 *   <li>{@code /blocklingdevtool debug &lt;true|false&gt;}</li>
 *   <li>{@code /blocklingdevtool biomes}</li>
 *   <li>{@code /blocklingdevtool starter} — force starter pack now</li>
 *   <li>{@code /blocklingdevtool starter reset} — clear one-time flag for self</li>
 * </ul>
 * Also available under {@code /blockling devtool ...}.
 */
public final class BlocklingDevTool
{
    private static final double DEFAULT_NEARBY_RADIUS = 128.0D;
    private static final int DEFAULT_LIST_LIMIT = 20;

    private BlocklingDevTool()
    {
    }

    /**
     * Registers {@code /blocklingdevtool} and attaches {@code /blockling devtool} under the given root builder.
     */
    public static void register(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher,
                                @Nonnull LiteralArgumentBuilder<CommandSourceStack> blocklingRoot)
    {
        LiteralArgumentBuilder<CommandSourceStack> tree = buildTree("blocklingdevtool");
        dispatcher.register(tree);
        blocklingRoot.then(buildTree("devtool"));
    }

    @Nonnull
    private static LiteralArgumentBuilder<CommandSourceStack> buildTree(@Nonnull String literal)
    {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(2))
                .executes(BlocklingDevTool::summary)
                .then(Commands.literal("count").executes(BlocklingDevTool::countAll))
                .then(Commands.literal("nearby")
                        .executes(ctx -> nearby(ctx, DEFAULT_NEARBY_RADIUS))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0D, 512.0D))
                                .executes(ctx -> nearby(ctx, DoubleArgumentType.getDouble(ctx, "radius")))))
                .then(Commands.literal("list")
                        .executes(ctx -> listAll(ctx, DEFAULT_NEARBY_RADIUS, DEFAULT_LIST_LIMIT))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0D, 512.0D))
                                .executes(ctx -> listAll(ctx, DoubleArgumentType.getDouble(ctx, "radius"), DEFAULT_LIST_LIMIT))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> listAll(
                                                ctx,
                                                DoubleArgumentType.getDouble(ctx, "radius"),
                                                IntegerArgumentType.getInteger(ctx, "limit"))))))
                .then(Commands.literal("here").executes(BlocklingDevTool::testHere))
                .then(Commands.literal("config").executes(BlocklingDevTool::showConfig))
                .then(Commands.literal("debug")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(BlocklingDevTool::setDebug)))
                .then(Commands.literal("biomes").executes(BlocklingDevTool::dumpBiomes))
                .then(Commands.literal("starter")
                        .executes(BlocklingDevTool::forceStarter)
                        .then(Commands.literal("reset").executes(BlocklingDevTool::resetStarter)))
                .then(Commands.literal("tasktest")
                        .executes(BlocklingDevTool::taskTestStart)
                        .then(Commands.literal("stop").executes(BlocklingDevTool::taskTestStop)));
    }

    /**
     * Starts writing a detailed task log for the blockling the player is looking at (or the nearest
     * one within a few blocks). See {@link BlocklingTaskLogger}.
     */
    private static int taskTestStart(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        BlocklingEntity target = findLookedAtBlockling(player);
        if (target == null)
        {
            List<BlocklingEntity> near = findNearby(player, 6.0D);
            near.sort(Comparator.comparingDouble(player::distanceToSqr));
            target = near.isEmpty() ? null : near.get(0);
        }

        if (target == null)
        {
            source.sendFailure(Component.literal(
                    "[Blockling DevTool] Regarde un blockling (ou approche-toi d'un) puis relance /blocklingdevtool tasktest."));
            return 0;
        }

        java.nio.file.Path file = BlocklingTaskLogger.start(target, player);
        if (file == null)
        {
            source.sendFailure(Component.literal("[Blockling DevTool] Impossible de créer le fichier de log."));
            return 0;
        }

        final BlocklingEntity watched = target;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Log des tâches démarré pour %s.%nFichier: %s%nArrête avec /blocklingdevtool tasktest stop.",
                watched.getName().getString(), file.toAbsolutePath())), false);
        return 1;
    }

    private static int taskTestStop(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();

        boolean wasWatching = BlocklingTaskLogger.isWatching();
        java.nio.file.Path file = BlocklingTaskLogger.currentFile();
        BlocklingTaskLogger.stop();

        if (wasWatching)
        {
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[Blockling DevTool] Log des tâches arrêté.%s",
                    file != null ? "\nFichier: " + file.toAbsolutePath() : "")), false);
        }
        else
        {
            source.sendSuccess(() -> Component.literal("[Blockling DevTool] Aucun log en cours."), false);
        }
        return 1;
    }

    @Nullable
    private static BlocklingEntity findLookedAtBlockling(@Nonnull ServerPlayer player)
    {
        double reach = 20.0D;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition(1.0f);
        net.minecraft.world.phys.Vec3 view = player.getViewVector(1.0f);
        net.minecraft.world.phys.Vec3 end = eye.add(view.scale(reach));
        AABB search = player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0D);

        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, search,
                entity -> entity instanceof BlocklingEntity && entity.isAlive(), reach * reach);

        if (hit != null && hit.getEntity() instanceof BlocklingEntity blockling)
        {
            return blockling;
        }
        return null;
    }

    private static int summary(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldScan scan = scanWorld(server);

        BlocklingSpawnConfig spawn = BlocklingsConfig.COMMON.spawn;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Loaded blocklings: %d (wild=%d, tamed=%d, small=%.2f-0.95: %d, full-size≈1.0: %d)",
                scan.total, scan.wild, scan.tamed, 0.45f, scan.small, scan.fullSize)), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Spawn config: enabled=%s cap=%d radius=%.0f preventDup=%s | debug=%s",
                spawn.isEnabled(), spawn.cap(), spawn.radius(), spawn.preventDuplicates(),
                BlocklingSpawnDiagnostics.isEnabled())), false);
        source.sendSuccess(() -> Component.literal(
                "Subcommands: count | nearby [r] | list [r] [limit] | here | config | debug <true|false> | biomes"), false);

        if (scan.total == 0)
        {
            source.sendSuccess(() -> Component.literal(
                    "No blocklings loaded. Natural spawns are strict (grass + sky for grass/dirt, etc.). " +
                            "Use /blocklingdevtool here near grass, or a spawn egg."), false);
        }

        return scan.total;
    }

    private static int countAll(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        WorldScan scan = scanWorld(source.getServer());

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Total loaded: %d", scan.total)), false);

        sendDimensionCounts(source, scan);
        sendTypeCounts(source, scan);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Wild=%d | Tamed=%d | Small(scale<0.95)=%d | Full-size=%d",
                scan.wild, scan.tamed, scan.small, scan.fullSize)), false);

        return scan.total;
    }

    private static int nearby(@Nonnull CommandContext<CommandSourceStack> ctx, double radius)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        List<BlocklingEntity> found = findNearby(player, radius);
        long wild = found.stream().filter(b -> !b.isTame()).count();
        long tamed = found.size() - wild;
        long small = found.stream().filter(b -> b.getBlocklingScale() < 0.95f).count();

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Nearby (r=%.0f): %d (wild=%d, tamed=%d, small=%d)",
                radius, found.size(), wild, tamed, small)), false);

        return found.size();
    }

    /**
     * World/dimension counts (numbers), then a nearby list with coordinates.
     */
    private static int listAll(@Nonnull CommandContext<CommandSourceStack> ctx, double radius, int limit)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        WorldScan scan = scanWorld(source.getServer());

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] World totals: %d blockling(s)", scan.total)), false);
        sendDimensionCounts(source, scan);
        sendTypeCounts(source, scan);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Wild=%d | Tamed=%d | Small=%d | Full-size=%d",
                scan.wild, scan.tamed, scan.small, scan.fullSize)), false);

        List<BlocklingEntity> found = findNearby(player, radius);
        found.sort(Comparator.comparingDouble(b -> b.distanceToSqr(player)));

        int shown = Math.min(limit, found.size());
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Nearby list (%d/%d within %.0f blocks of player):", shown, found.size(), radius)), false);

        if (found.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("  (none nearby)"), false);
            return scan.total;
        }

        for (int i = 0; i < shown; i++)
        {
            BlocklingEntity blockling = found.get(i);
            BlockPos pos = blockling.blockPosition();
            String type = blockling.getBlocklingType() != null ? blockling.getBlocklingType().key : "?";
            float scale = blockling.getBlocklingScale();
            double dist = Math.sqrt(blockling.distanceToSqr(player));
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "  #%d %s scale=%.2f %s dist=%.1f @ %s %d %d %d",
                    blockling.getId(),
                    type,
                    scale,
                    blockling.isTame() ? "tamed" : "wild",
                    dist,
                    blockling.level().dimension().location(),
                    pos.getX(), pos.getY(), pos.getZ())), false);
        }

        if (found.size() > shown)
        {
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "  ... and %d more (raise limit)", found.size() - shown)), false);
        }

        return scan.total;
    }

    private static void sendDimensionCounts(@Nonnull CommandSourceStack source, @Nonnull WorldScan scan)
    {
        if (scan.perDimension.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("By dimension: (none)"), false);
            return;
        }

        source.sendSuccess(() -> Component.literal("By dimension:"), false);
        scan.perDimension.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                        "  %s: %d", entry.getKey(), entry.getValue())), false));
    }

    private static void sendTypeCounts(@Nonnull CommandSourceStack source, @Nonnull WorldScan scan)
    {
        if (scan.perType.isEmpty())
        {
            return;
        }

        source.sendSuccess(() -> Component.literal("By type:"), false);
        scan.perType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                        "  %s: %d", entry.getKey(), entry.getValue())), false));
    }

    private static int testHere(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        BlockPos support = feet.below();

        BlocklingSpawnConfig spawn = BlocklingsConfig.COMMON.spawn;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Spawn test at %d %d %d (below=%s, biome=%s)",
                feet.getX(), feet.getY(), feet.getZ(),
                level.getBlockState(support).getBlock(),
                level.getBiome(feet).unwrapKey().map(k -> k.location().toString()).orElse("?"))), false);

        if (!spawn.isEnabled())
        {
            source.sendFailure(Component.literal("FAIL: Spawn.enabled=false in config."));
            return 0;
        }

        if (!level.getBlockState(support).canOcclude())
        {
            source.sendFailure(Component.literal("FAIL: block under feet is not opaque (support)."));
            return 0;
        }

        final double radius = spawn.radius();
        AABB area = new AABB(
                support.getX() - radius, level.getMinBuildHeight(), support.getZ() - radius,
                support.getX() + radius, level.getMaxBuildHeight(), support.getZ() + radius);
        List<BlocklingEntity> nearby = new ArrayList<>(level.getEntitiesOfClass(BlocklingEntity.class, area));
        nearby.removeIf(BlocklingEntity::isTame);
        int cap = spawn.cap();
        if (cap > 0 && nearby.size() >= cap)
        {
            source.sendFailure(Component.literal(String.format(Locale.ROOT,
                    "FAIL: nearby cap (%d wild in r=%.0f, cap=%d).", nearby.size(), radius, cap)));
            return 0;
        }

        BlocklingEntity probe = BlocklingsRegistries.blocklingEntity().create(level);
        if (probe == null)
        {
            source.sendFailure(Component.literal("FAIL: could not create probe entity."));
            return 0;
        }

        probe.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, 0.0f, 0.0f);
        boolean ok = probe.chooseSpawnTypeForLocation(level, MobSpawnType.NATURAL);
        String typeKey = probe.getBlocklingType() != null ? probe.getBlocklingType().key : "?";
        probe.discard();

        if (!ok)
        {
            source.sendFailure(Component.literal(
                    "FAIL: no type matches here (e.g. grass/dirt need grass + sky; " +
                            "stone/iron need stone/ore nearby, not grass)."));
            return 0;
        }

        if (spawn.preventDuplicates()
                && nearby.stream().anyMatch(b -> b.getBlocklingType() != null && b.getBlocklingType().key.equals(typeKey)))
        {
            source.sendFailure(Component.literal(String.format(Locale.ROOT,
                    "FAIL: preventDuplicateNearbyType — a wild %s is already nearby.", typeKey)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "OK: a natural spawn could pick type=%s here (nearby wild=%d/%s).",
                typeKey, nearby.size(), cap > 0 ? Integer.toString(cap) : "∞")), false);
        return 1;
    }

    private static int showConfig(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        BlocklingSpawnConfig spawn = BlocklingsConfig.COMMON.spawn;

        source.sendSuccess(() -> Component.literal("[Blockling DevTool] Spawn config:"), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "  enabled=%s  nearbyCap=%d  nearbyRadius=%.1f  preventDuplicateNearbyType=%s",
                spawn.isEnabled(), spawn.cap(), spawn.radius(), spawn.preventDuplicates())), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "  starter: enabled=%s count=%d radius=%d delay=%d",
                spawn.starterEnabled(), spawn.starterCount(), spawn.starterRadius(), spawn.starterDelayTicks())), false);
        source.sendSuccess(() -> Component.literal("  Per-type (enabled / weight):"), false);

        for (BlocklingType type : BlocklingType.TYPES)
        {
            BlocklingSpawnConfig.TypeConfig typeConfig = spawn.forType(type);
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "    %s: enabled=%s weight=%d",
                    type.key, typeConfig.isEnabled(), typeConfig.weight())), false);
        }

        return 1;
    }

    private static int setDebug(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");

        if (enabled)
        {
            BlocklingSpawnDiagnostics.enable(BlocklingsRegistries.blocklingEntity());
            source.sendSuccess(() -> Component.literal(
                    "[Blockling DevTool] Spawn debug ON — search [SpawnDebug] in latest.log"), false);
            BlocklingSpawnDiagnostics.dumpBiomeRegistrations(source.getServer());
        }
        else
        {
            BlocklingSpawnDiagnostics.disable();
            source.sendSuccess(() -> Component.literal("[Blockling DevTool] Spawn debug OFF"), false);
        }

        return 1;
    }

    private static int dumpBiomes(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        if (!BlocklingSpawnDiagnostics.isEnabled())
        {
            BlocklingSpawnDiagnostics.enable(BlocklingsRegistries.blocklingEntity());
        }
        BlocklingSpawnDiagnostics.dumpBiomeRegistrations(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "[Blockling DevTool] Biome dump written to latest.log ([SpawnDebug])."), false);
        return 1;
    }

    private static int forceStarter(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        BlocklingStarterSpawnData.get(player.serverLevel()).clear(player.getUUID());
        int spawned = BlocklingStarterSpawn.trySpawnPack(player);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[Blockling DevTool] Starter pack spawned %d blockling(s).", spawned)), false);
        return spawned > 0 ? 1 : 0;
    }

    private static int resetStarter(@Nonnull CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }

        boolean cleared = BlocklingStarterSpawnData.get(player.serverLevel()).clear(player.getUUID());
        source.sendSuccess(() -> Component.literal(cleared
                ? "[Blockling DevTool] Starter flag cleared — reconnect (or /blocklingdevtool starter) to spawn again."
                : "[Blockling DevTool] Starter flag was already clear."), false);
        return 1;
    }

    @Nonnull
    private static List<BlocklingEntity> findNearby(@Nonnull ServerPlayer player, double radius)
    {
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(radius, Math.max(radius, 64.0D), radius);
        return new ArrayList<>(level.getEntitiesOfClass(BlocklingEntity.class, area));
    }

    @Nonnull
    private static WorldScan scanWorld(@Nonnull MinecraftServer server)
    {
        WorldScan scan = new WorldScan();

        for (ServerLevel level : server.getAllLevels())
        {
            List<BlocklingEntity> list = new ArrayList<>(level.getEntities(
                    EntityTypeTest.forClass(BlocklingEntity.class),
                    entity -> true));

            if (!list.isEmpty())
            {
                scan.perDimension.put(level.dimension().location().toString(), list.size());
            }

            for (BlocklingEntity blockling : list)
            {
                scan.total++;
                if (blockling.isTame())
                {
                    scan.tamed++;
                }
                else
                {
                    scan.wild++;
                }

                float scale = blockling.getBlocklingScale();
                if (scale < 0.95f)
                {
                    scan.small++;
                }
                else
                {
                    scan.fullSize++;
                }

                String type = blockling.getBlocklingType() != null ? blockling.getBlocklingType().key : "?";
                scan.perType.merge(type, 1, Integer::sum);
            }
        }

        return scan;
    }

    private static final class WorldScan
    {
        int total;
        int wild;
        int tamed;
        int small;
        int fullSize;
        final Map<String, Integer> perDimension = new HashMap<>();
        final Map<String, Integer> perType = new HashMap<>();
    }
}
