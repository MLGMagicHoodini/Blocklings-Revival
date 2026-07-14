package com.willr27.blocklings.client.gui.screen;

import com.willr27.blocklings.client.gui.control.controls.ScreenControl;
import com.willr27.blocklings.client.gui.control.event.events.input.CharTypedEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.KeyPressedEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.KeyReleasedEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.MouseClickedEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.MouseScrolledEvent;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BlocklingsContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T>
{
    @Nonnull
    public final BlocklingEntity blockling;

    @Nonnull
    private final Player player;

    @Nonnull
    public final ScreenControl screenControl = new ScreenControl();

    protected BlocklingsContainerScreen(@Nonnull BlocklingEntity blockling, @Nonnull T container)
    {
        super(container, Minecraft.getInstance().player.getInventory(), Component.literal(""));
        this.blockling = blockling;
        this.player = Minecraft.getInstance().player;
    }

    @Override
    protected void init()
    {
        super.init();

        screenControl.setWidth(width);
        screenControl.setHeight(height);
        screenControl.markMeasureDirty(true);
        screenControl.markArrangeDirty(true);
    }

    @Override
    public void onClose()
    {
        super.onClose();
        screenControl.forwardClose(screenControl.shouldReallyClose());
    }

    @Override
    protected void containerTick()
    {
        screenControl.forwardTick();
        super.containerTick();
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY)
    {
    }

    /**
     * Skip vanilla's transparent overlay here. Tabbed screens already dim the world via
     * their own background control; drawing the overlay in {@link #render} after
     * {@code screenControl} would tint the equipment panel dark.
     */
    @Override
    public void renderBackground(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        screenControl.render(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int screenMouseX, int screenMouseY)
    {
    }

    @Override
    public boolean mouseClicked(double screenMouseX, double screenMouseY, int button)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseClickedEvent e = new MouseClickedEvent(mouseX, mouseY, button);
        screenControl.forwardMouseClicked(e);

        if (e.isHandled() || super.mouseClicked(screenMouseX, screenMouseY, button))
        {
            return true;
        }

        screenControl.setPressed(true);
        screenControl.setFocused(true);
        return false;
    }

    @Override
    public boolean mouseReleased(double screenMouseX, double screenMouseY, int button)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseReleasedEvent e = new MouseReleasedEvent(mouseX, mouseY, button);
        screenControl.forwardMouseReleased(e);

        return e.isHandled() || super.mouseReleased(screenMouseX, screenMouseY, button);
    }

    @Override
    public boolean mouseScrolled(double screenMouseX, double screenMouseY, double deltaX, double deltaY)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseScrolledEvent e = new MouseScrolledEvent(mouseX, mouseY, deltaY);
        screenControl.forwardMouseScrolled(e);

        return e.isHandled() || super.mouseScrolled(screenMouseX, screenMouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        KeyPressedEvent e = new KeyPressedEvent(keyCode, scanCode, modifiers);
        screenControl.forwardGlobalKeyPressed(e);
        return e.isHandled() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        KeyReleasedEvent e = new KeyReleasedEvent(keyCode, scanCode, modifiers);
        screenControl.forwardGlobalKeyReleased(e);
        return e.isHandled() || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers)
    {
        CharTypedEvent e = new CharTypedEvent(character, modifiers);
        screenControl.forwardGlobalCharTyped(e);
        return e.isHandled() || super.charTyped(character, modifiers);
    }
}
