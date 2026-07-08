package com.willr27.blocklings.client.gui.control;

import com.willr27.blocklings.util.event.EventBus;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

/**
 * An event bus for controls that will forward events to the appropriate subscribers.
 */
@OnlyIn(Dist.CLIENT)
public class ControlEventBus extends EventBus<BaseControl>
{

}

