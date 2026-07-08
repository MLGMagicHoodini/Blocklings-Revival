package com.willr27.blocklings.client.gui.texture;

import net.minecraft.resources.ResourceLocation;

/** Minimal texture descriptor for task icons until GUI port completes. */
public record Texture(ResourceLocation atlas, int u, int v, int width, int height) {
    public Texture(ResourceLocation atlas, int u, int v, int size) {
        this(atlas, u, v, size, size);
    }
}
