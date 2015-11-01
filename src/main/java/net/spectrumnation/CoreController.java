package net.spectrumnation;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by sirtidez on 10/31/15.
 */
public class CoreController {
    private static boolean isInitialized = false;
    private static Logger logger;
    private static SpectrumCore corePlugin;
    private static HashMap<String, SpectrumPlugin> registeredListeners;

    public void init(SpectrumCore core) {
        corePlugin = core;
        logger = Logger.getLogger("Minecraft");
        registeredListeners = new HashMap<String, SpectrumPlugin>();
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

    public static boolean registerPlugin(String name, SpectrumPlugin pluginClass) {
        if(registeredListeners.containsKey(name) || registeredListeners.containsValue(pluginClass))     return false;
        else {
            registeredListeners.put(name, pluginClass);
            return true;
        }
    }
}
