package com.willr27.blocklings.network;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import com.willr27.blocklings.security.BlocklingPacketGuard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BlocklingMessage<T extends BlocklingMessage<T>> extends Message {
    @Nullable
    protected BlocklingEntity blockling;
    /** Network entity id (fallback only — can desync between client/server). */
    protected int blocklingId;
    /** Persistent entity UUID — preferred for server lookups. */
    @Nonnull
    protected UUID blocklingUuid = new UUID(0L, 0L);
    @Nonnull
    private UUID clientPlayerId = new UUID(0L, 0L);
    private boolean syncBackToClients = true;

    protected BlocklingMessage(@Nullable BlocklingEntity blockling) {
        this.blockling = blockling;
        if (blockling != null) {
            blocklingId = blockling.getId();
            blocklingUuid = blockling.getUUID();
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
        buf.writeUUID(blocklingUuid);
        buf.writeUUID(clientPlayerId);
        buf.writeBoolean(syncBackToClients);
    }

    public void decode(@Nonnull FriendlyByteBuf buf) {
        blocklingId = buf.readInt();
        blocklingUuid = buf.readUUID();
        clientPlayerId = buf.readUUID();
        syncBackToClients = buf.readBoolean();
    }

    public void handleOnServer(@Nonnull ServerPlayer player) {
        blockling = BlocklingPacketGuard.allowAndResolve(player, blocklingUuid, blocklingId);
        if (blockling == null) {
            return;
        }
        // Keep net id fresh for any sync-back encoding.
        blocklingId = blockling.getId();
        blocklingUuid = blockling.getUUID();
        handle(player, blockling);
        if (syncBackToClients) {
            // Ignore the original sender (client already applied optimistically).
            sendToAllClients(new ArrayList<>(blockling.level().players().stream()
                    .filter(p -> p.getUUID().equals(clientPlayerId))
                    .map(p -> (Player) p)
                    .toList()));
        }
    }

    public void handleOnClient(@Nonnull Player player) {
        blockling = resolveClientBlockling(player);
        if (blockling == null) {
            return;
        }
        handle(player, blockling);
    }

    @Nullable
    private BlocklingEntity resolveClientBlockling(@Nonnull Player player) {
        if (!isNil(blocklingUuid)) {
            var nearby = player.level().getEntitiesOfClass(
                    BlocklingEntity.class,
                    player.getBoundingBox().inflate(128.0D),
                    entity -> entity.getUUID().equals(blocklingUuid));
            if (!nearby.isEmpty()) {
                return nearby.get(0);
            }
        }
        Entity byId = player.level().getEntity(blocklingId);
        return byId instanceof BlocklingEntity found ? found : null;
    }

    private static boolean isNil(@Nonnull UUID uuid) {
        return uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L;
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

    /**
     * Control packets originating on the client must only be sent by the blockling's owner.
     * Every blockling (wild or owned by someone else) still ticks its actions, cooldowns and
     * timers client-side, which used to fire client&#8594;server syncs the server always rejected
     * ("not owner" log spam). Server-side sends are always allowed.
     *
     * @return true if this message may be sent from the current side.
     */
    public boolean canSendFromCurrentSide() {
        if (blockling == null) {
            return false;
        }
        if (!blockling.level().isClientSide()) {
            return true;
        }
        return isLocalPlayerOwner();
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isLocalPlayerOwner() {
        Player localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        return localPlayer != null && blockling != null && blockling.isOwnedBy(localPlayer);
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
