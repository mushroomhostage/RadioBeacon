
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

        ants.put(location, this);
        log.info("New antenna at " + loc);
    }

    public static Antenna getAntenna(Location loc) {
        return ants.get(loc);
    }

    public static Antenna getAntenna(Block block) {
        return getAntenna(block.getLocation());
    }

    public static void destroy(Antenna ant) {
        ants.remove(ant.location);
    }

    // Extend or shrink size of the antenna, updating the new center location
    public void move(Location newLoc) {
        destroy(this);

        location = newLoc;

        ants.put(location, this);
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
        return "<Antenna at "+location.getX()+","+location.getY()+","+location.getZ()+">";
    }
}

class BlockPlaceListener extends BlockListener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    public BlockPlaceListener(RadioBeacon pl) {
        plugin = pl;
    }

    // Building an antenna
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.IRON_BLOCK) {
            // Base material for antenna, if powered
            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                player.sendMessage("New antenna " + new Antenna(block.getLocation()));
            }
        } else if (block.getType() == Material.IRON_FENCE) {
            Block against = event.getBlockAgainst();

            //player.sendMessage("Placing against " + against);
            Antenna existingAnt = Antenna.getAntenna(against);
            if (existingAnt != null) {
                existingAnt.move(block.getLocation());
                player.sendMessage("Extended antenna to " + existingAnt);
            }
        }
    }

    // Destroying an antenna
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();

        if (block.getType() == Material.IRON_BLOCK || block.getType() == Material.IRON_FENCE) {
            Antenna existingAnt = Antenna.getAntenna(block);

            if (existingAnt != null) {
                Location locBelow = existingAnt.location.subtract(0, 1, 0);
                Block blockBelow = world.getBlockAt(locBelow);

                if (blockBelow.getType() == Material.IRON_BLOCK || blockBelow.getType() == Material.IRON_FENCE) {
                    existingAnt.move(locBelow);
                    event.getPlayer().sendMessage("Shrunk antenna to " + existingAnt);
                } else {
                    event.getPlayer().sendMessage("Destroyed antenna " + existingAnt);
                    Antenna.destroy(existingAnt);
                }
            }
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
        getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.BLOCK_BREAK, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
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
        int count = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Location loc = (Location)pair.getKey();
            Antenna ant = (Antenna)pair.getValue();

            sender.sendMessage("Antenna at " + ant);
            count += 1;
        }
        sender.sendMessage("Found " + count + " antennas");

        return true;
    }
}
