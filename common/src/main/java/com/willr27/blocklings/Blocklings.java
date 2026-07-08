package com.willr27.blocklings;

import com.willr27.blocklings.util.ObjectUtil;
import com.willr27.blocklings.util.Version;
import org.slf4j.Logger;

/**
 * Shared mod identifiers used by common and loader modules.
 */
public final class Blocklings {
    public static final String MODID = BlocklingsConstants.MODID;
    public static final Logger LOGGER = BlocklingsConstants.LOG;
    public static final Version VERSION = new Version(
            ObjectUtil.coalesce(Blocklings.class.getPackage().getImplementationVersion(), "1.0.0.0"));

    private Blocklings() {
    }
}
