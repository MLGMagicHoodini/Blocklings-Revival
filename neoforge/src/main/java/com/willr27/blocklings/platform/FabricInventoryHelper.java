package com.willr27.blocklings.platform;

import com.willr27.blocklings.inventory.BlocklingItemHandler;
import com.willr27.blocklings.inventory.ContainerItemHandlerAdapter;
import com.willr27.blocklings.platform.services.IInventoryHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class FabricInventoryHelper implements IInventoryHelper {
    @Override
    @Nullable
    public BlocklingItemHandler getItemHandler(
            @Nonnull Level level,
            @Nonnull BlockPos pos,
            @Nonnull BlockEntity blockEntity,
            @Nonnull Direction direction) {
        Storage<ItemVariant> storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                level, pos, blockEntity.getBlockState(), blockEntity, direction);
        if (storage == null) {
            storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                    level, pos, blockEntity.getBlockState(), blockEntity, null);
        }
        if (storage != null) {
            return new StorageItemHandlerAdapter(storage);
        }
        if (blockEntity instanceof Container container) {
            return new ContainerItemHandlerAdapter(container, direction);
        }
        return null;
    }

    private static final class StorageItemHandlerAdapter implements BlocklingItemHandler {
        private final Storage<ItemVariant> storage;
        private final List<StorageView<ItemVariant>> views;

        private StorageItemHandlerAdapter(Storage<ItemVariant> storage) {
            this.storage = storage;
            this.views = collectViews(storage);
        }

        private static List<StorageView<ItemVariant>> collectViews(Storage<ItemVariant> storage) {
            List<StorageView<ItemVariant>> list = new ArrayList<>();
            for (StorageView<ItemVariant> view : storage) {
                list.add(view);
            }
            return List.copyOf(list);
        }

        @Override
        public int getSlots() {
            return views.size();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= views.size()) {
                return ItemStack.EMPTY;
            }
            StorageView<ItemVariant> view = views.get(slot);
            return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= views.size()) {
                return stack;
            }
            ItemVariant variant = ItemVariant.of(stack);
            long inserted = insert(storage, variant, stack.getCount(), simulate);
            if (inserted >= stack.getCount()) {
                return ItemStack.EMPTY;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink((int) inserted);
            return remainder;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= views.size()) {
                return ItemStack.EMPTY;
            }
            StorageView<ItemVariant> view = views.get(slot);
            ItemVariant variant = view.getResource();
            if (variant.isBlank()) {
                return ItemStack.EMPTY;
            }
            long extracted = extract(storage, variant, amount, simulate);
            return variant.toStack((int) extracted);
        }

        private static long insert(
                Storage<ItemVariant> storage,
                ItemVariant variant,
                long amount,
                boolean simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                if (simulate) {
                    try (Transaction nested = tx.openNested()) {
                        return storage.insert(variant, amount, nested);
                    }
                }
                long inserted = storage.insert(variant, amount, tx);
                tx.commit();
                return inserted;
            }
        }

        private static long extract(
                Storage<ItemVariant> storage,
                ItemVariant variant,
                long amount,
                boolean simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                if (simulate) {
                    try (Transaction nested = tx.openNested()) {
                        return storage.extract(variant, amount, nested);
                    }
                }
                long extracted = storage.extract(variant, amount, tx);
                tx.commit();
                return extracted;
            }
        }
    }
}
