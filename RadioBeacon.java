
package com.exphc.RadioBeacon;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class RadioBeacon extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");

    public void onEnable() {
        log.info("beacon enable");
    }

    public void onDisable() {
        log.info("beacon disable");
    }
}
