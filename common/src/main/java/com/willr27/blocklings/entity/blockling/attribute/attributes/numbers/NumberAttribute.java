package com.willr27.blocklings.entity.blockling.attribute.attributes.numbers;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.attribute.Attribute;
import com.willr27.blocklings.entity.blockling.attribute.IModifier;
import com.willr27.blocklings.entity.blockling.attribute.Operation;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A simple number attribute.
 */
public abstract class NumberAttribute<T extends Number> extends Attribute<T> {
    @Nullable
    protected Holder<net.minecraft.world.entity.ai.attributes.Attribute> vanillaAttribute;

    public NumberAttribute(@Nonnull String id, @Nonnull String key, @Nonnull BlocklingEntity blockling, T initialValue,
                           @Nullable Function<T, String> displayStringValueFunction,
                           @Nullable Supplier<String> displayStringNameSupplier, boolean isEnabled) {
        super(id, key, blockling, displayStringValueFunction, displayStringNameSupplier, isEnabled);
        this.value = initialValue;
    }

    @Nullable
    public Holder<net.minecraft.world.entity.ai.attributes.Attribute> getVanillaAttribute() {
        return vanillaAttribute;
    }

    public void setVanillaAttribute(@Nullable Holder<net.minecraft.world.entity.ai.attributes.Attribute> vanillaAttribute) {
        removeFromVanillaAttribute();
        this.vanillaAttribute = vanillaAttribute;
        updateVanillaAttribute();
    }

    @Nonnull
    private ResourceLocation modifierId() {
        return ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, id.toString());
    }

    protected void removeFromVanillaAttribute() {
        if (vanillaAttribute != null) {
            AttributeInstance vanillaAttributeInstance = blockling.getAttribute(vanillaAttribute);
            if (this instanceof IModifier) {
                vanillaAttributeInstance.removeModifier(modifierId());
            }
        }
    }

    protected void updateVanillaAttribute() {
        if (vanillaAttribute == null) {
            return;
        }

        AttributeInstance vanillaAttributeInstance = blockling.getAttribute(vanillaAttribute);

        if (this instanceof IModifier) {
            IModifier<T> modifier = (IModifier<T>) this;

            if (modifier.getAttributes().stream().anyMatch(modifiable -> modifiable instanceof IModifier)) {
                Blocklings.LOGGER.warn("Tried to add a modifier to a vanilla attribute that is applied to other modifiers.");
                return;
            }

            vanillaAttributeInstance.removeModifier(modifierId());

            if (isEnabled()) {
                vanillaAttributeInstance.addTransientModifier(new AttributeModifier(
                        modifierId(),
                        getValue().doubleValue(),
                        Operation.vanillaOperation(modifier.getOperation())));
            }
        } else {
            vanillaAttributeInstance.setBaseValue(value.doubleValue());
        }
    }

    @Override
    public void onValueChanged() {
        super.onValueChanged();
        updateVanillaAttribute();
    }
}
