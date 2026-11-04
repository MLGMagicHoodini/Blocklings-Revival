package com.willr27.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.goals.gather.BlocklingFarmGoal;
import com.willr27.blocklings.entity.blockling.task.Task;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FarmingAreaRendererFabric {
   private static final float[] ACTIVE = new float[]{0.25F, 0.55F, 1.0F};
   private static final float[] INACTIVE = new float[]{0.6F, 0.62F, 0.68F};

   private FarmingAreaRendererFabric() {
   }

   public static void register() {
      WorldRenderEvents.AFTER_TRANSLUCENT.register(FarmingAreaRendererFabric::onRenderLevel);
   }

   private static void onRenderLevel(@Nonnull WorldRenderContext context) {
      if (FarmingAreaSelection.showGhostBlocks) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            PoseStack poseStack = context.matrixStack();
            if (poseStack != null && context.camera() != null) {
               List<FarmingAreaRendererFabric.Box> boxes = new ArrayList<>();

               for (Entity entity : mc.level.entitiesForRendering()) {
                  if (entity instanceof BlocklingEntity blockling && blockling.isOwnedBy(player)) {
                     BlocklingFarmGoal goal = findFarmGoal(blockling);
                     if (goal != null) {
                        BlockPos c1 = goal.getAreaCorner1();
                        BlockPos c2 = goal.getAreaCorner2();
                        float[] color = goal.isFarmingAreaActive() ? ACTIVE : INACTIVE;
                        if (c1 != null) {
                           boxes.add(new FarmingAreaRendererFabric.Box(cornerBox(c1), color, true));
                        }

                        if (c2 != null) {
                           boxes.add(new FarmingAreaRendererFabric.Box(cornerBox(c2), color, true));
                        }

                        if (c1 != null && c2 != null) {
                           boxes.add(new FarmingAreaRendererFabric.Box(areaBox(c1, c2), color, false));
                        }
                     }
                  }
               }

               if (!boxes.isEmpty()) {
                  Vec3 cam = context.camera().getPosition();
                  BufferSource buffers = mc.renderBuffers().bufferSource();

                  for (FarmingAreaRendererFabric.Box box : boxes) {
                     float alpha = box.corner() ? 0.45F : 0.15F;
                     DebugRenderer.renderFilledBox(poseStack, buffers, box.aabb(), box.color()[0], box.color()[1], box.color()[2], alpha);
                  }

                  buffers.endBatch();
                  VertexConsumer lines = buffers.getBuffer(RenderType.lines());

                  for (FarmingAreaRendererFabric.Box box : boxes) {
                     float alpha = box.corner() ? 1.0F : 0.85F;
                     LevelRenderer.renderLineBox(
                        poseStack, lines, box.aabb().move(-cam.x, -cam.y, -cam.z), box.color()[0], box.color()[1], box.color()[2], alpha
                     );
                  }

                  buffers.endBatch(RenderType.lines());
               }
            }
         }
      }
   }

   @Nullable
   private static BlocklingFarmGoal findFarmGoal(@Nonnull BlocklingEntity blockling) {
      for (Task task : blockling.getTasks().getPrioritisedTasks()) {
         if (task.getGoal() instanceof BlocklingFarmGoal farmGoal) {
            return farmGoal;
         }
      }

      return null;
   }

   @Nonnull
   private static AABB cornerBox(@Nonnull BlockPos pos) {
      return new AABB(pos);
   }

   @Nonnull
   private static AABB areaBox(@Nonnull BlockPos c1, @Nonnull BlockPos c2) {
      int minX = Math.min(c1.getX(), c2.getX());
      int minY = Math.min(c1.getY(), c2.getY());
      int minZ = Math.min(c1.getZ(), c2.getZ());
      int maxX = Math.max(c1.getX(), c2.getX());
      int maxY = Math.max(c1.getY(), c2.getY());
      int maxZ = Math.max(c1.getZ(), c2.getZ());
      return new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
   }

   private record Box(@Nonnull AABB aabb, @Nonnull float[] color, boolean corner) {
   }
}
