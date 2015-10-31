package net.spectrumnation;

import java.util.logging.Logger;

/**
 * Created by sirtidez on 10/31/15.
 */
public class CoreController {
    private static boolean isInitialized = false;
    private static Logger logger;
    private static SpectrumCore corePlugin;

    public void init(SpectrumCore core) {
        corePlugin = core;
        logger = Logger.getLogger("Minecraft");
        isInitialized = true;
    }

    public void shutdown() {
        corePlugin = null;
        logger = null;
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static SpectrumCore corePlugin() {
        return corePlugin;
    }
}
