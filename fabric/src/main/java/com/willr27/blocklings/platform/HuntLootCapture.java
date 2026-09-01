package com.willr27.blocklings.platform;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

public final class HuntLootCapture {
   private static final ThreadLocal<List<ItemStack>> DROPS = new ThreadLocal<>();
   private static final ThreadLocal<BlocklingEntity> KILLER = new ThreadLocal<>();

   private HuntLootCapture() {
   }

   public static void begin(@Nonnull BlocklingEntity blockling, @Nonnull List<ItemStack> drops) {
      KILLER.set(blockling);
      DROPS.set(drops);
   }

   public static void clear() {
      DROPS.remove();
      KILLER.remove();
   }

   @Nullable
   public static List<ItemStack> drops() {
      return DROPS.get();
   }

   @Nullable
   public static BlocklingEntity killer() {
      return KILLER.get();
   }
}
