package com.willr27.blocklings.command;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.BlocklingGoal;
import com.willr27.blocklings.entity.blockling.task.Task;
import com.willr27.blocklings.util.ToolType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Developer aid: records, to a text file, exactly what a single watched blockling is doing each
 * tick (active task, target, held tools, attack target) plus discrete events (goal state changes,
 * blocks gathered). Enable with {@code /blocklingdevtool tasktest} while looking at a blockling.
 *
 * <p>Server-thread only — all blockling AI, interactions and commands run there, so no locking.
 */
public final class BlocklingTaskLogger
{
    @Nullable
    private static volatile UUID watchedUuid;
    @Nullable
    private static BufferedWriter writer;
    @Nullable
    private static Path currentFile;
    @Nonnull
    private static String lastSnapshot = "";
    private static int startTick;

    private BlocklingTaskLogger()
    {
    }

    /**
     * Starts logging the given blockling to a fresh file, replacing any previous watch.
     *
     * @return the path of the created log file, or {@code null} if it could not be created.
     */
    @Nullable
    public static Path start(@Nonnull BlocklingEntity blockling, @Nonnull ServerPlayer player)
    {
        stop();

        try
        {
            Path dir = player.server.getServerDirectory().resolve("blockling-tasklogs");
            Files.createDirectories(dir);

            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            Path file = dir.resolve("tasklog-" + stamp + ".txt");

            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            currentFile = file;
            watchedUuid = blockling.getUUID();
            startTick = blockling.tickCount;
            lastSnapshot = "";

            raw("=== Blockling task log ===");
            raw("blockling: " + blockling.getUUID() + " (" + blockling.getName().getString() + ")");
            raw("owner: " + (blockling.getOwnerUUID() != null ? blockling.getOwnerUUID() : "none")
                    + "  tame: " + blockling.isTame());
            raw("started by: " + player.getGameProfile().getName() + " at tick " + startTick);
            raw("");
            dumpSetup(blockling);
            raw("format: [t+ticks] snapshot|EVENT ...");
            raw("---------------------------------");
            flush();

            return file;
        }
        catch (IOException e)
        {
            stop();
            return null;
        }
    }

    public static boolean isWatching()
    {
        return watchedUuid != null;
    }

    @Nullable
    public static Path currentFile()
    {
        return currentFile;
    }

    public static boolean isWatched(@Nonnull BlocklingEntity blockling)
    {
        UUID uuid = watchedUuid;
        return uuid != null && uuid.equals(blockling.getUUID());
    }

    /**
     * Records a per-tick state snapshot, but only when it differs from the previous one
     * (keeps the file readable instead of thousands of identical lines).
     */
    public static void snapshot(@Nonnull BlocklingEntity blockling)
    {
        if (writer == null || !isWatched(blockling))
        {
            return;
        }

        String snap = buildSnapshot(blockling);
        if (snap.equals(lastSnapshot))
        {
            return;
        }
        lastSnapshot = snap;
        line(blockling, snap);
    }

    /**
     * Records a discrete event (goal state change, block gathered, attack, …).
     */
    public static void event(@Nonnull BlocklingEntity blockling, @Nonnull String category, @Nonnull String detail)
    {
        if (writer == null || !isWatched(blockling))
        {
            return;
        }
        line(blockling, "EVENT " + category + " | " + detail);
    }

    /**
     * Dumps the blockling's configured tasks and available tools once at start — this is usually
     * enough to explain "why won't it work?" (no task assigned, or no matching tool).
     */
    private static void dumpSetup(@Nonnull BlocklingEntity blockling)
    {
        raw("tasks (priority order):");
        boolean anyTask = false;
        for (Task task : blockling.getTasks().getPrioritisedTasks())
        {
            anyTask = true;
            BlocklingGoal goal = task.getGoal();
            String type = task.getType() != null ? task.getType().name.getString() : "unset";
            String state = goal != null ? goal.getState().toString() : "no-goal";
            raw("  - " + task.getCustomName() + " [type=" + type
                    + " configured=" + task.isConfigured() + " state=" + state + "]");
        }
        if (!anyTask)
        {
            raw("  (none — this blockling has no tasks assigned!)");
        }

        raw("tools: main=" + itemName(blockling.getMainHandItem())
                + " off=" + itemName(blockling.getOffhandItem()));
        StringBuilder inv = new StringBuilder("inventory tools:");
        for (ToolType type : ToolType.values())
        {
            inv.append(" ").append(type).append("=").append(hasToolInInventory(blockling, type) ? "yes" : "no");
        }
        raw(inv.toString());
        raw("");
    }

    private static boolean hasToolInInventory(@Nonnull BlocklingEntity blockling, @Nonnull ToolType type)
    {
        for (int i = 0; i < blockling.getEquipment().getContainerSize(); i++)
        {
            if (type.is(blockling.getEquipment().getItem(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static String buildSnapshot(@Nonnull BlocklingEntity blockling)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("pos=").append(blockling.blockPosition().toShortString());
        sb.append(" main=").append(itemName(blockling.getMainHandItem()));
        sb.append(" off=").append(itemName(blockling.getOffhandItem()));

        LivingEntity attackTarget = blockling.getTarget();
        if (attackTarget == null)
        {
            sb.append(" attackTarget=-");
        }
        else
        {
            // Include target HP so active combat (HP dropping) is distinguishable from being
            // stuck on an unreachable mob (HP frozen while pos/nav also frozen).
            sb.append(" attackTarget=").append(attackTarget.getName().getString())
                    .append("(hp=").append(String.format(Locale.ROOT, "%.1f", attackTarget.getHealth()))
                    .append("/").append(String.format(Locale.ROOT, "%.1f", attackTarget.getMaxHealth()))
                    .append(")");
        }

        sb.append(" nav=").append(blockling.getNavigation().isDone() ? "idle" : "moving");

        // Which physical work action is currently running (combat swing / block gathering).
        if (blockling.getActions().attack.isRunning(com.willr27.blocklings.entity.blockling.BlocklingHand.MAIN)
                || blockling.getActions().attack.isRunning(com.willr27.blocklings.entity.blockling.BlocklingHand.OFF))
        {
            sb.append(" act=attacking");
        }
        else if (blockling.getActions().gather.isRunning())
        {
            sb.append(" act=gathering");
        }
        else
        {
            sb.append(" act=-");
        }

        sb.append(" | ");
        boolean anyActive = false;
        for (Task task : blockling.getTasks().getPrioritisedTasks())
        {
            if (!task.isConfigured())
            {
                continue;
            }
            BlocklingGoal goal = task.getGoal();
            if (goal == null || goal.getState() != BlocklingGoal.State.ACTIVE)
            {
                continue;
            }
            if (anyActive)
            {
                sb.append(", ");
            }
            sb.append(task.getCustomName()).append("[").append(goal.getDebugStatus()).append("]");
            anyActive = true;
        }
        if (!anyActive)
        {
            sb.append("(no active task)");
        }

        return sb.toString();
    }

    @Nonnull
    private static String itemName(@Nonnull ItemStack stack)
    {
        return stack.isEmpty() ? "-" : stack.getHoverName().getString();
    }

    private static void line(@Nonnull BlocklingEntity blockling, @Nonnull String text)
    {
        raw("[t+" + (blockling.tickCount - startTick) + "] " + text);
        flush();
    }

    private static void raw(@Nonnull String text)
    {
        if (writer == null)
        {
            return;
        }
        try
        {
            writer.write(text);
            writer.newLine();
        }
        catch (IOException ignored)
        {
        }
    }

    private static void flush()
    {
        if (writer == null)
        {
            return;
        }
        try
        {
            writer.flush();
        }
        catch (IOException ignored)
        {
        }
    }

    public static void stop()
    {
        watchedUuid = null;
        lastSnapshot = "";
        currentFile = null;
        if (writer != null)
        {
            try
            {
                writer.flush();
                writer.close();
            }
            catch (IOException ignored)
            {
            }
            writer = null;
        }
    }
}
