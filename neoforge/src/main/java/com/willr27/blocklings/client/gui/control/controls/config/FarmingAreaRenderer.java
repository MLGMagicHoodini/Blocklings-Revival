package com.willr27.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.goals.gather.BlocklingFarmGoal;
import com.willr27.blocklings.entity.blockling.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a translucent blue "ghost" preview of a blockling's farming area in the world:
 * one filled block on each selected corner, plus the full X/Z rectangle between them so the
 * player can read the length and width at a glance. Rendered for the local player's own
 * blocklings, and updates live while corners are being selected.
 */
@EventBusSubscriber(modid = Blocklings.MODID, value = Dist.CLIENT)
public final class FarmingAreaRenderer
{
    /** Bright blue: the area is active (farmer restricted to it). */
    private static final float[] ACTIVE = { 0.25f, 0.55f, 1.0f };
    /** Muted grey: corners set but the area is disabled (farmer works normally). */
    private static final float[] INACTIVE = { 0.6f, 0.62f, 0.68f };

    private FarmingAreaRenderer()
    {
    }

    /** A box to draw, with its colour and whether it is a single-block corner marker. */
    private record Box(@Nonnull AABB aabb, @Nonnull float[] color, boolean corner)
    {
    }

    @SubscribeEvent
    public static void onRenderLevel(@Nonnull RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        if (!FarmingAreaSelection.showGhostBlocks)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
        {
            return;
        }

        // Collect the boxes to draw first so we can render all fills in one pass and all outlines in
        // another. The shared BufferSource only builds one buffer at a time, so interleaving the
        // filled-box buffer with the lines buffer (or caching a consumer across getBuffer calls)
        // throws "Not building!". Two clean passes avoid that entirely.
        List<Box> boxes = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (!(entity instanceof BlocklingEntity blockling) || !blockling.isOwnedBy(player))
            {
                continue;
            }

            BlocklingFarmGoal goal = findFarmGoal(blockling);
            if (goal == null)
            {
                continue;
            }

            BlockPos c1 = goal.getAreaCorner1();
            BlockPos c2 = goal.getAreaCorner2();
            float[] color = goal.isFarmingAreaActive() ? ACTIVE : INACTIVE;

            if (c1 != null)
            {
                boxes.add(new Box(cornerBox(c1), color, true));
            }
            if (c2 != null)
            {
                boxes.add(new Box(cornerBox(c2), color, true));
            }
            if (c1 != null && c2 != null)
            {
                boxes.add(new Box(areaBox(c1, c2), color, false));
            }
        }

        if (boxes.isEmpty())
        {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // Pass 1: translucent fills (DebugRenderer offsets by the camera internally).
        for (Box box : boxes)
        {
            float alpha = box.corner() ? 0.45f : 0.15f;
            DebugRenderer.renderFilledBox(poseStack, buffers, box.aabb(),
                    box.color()[0], box.color()[1], box.color()[2], alpha);
        }
        buffers.endBatch();

        // Pass 2: solid outlines (LevelRenderer needs camera-relative coordinates).
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Box box : boxes)
        {
            float alpha = box.corner() ? 1.0f : 0.85f;
            LevelRenderer.renderLineBox(poseStack, lines, box.aabb().move(-cam.x, -cam.y, -cam.z),
                    box.color()[0], box.color()[1], box.color()[2], alpha);
        }
        buffers.endBatch(RenderType.lines());
    }

    @Nullable
    private static BlocklingFarmGoal findFarmGoal(@Nonnull BlocklingEntity blockling)
    {
        for (Task task : blockling.getTasks().getPrioritisedTasks())
        {
            if (task.getGoal() instanceof BlocklingFarmGoal farmGoal)
            {
                return farmGoal;
            }
        }
        return null;
    }

    /** A single-block ghost box on a corner. */
    @Nonnull
    private static AABB cornerBox(@Nonnull BlockPos pos)
    {
        return new AABB(pos);
    }

    /** The full X/Z footprint between the two corners (height spans the corners, min 1 block). */
    @Nonnull
    private static AABB areaBox(@Nonnull BlockPos c1, @Nonnull BlockPos c2)
    {
        int minX = Math.min(c1.getX(), c2.getX());
        int minY = Math.min(c1.getY(), c2.getY());
        int minZ = Math.min(c1.getZ(), c2.getZ());
        int maxX = Math.max(c1.getX(), c2.getX());
        int maxY = Math.max(c1.getY(), c2.getY());
        int maxZ = Math.max(c1.getZ(), c2.getZ());

        return new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }
}
