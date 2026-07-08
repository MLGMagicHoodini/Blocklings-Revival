package com.willr27.blocklings.client.gui.texture;

import net.minecraft.resources.ResourceLocation;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * A {@link ResourceLocation} based gui texture.
 */
@OnlyIn(Dist.CLIENT)
public class Texture
{
    @Nonnull
    public final ResourceLocation resourceLocation;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public Texture(@Nonnull ResourceLocation resourceLocation, int x, int y, int width, int height)
    {
        this.resourceLocation = resourceLocation;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Nonnull
    public Texture x(int x)
    {
        return new Texture(resourceLocation, x, y, width, height);
    }

    @Nonnull
    public Texture y(int y)
    {
        return new Texture(resourceLocation, x, y, width, height);
    }

    @Nonnull
    public Texture width(int width)
    {
        return new Texture(resourceLocation, x, y, width, height);
    }

    @Nonnull
    public Texture height(int height)
    {
        return new Texture(resourceLocation, x, y, width, height);
    }

    @Nonnull
    public Texture dx(int dx)
    {
        return new Texture(resourceLocation, x + dx, y, width, height);
    }

    @Nonnull
    public Texture dy(int dy)
    {
        return new Texture(resourceLocation, x, y + dy, width, height);
    }

    @Nonnull
    public Texture dWidth(int dWidth)
    {
        return new Texture(resourceLocation, x, y, width + dWidth, height);
    }

    @Nonnull
    public Texture dHeight(int dHeight)
    {
        return new Texture(resourceLocation, x, y, width, height + dHeight);
    }
}
