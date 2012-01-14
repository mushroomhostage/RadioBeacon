
package com.exphc.RadioBeacon;

import java.util.Map;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.Material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.*;

class Antenna implements Comparable {
    Location location;
    boolean enabled;

    static Logger log = Logger.getLogger("Minecraft");

    static ConcurrentHashMap<Location, Antenna> ants = new ConcurrentHashMap<Location, Antenna>();

    public Antenna(Location loc) {
        location = loc;
        enabled = true;

        ants.put(loc, this);
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
        return location.hashCode() - rhs.location.hashCode();
    }

    public String toString() {
        return location.toString();
    }
}

class BlockPlaceListener extends BlockListener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    public BlockPlaceListener(RadioBeacon pl) {
        plugin = pl;
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.IRON_BLOCK) {
            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                new Antenna(block.getLocation());
            }

            player.sendMessage("powered? " + block.isBlockPowered());
            player.sendMessage("ind powered?" + block.isBlockIndirectlyPowered());
        }

    }

    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        /* TODO: find out how to disable antennas, get when block becomes unpowered
        World world = event.getBlock().getWorld();

        if (event.getOldCurrent() == 0) {
            // TODO: find antenna at location and disable
            log.info("current turned off at "+event.getBlock());

            for (Antenna ant: plugin.ants) {
                // TODO: efficiency
                Block block = world.getBlockAt(ant.location);
                log.info("ant block:"+block);
            }
        }
        */
    }
}

class PlayerInteractListener extends PlayerListener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    public PlayerInteractListener(RadioBeacon pl) {
        plugin = pl;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        log.info("clicked " + event.getClickedBlock() + " using " + event.getItem() + ", action " + event.getAction());

        // TODO: if on antenna, tune
    }
}

public class RadioBeacon extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    BlockListener blockListener;
    PlayerListener playerListener;


    public void onEnable() {

        blockListener = new BlockPlaceListener(this);
        playerListener = new PlayerInteractListener(this);

        getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.BLOCK_PLACE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.REDSTONE_CHANGE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        
        getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.PLAYER_INTERACT, playerListener, org.bukkit.event.Event.Priority.Lowest, this);


        log.info("beacon enable");
    }

    public void onDisable() {
        log.info("beacon disable");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("ant")) {
            return false;
        }

        Iterator it = Antenna.ants.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Location loc = (Location)pair.getKey();
            Antenna ant = (Antenna)pair.getValue();

            sender.sendMessage("Antenna at " + ant);
        }

        return true;
    }
}
