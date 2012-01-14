
package com.exphc.RadioBeacon;

import java.util.logging.Logger;
import java.util.concurrent.ConcurrentSkipListSet;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.Material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.*;

class Antenna implements Comparable {
    Location location;
    boolean enabled;

    Logger log = Logger.getLogger("Minecraft");

    public Antenna(Location loc) {
        location = loc;
        enabled = true;

        log.info("New antenna at " + loc);
    }

    public void enable() {
        log.info("enabled antenna "+location);
        enabled = true;
    }

    public void disable() {
        log.info("disabled antenna "+location);
        enabled = false;
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof Antenna)) {
            return -1;
        }
        Antenna rhs = (Antenna)obj;

        //return location.compareTo(rhs.location);
        // TODO

        return 1;
    }
}
class BlockPlaceListener extends BlockListener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    public BlockPlaceListener(RadioBeacon pl) {
        plugin = pl;
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        log.info("attempt to place " + event);
        log.info("player = " + event.getPlayer());
        log.info("block = " + event.getBlock());

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.IRON_BLOCK) {
            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                plugin.createAntenna(block.getLocation());
            }

            player.sendMessage("powered? " + block.isBlockPowered());
            player.sendMessage("ind powered?" + block.isBlockIndirectlyPowered());
        }

    }
}

public class RadioBeacon extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    BlockListener blockListener;

    ConcurrentSkipListSet<Antenna> ants = new ConcurrentSkipListSet<Antenna>();

    public void onEnable() {

        blockListener = new BlockPlaceListener(this);

        getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.BLOCK_PLACE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);


        log.info("beacon enable");
    }

    public void onDisable() {
        log.info("beacon disable");
    }

    public Antenna createAntenna(Location loc)
    {   
        Antenna ant = new Antenna(loc);

        ants.add(ant);
        
        return ant;
    }
}
