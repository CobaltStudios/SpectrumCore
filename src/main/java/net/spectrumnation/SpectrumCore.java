package net.spectrumnation;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sirtidez on 10/30/15.
 */
public class SpectrumCore extends JavaPlugin {
    private static Logger logger;
    private static boolean debug;

    @Override
    public void onEnable() {
        this.logger = Logger.getLogger("Minecraft");
    }

    @Override
    public void onDisable() {

    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void error(String message) {
        logger.log(Level.WARNING, message);
    }

    public static void debug(String message) {
        if(debug) {
            logger.log(Level.INFO, "[DEV] "+message);
        }
    }
}
