package com.willr27.blocklings.network;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.security.BlocklingPacketGuard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BlocklingMessage<T extends BlocklingMessage<T>> extends Message {
    @Nullable
    protected BlocklingEntity blockling;
    protected int blocklingId;
    @Nonnull
    private UUID clientPlayerId = new UUID(0L, 0L);
    private boolean syncBackToClients = true;

    protected BlocklingMessage(@Nullable BlocklingEntity blockling) {
        this.blockling = blockling;
        if (blockling != null) {
            blocklingId = blockling.getId();
            if (blockling.level().isClientSide()) {
                clientPlayerId = getClientPlayerId();
            }
        }
    }

    protected BlocklingMessage(@Nullable BlocklingEntity blockling, boolean syncBackToClients) {
        this(blockling);
        this.syncBackToClients = syncBackToClients;
    }

    public void encode(@Nonnull FriendlyByteBuf buf) {
        buf.writeInt(blocklingId);
        buf.writeUUID(clientPlayerId);
        buf.writeBoolean(syncBackToClients);
    }

    public void decode(@Nonnull FriendlyByteBuf buf) {
        blocklingId = buf.readInt();
        clientPlayerId = buf.readUUID();
        syncBackToClients = buf.readBoolean();
    }

    public void handleOnServer(@Nonnull ServerPlayer player) {
        if (!BlocklingPacketGuard.allowPacket(player, blocklingId)) {
            return;
        }
        blockling = (BlocklingEntity) player.serverLevel().getEntity(blocklingId);
        if (blockling == null) {
            return;
        }
        handle(player, blockling);
        if (syncBackToClients) {
            sendToAllClients(new ArrayList<>(blockling.level().players().stream()
                    .filter(p -> !p.getUUID().equals(clientPlayerId))
                    .map(p -> (Player) p)
                    .toList()));
        }
    }

    public void handleOnClient(@Nonnull Player player) {
        blockling = (BlocklingEntity) player.level().getEntity(blocklingId);
        if (blockling == null) {
            return;
        }
        handle(player, blockling);
    }

    @Override
    public void handle(@Nonnull Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            handleOnServer(serverPlayer);
        } else {
            handleOnClient(player);
        }
    }

    protected abstract void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling);

    public void sync() {
        if (blockling != null) {
            NetworkHandler.sync(blockling.level(), this);
        }
    }

    public void sendToServer() {
        NetworkHandler.sendToServer(this);
    }

    public void sendToClient(Player player) {
        NetworkHandler.sendToClient(player, this);
    }

    public void sendToAllClients(List<Player> playersToIgnore) {
        if (blockling != null) {
            NetworkHandler.sendToAllClients(blockling.level(), this, playersToIgnore);
        }
    }
}
