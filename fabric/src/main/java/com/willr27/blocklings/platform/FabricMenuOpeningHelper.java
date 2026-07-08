package com.willr27.blocklings.platform;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class FabricMenuOpeningHelper {
    public static final StreamCodec<RegistryFriendlyByteBuf, byte[]> OPEN_DATA_CODEC = new StreamCodec<>() {
        @Override
        public byte[] decode(RegistryFriendlyByteBuf buf) {
            return buf.readByteArray();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, byte[] value) {
            buf.writeByteArray(value);
        }
    };

    @FunctionalInterface
    public interface BufWriter {
        void write(RegistryFriendlyByteBuf buf);
    }

    @FunctionalInterface
    public interface MenuSupplier<T extends AbstractContainerMenu> {
        T create(int windowId, Inventory inv, RegistryFriendlyByteBuf extraData);
    }

    private FabricMenuOpeningHelper() {
    }

    public static <T extends AbstractContainerMenu> MenuType<T> create(MenuSupplier<T> supplier) {
        return new ExtendedScreenHandlerType<>(
                (windowId, inv, data) -> {
                    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                            Unpooled.wrappedBuffer(data.length == 0 ? new byte[0] : data),
                            inv.player.level().registryAccess());
                    return supplier.create(windowId, inv, buf);
                },
                OPEN_DATA_CODEC);
    }

    public static void openMenu(Player player, MenuProvider provider, BufWriter writer) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new Wrapped(provider, writer));
        }
    }

    private record Wrapped(MenuProvider delegate, BufWriter writer) implements ExtendedScreenHandlerFactory<byte[]> {
        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
            return delegate.createMenu(syncId, inventory, player);
        }

        @Override
        public Component getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public byte[] getScreenOpeningData(ServerPlayer player) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            writer.write(buf);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        }
    }
}
