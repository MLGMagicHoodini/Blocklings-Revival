package com.willr27.blocklings.platform;

import com.mojang.authlib.GameProfile;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.hybrid.BukkitDetector;
import com.willr27.blocklings.platform.services.IPlatformHelper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;

public class NeoForgePlatformHelper implements IPlatformHelper {
   private static final UUID BLOCKLING_BREAKER = UUID.fromString("6b0c1f2a-9d34-4e71-8a55-2c8f0d1e4b77");

   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   public boolean isDedicatedServer() {
      return FMLEnvironment.dist.isDedicatedServer();
   }

   public boolean isHybridServer() {
      return BukkitDetector.isBukkitPresent();
   }

   public String hybridServerName() {
      return BukkitDetector.serverSoftwareName();
   }

   public boolean isDevelopmentEnvironment() {
      return !FMLEnvironment.production;
   }

   public boolean allowAnimalTame(Animal animal, Player player) {
      return !EventHooks.onAnimalTame(animal, player);
   }

   public boolean destroyBlockAsPlayer(LivingEntity actor, BlockPos pos, ItemStack tool) {
      if (!(actor.level() instanceof ServerLevel server)) {
         return false;
      } else {
         GameProfile profile;
         if (actor instanceof BlocklingEntity blockling && blockling.getOwner() instanceof Player owner) {
            profile = owner.getGameProfile();
         } else {
            profile = new GameProfile(BLOCKLING_BREAKER, "Blockling");
         }

         FakePlayer fake = FakePlayerFactory.get(server, profile);
         fake.setPos(actor.getX(), actor.getY(), actor.getZ());
         ItemStack previous = fake.getMainHandItem();
         fake.setItemInHand(InteractionHand.MAIN_HAND, tool.isEmpty() ? ItemStack.EMPTY : tool.copy());

         try {
            return fake.gameMode.destroyBlock(pos);
         } finally {
            fake.setItemInHand(InteractionHand.MAIN_HAND, previous);
         }
      }
   }

   public int getBlockExpDrop(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
      return state.getExpDrop(level, pos, level.getBlockEntity(pos), null, tool);
   }
}
