package com.willr27.blocklings.network.messages;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.Whitelist;
import com.willr27.blocklings.network.BlocklingMessage;
import com.willr27.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.UUID;

public class WhitelistAllMessage extends BlocklingMessage<WhitelistAllMessage>
{
    /**
     * The associated task id.
     */
    private UUID taskId;

    /**
     * The whitelist id.
     */
    private int whitelistId;

    /**
     * The actual underlying whitelist.
     */
    private Whitelist<ResourceLocation> whitelist;

    /**
     * Empty constructor used ONLY for decoding.
     */
    public WhitelistAllMessage()
    {
        super(null);
    }

    /**
     * @param blockling the blockling.
     * @param taskId the associated task id.
     * @param whitelistId the whitelist id.
     * @param whitelist the actual underlying whitelist.
     */
    public WhitelistAllMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, int whitelistId, @Nonnull GoalWhitelist whitelist)
    {
        super(blockling);
        this.taskId = taskId;
        this.whitelistId = whitelistId;
        // Snapshot — do not keep a live TreeMap ref (encode/GUI race + clear bugs).
        this.whitelist = new Whitelist<>();
        this.whitelist.putAll(whitelist);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeUUID(taskId);
        buf.writeInt(whitelistId);
        buf.writeInt(whitelist.size());

        for (ResourceLocation entry : whitelist.keySet())
        {
            FriendlyByteBufUtils.writeString(buf, entry.toString());
            buf.writeBoolean(whitelist.get(entry));
        }
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        taskId = buf.readUUID();
        whitelistId = buf.readInt();
        whitelist = new Whitelist<>();

        int size = buf.readInt();

        for (int i = 0; i < size; i++)
        {
            whitelist.put(ResourceLocation.parse(FriendlyByteBufUtils.readString(buf)), buf.readBoolean());
        }
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        var task = blockling.getTasks().getTaskOrLog(taskId, "WhitelistAllMessage");
        if (task != null && task.getGoal() != null
                && whitelistId >= 0 && whitelistId < task.getGoal().whitelists.size())
        {
            task.getGoal().whitelists.get(whitelistId).setWhitelist(whitelist, false);
        }
    }
}