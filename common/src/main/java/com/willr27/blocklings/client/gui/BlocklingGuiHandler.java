package com.willr27.blocklings.client.gui;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.client.gui.containers.EquipmentContainer;
import com.willr27.blocklings.platform.Services;
import com.willr27.blocklings.client.gui.control.controls.TabbedUIControl;
import com.willr27.blocklings.client.gui.screen.BlocklingsScreen;
import com.willr27.blocklings.client.gui.screen.screens.EquipmentScreen;
import com.willr27.blocklings.client.gui.screen.screens.SkillsScreen;
import com.willr27.blocklings.client.gui.screen.screens.StatsScreen;
import com.willr27.blocklings.client.gui.screen.screens.TasksScreen;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.skill.BlocklingSkills;
import com.willr27.blocklings.network.BlocklingMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class BlocklingGuiHandler
{
    public static final int STATS_ID = 0;
    public static final int TASKS_ID = 1;
    public static final int EQUIPMENT_ID = 2;
    public static final int GENERAL_ID = 4;
    public static final int COMBAT_ID = 5;
    public static final int MINING_ID = 6;
    public static final int WOODCUTTING_ID = 7;
    public static final int FARMING_ID = 8;

    @Nonnull
    public final BlocklingEntity blockling;

    private int recentGuiId = STATS_ID;

    public BlocklingGuiHandler(@Nonnull BlocklingEntity blockling)
    {
        this.blockling = blockling;
    }

    public static void openScreen(@Nonnull Screen screen)
    {
        Minecraft.getInstance().setScreen(screen);

        if (screen instanceof BlocklingsScreen blocklingsScreen && blocklingsScreen.blockling.isDeadOrDying())
        {
            blocklingsScreen.onClose();
        }
    }

    public void openGui(@Nonnull Player player)
    {
        openGui(recentGuiId, -1, player);
    }

    public void openGui(int guiId, @Nonnull Player player)
    {
        openGui(guiId, -1, player);
    }

    private void openGui(int guiId, int windowId, @Nonnull Player player)
    {
        if (!blockling.level().isClientSide())
        {
            if (!(player instanceof ServerPlayer serverPlayer))
            {
                return;
            }

            recentGuiId = guiId;

            if (guiId == EQUIPMENT_ID)
            {
                Services.MENUS.openEquipmentMenu(serverPlayer, blockling);
                return;
            }

            new OpenMessage(blockling, guiId, 0, player.getUUID()).sendToClient(player);
            return;
        }

        recentGuiId = guiId;

        if (windowId == -1)
        {
            if (guiId != EQUIPMENT_ID && Minecraft.getInstance().screen instanceof EquipmentScreen)
            {
                openScreen(guiId, player, null);
            }

            new OpenMessage(blockling, guiId, windowId, player.getUUID()).sendToServer();
            return;
        }

        if (guiId == EQUIPMENT_ID)
        {
            return;
        }

        openScreen(guiId, player, null);
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen(int guiId, @Nonnull Player player, @Nullable AbstractContainerMenu container)
    {
        Screen screen = createScreen(guiId, container, player);

        if (screen != null)
        {
            Minecraft.getInstance().setScreen(screen);
        }
        else
        {
            Blocklings.LOGGER.warn("No screen exists for gui id: {}", guiId);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private Screen createScreen(int guiId, @Nullable AbstractContainerMenu container, @Nonnull Player player)
    {
        return switch (guiId)
        {
            case STATS_ID -> new StatsScreen(blockling);
            case TASKS_ID -> new TasksScreen(blockling);
            case EQUIPMENT_ID -> null;
            case GENERAL_ID -> new SkillsScreen(blockling, BlocklingSkills.Groups.GENERAL, TabbedUIControl.Tab.GENERAL);
            case COMBAT_ID -> new SkillsScreen(blockling, BlocklingSkills.Groups.COMBAT, TabbedUIControl.Tab.COMBAT);
            case MINING_ID -> new SkillsScreen(blockling, BlocklingSkills.Groups.MINING, TabbedUIControl.Tab.MINING);
            case WOODCUTTING_ID -> new SkillsScreen(blockling, BlocklingSkills.Groups.WOODCUTTING, TabbedUIControl.Tab.WOODCUTTING);
            case FARMING_ID -> new SkillsScreen(blockling, BlocklingSkills.Groups.FARMING, TabbedUIControl.Tab.FARMING);
            default -> null;
        };
    }

    public int getRecentGuiId()
    {
        return recentGuiId;
    }

    public void setRecentGuiId(int guiId)
    {
        recentGuiId = guiId;
    }

    public static class OpenMessage extends BlocklingMessage<OpenMessage>
    {
        private int guiId;
        private int windowId;
        private UUID playerId;

        public OpenMessage()
        {
            super(null);
        }

        public OpenMessage(@Nonnull BlocklingEntity blockling, int guiId, int windowId, @Nonnull UUID playerId)
        {
            super(blockling, false);
            this.guiId = guiId;
            this.windowId = windowId;
            this.playerId = playerId;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf)
        {
            super.encode(buf);
            buf.writeInt(guiId);
            buf.writeInt(windowId);
            buf.writeUUID(playerId);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf)
        {
            super.decode(buf);
            guiId = buf.readInt();
            windowId = buf.readInt();
            playerId = buf.readUUID();
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
        {
            if (blockling.level().isClientSide())
            {
                if (guiId == EQUIPMENT_ID)
                {
                    return;
                }
                blockling.guiHandler.openGui(guiId, windowId, player);
            }
            else
            {
                Player targetedPlayer = blockling.level().players().stream()
                        .filter(serverPlayer -> serverPlayer.getUUID().equals(playerId))
                        .findFirst()
                        .orElse(null);

                if (targetedPlayer != null)
                {
                    if (guiId != EQUIPMENT_ID && targetedPlayer.containerMenu instanceof EquipmentContainer)
                    {
                        targetedPlayer.getInventory().setChanged();
                        if (targetedPlayer instanceof ServerPlayer serverPlayer) {
                            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(targetedPlayer.containerMenu.containerId));
                        }
                        targetedPlayer.containerMenu = targetedPlayer.inventoryMenu;
                    }

                    blockling.guiHandler.openGui(guiId, windowId, targetedPlayer);
                }
                else
                {
                    Blocklings.LOGGER.warn("Tried opening a gui for a player that does not exist on the server with id: {}", playerId);
                }
            }
        }
    }
}
