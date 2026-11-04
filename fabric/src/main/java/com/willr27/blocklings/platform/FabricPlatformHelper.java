package com.willr27.blocklings.platform;

import com.mojang.authlib.GameProfile;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.hybrid.BukkitDetector;
import com.willr27.blocklings.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final UUID BLOCKLING_BREAKER = UUID.fromString("6b0c1f2a-9d34-4e71-8a55-2c8f0d1e4b77");

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }

    @Override
    public boolean isHybridServer() {
        return BukkitDetector.isBukkitPresent();
    }

    @Override
    public String hybridServerName() {
        return BukkitDetector.serverSoftwareName();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean allowAnimalTame(Animal animal, Player player) {
        return true;
    }

    @Override
    public boolean destroyBlockAsPlayer(LivingEntity actor, BlockPos pos, ItemStack tool)
    {
        if (!(actor.level() instanceof ServerLevel server))
        {
            return false;
        }

        GameProfile profile;
        if (actor instanceof BlocklingEntity blockling && blockling.getOwner() instanceof Player owner)
        {
            profile = owner.getGameProfile();
        }
        else
        {
            profile = new GameProfile(BLOCKLING_BREAKER, "Blockling");
        }

        FakePlayer fake = FakePlayer.get(server, profile);
        fake.setPos(actor.getX(), actor.getY(), actor.getZ());
        ItemStack previous = fake.getMainHandItem();
        fake.setItemInHand(InteractionHand.MAIN_HAND, tool.isEmpty() ? ItemStack.EMPTY : tool.copy());
        try
        {
            return fake.gameMode.destroyBlock(pos);
        }
        finally
        {
            fake.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }

    @Override
    public int getBlockExpDrop(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool)
    {
        if (state.getBlock() instanceof DropExperienceBlock drop)
        {
            return Math.max(0, ((com.willr27.blocklings.mixin.DropExperienceBlockAccessor) drop).blocklings$getXpRange().sample(level.random));
        }

        return 0;
    }
}
