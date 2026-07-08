package com.willr27.blocklings.network;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.client.gui.BlocklingGuiHandler;
import com.willr27.blocklings.entity.blockling.action.Action;
import com.willr27.blocklings.entity.blockling.attribute.Attribute;
import com.willr27.blocklings.entity.blockling.attribute.attributes.EnumAttribute;
import com.willr27.blocklings.entity.blockling.attribute.attributes.numbers.FloatAttribute;
import com.willr27.blocklings.entity.blockling.attribute.attributes.numbers.IntAttribute;
import com.willr27.blocklings.entity.blockling.attribute.attributes.numbers.ModifiableFloatAttribute;
import com.willr27.blocklings.entity.blockling.attribute.attributes.numbers.ModifiableIntAttribute;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.OrderedItemInfoSet;
import com.willr27.blocklings.entity.blockling.goal.goals.container.BlocklingContainerGoal;
import com.willr27.blocklings.entity.blockling.task.config.Property;
import com.willr27.blocklings.network.messages.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class MessageRegistry {
    private static final Map<Integer, MessageCodec> BY_ID = new HashMap<>();
    private static final Map<Class<? extends Message>, Integer> BY_CLASS = new HashMap<>();
    private static int nextId = 0;
    private static boolean initialized = false;

    private record MessageCodec(int id, Function<FriendlyByteBuf, Message> decoder, BiConsumer<Message, FriendlyByteBuf> encoder) {
    }

    private MessageRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerCommand(SetLevelCommandMessage.class, SetLevelCommandMessage::decode, (message, buf) -> message.encode(buf));
        registerCommand(SetTypeCommandMessage.class, SetTypeCommandMessage::decode, (message, buf) -> message.encode(buf));
        registerCommand(SetXpCommandMessage.class, SetXpCommandMessage::decode, (message, buf) -> message.encode(buf));
        registerCommand(BlocklingSpawnSyncMessage.class, BlocklingSpawnSyncMessage::decode, (message, buf) -> message.encode(buf));

        registerBlockling(Attribute.IsEnabledMessage.class);
        registerBlockling(EnumAttribute.Message.class);
        registerBlockling(FloatAttribute.ValueMessage.class);
        registerBlockling(ModifiableFloatAttribute.BaseValueMessage.class);
        registerBlockling(IntAttribute.ValueMessage.class);
        registerBlockling(ModifiableIntAttribute.BaseValueMessage.class);

        registerBlockling(Action.CountMessage.class);
        registerBlockling(BlocklingAttackTargetMessage.class);
        registerBlockling(BlocklingGuiHandler.OpenMessage.class);
        registerBlockling(BlocklingNameMessage.class);
        registerBlockling(BlocklingScaleMessage.class);
        registerBlockling(BlocklingTypeMessage.class);
        registerBlockling(BlocklingActiveAbilityMessage.class);
        registerBlockling(EquipmentInventoryMessage.class);
        registerBlockling(GoalStateMessage.class);
        registerBlockling(SkillStateMessage.class);
        registerBlockling(SkillTryBuyMessage.class);

        registerBlockling(TaskCreateMessage.class);
        registerBlockling(TaskPriorityMessage.class);
        registerBlockling(TaskRemoveMessage.class);
        registerBlockling(TaskCustomNameMessage.class);
        registerBlockling(Property.TaskPropertyMessage.class);
        registerBlockling(TaskSwapPriorityMessage.class);
        registerBlockling(TaskTypeMessage.class);
        registerBlockling(TaskTypeIsUnlockedMessage.class);

        registerBlockling(OrderedItemInfoSet.AddItemInfoInfoMessage.class);
        registerBlockling(OrderedItemInfoSet.RemoveItemInfoInfoMessage.class);
        registerBlockling(OrderedItemInfoSet.MoveItemInfoInfoMessage.class);
        registerBlockling(OrderedItemInfoSet.SetItemInfoInfoMessage.class);

        registerBlockling(WhitelistAllMessage.class);
        registerBlockling(WhitelistIsUnlockedMessage.class);
        registerBlockling(WhitelistSingleMessage.class);

        registerBlockling(BlocklingContainerGoal.ContainerGoalContainerAddRemoveMessage.class);
        registerBlockling(BlocklingContainerGoal.ContainerGoalContainerMessage.class);
        registerBlockling(BlocklingContainerGoal.ContainerGoalContainerMoveMessage.class);

        Blocklings.LOGGER.info("Blocklings message registry initialized ({} packets)", BY_ID.size());
    }

    private static <T extends BlocklingMessage<T>> void registerBlockling(@Nonnull Class<T> type) {
        register(type, buf -> {
            try {
                T message = type.getDeclaredConstructor().newInstance();
                message.decode(buf);
                return message;
            } catch (ReflectiveOperationException exception) {
                Blocklings.LOGGER.error("Failed to decode blockling message {}", type.getName(), exception);
                return null;
            }
        }, (message, buf) -> type.cast(message).encode(buf));
    }

    private static <T extends Message> void registerCommand(
            @Nonnull Class<T> type,
            @Nonnull Function<FriendlyByteBuf, T> decoder,
            @Nonnull BiConsumer<T, FriendlyByteBuf> encoder
    ) {
        register(type, decoder::apply, (message, buf) -> encoder.accept(type.cast(message), buf));
    }

    private static void register(
            @Nonnull Class<? extends Message> type,
            @Nonnull Function<FriendlyByteBuf, Message> decoder,
            @Nonnull BiConsumer<Message, FriendlyByteBuf> encoder
    ) {
        int id = nextId++;
        MessageCodec codec = new MessageCodec(id, decoder, encoder);
        BY_ID.put(id, codec);
        BY_CLASS.put(type, id);
    }

    @Nonnull
    public static BlocklingNetworkPayload encode(@Nonnull Message message) {
        Integer id = BY_CLASS.get(message.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Unregistered message type: " + message.getClass().getName());
        }

        MessageCodec codec = BY_ID.get(id);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(id);
        codec.encoder().accept(message, buffer);

        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        buffer.release();
        return new BlocklingNetworkPayload(data);
    }

    @Nullable
    public static Message decode(@Nonnull BlocklingNetworkPayload payload) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        if (!buffer.isReadable()) {
            buffer.release();
            return null;
        }

        int id = buffer.readVarInt();
        MessageCodec codec = BY_ID.get(id);
        if (codec == null) {
            Blocklings.LOGGER.warn("Unknown blocklings message id: {}", id);
            buffer.release();
            return null;
        }

        Message message = codec.decoder().apply(buffer);
        buffer.release();
        return message;
    }
}
