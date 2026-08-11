package com.willr27.blocklings.platform;

import com.willr27.blocklings.inventory.BlocklingItemHandler;
import com.willr27.blocklings.inventory.ContainerItemHandlerAdapter;
import com.willr27.blocklings.platform.services.IInventoryHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
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

/**
 * Resolves inventories through the Fabric Transfer API, so any mod exposing an item {@link Storage}
 * works without Blocklings depending on it. Falls back to the vanilla {@link Container} interface.
 */
public final class FabricInventoryHelper implements IInventoryHelper {
    @Override
    @Nullable
    public BlocklingItemHandler getItemHandler(
            @Nonnull Level level,
            @Nonnull BlockPos pos,
            @Nullable BlockEntity blockEntity,
            @Nonnull Direction direction) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                level, pos, level.getBlockState(pos), blockEntity, direction);
        if (storage == null) {
            storage = ItemStorage.SIDED.find(
                    level, pos, level.getBlockState(pos), blockEntity, null);
        }
        if (storage != null) {
            // A slotted storage maps one-to-one onto the slot based handler the goals expect. Anything
            // else (an AE2 network, a virtual storage) only exposes resource views, so it gets the
            // whole storage adapter where a slot is really "the view at that index".
            return storage instanceof SlottedStorage<ItemVariant> slotted
                    ? new SlottedStorageAdapter(slotted)
                    : new StorageItemHandlerAdapter(storage);
        }
        if (blockEntity instanceof Container container) {
            return new ContainerItemHandlerAdapter(container, direction);
        }
        return null;
    }

    /**
     * Adapts a storage whose slots are addressable, so inserting into slot {@code i} really only
     * touches slot {@code i} and the goals' per slot capacity accounting stays honest.
     */
    private record SlottedStorageAdapter(SlottedStorage<ItemVariant> storage) implements BlocklingItemHandler {
        @Override
        public int getSlots() {
            return storage.getSlotCount();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= storage.getSlotCount()) {
                return ItemStack.EMPTY;
            }
            return toStack(storage.getSlot(slot));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= storage.getSlotCount()) {
                return stack;
            }
            long inserted = insert(storage.getSlot(slot), ItemVariant.of(stack), stack.getCount(), simulate);
            return shrunk(stack, inserted);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot < 0 || slot >= storage.getSlotCount()) {
                return ItemStack.EMPTY;
            }
            SingleSlotStorage<ItemVariant> view = storage.getSlot(slot);
            ItemVariant variant = view.getResource();
            if (variant.isBlank() || view.getAmount() <= 0) {
                return ItemStack.EMPTY;
            }
            long extracted = extract(view, variant, amount, simulate);
            return extracted <= 0 ? ItemStack.EMPTY : variant.toStack((int) extracted);
        }
    }

    /**
     * Adapts a storage that has no addressable slots. Each index is a resource view, and every
     * insert or extract goes through the storage as a whole.
     */
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
            return toStack(views.get(slot));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= views.size()) {
                return stack;
            }
            long inserted = insert(storage, ItemVariant.of(stack), stack.getCount(), simulate);
            return shrunk(stack, inserted);
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
            return extracted <= 0 ? ItemStack.EMPTY : variant.toStack((int) extracted);
        }
    }

    @Nonnull
    private static ItemStack toStack(@Nonnull StorageView<ItemVariant> view) {
        ItemVariant variant = view.getResource();
        if (variant.isBlank() || view.getAmount() <= 0) {
            return ItemStack.EMPTY;
        }
        return variant.toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
    }

    @Nonnull
    private static ItemStack shrunk(@Nonnull ItemStack stack, long inserted) {
        if (inserted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        remainder.shrink((int) inserted);
        return remainder;
    }

    private static long insert(
            Storage<ItemVariant> storage,
            ItemVariant variant,
            long amount,
            boolean simulate) {
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    private static long extract(
            Storage<ItemVariant> storage,
            ItemVariant variant,
            long amount,
            boolean simulate) {
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = storage.extract(variant, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }
}
