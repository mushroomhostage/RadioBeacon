
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

// Integral location (unlike Bukkit Location)
class AntennaLocation implements Comparable {
    static Logger log = Logger.getLogger("Minecraft");
    World world;
    int x, y, z;

    public AntennaLocation(World w, int x0, int y0, int z0) {
        world = w;
        x = x0;
        y = y0;
        z = z0;
    }

    public AntennaLocation(Location loc) {
        world = loc.getWorld();
        x = loc.getBlockX();
        y = loc.getBlockY();
        z = loc.getBlockZ();
    }

    public Location getLocation() {
        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    public String toString() {
        return x + "," + y + "," + z;
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof AntennaLocation)) {
            return -1;
        }
        AntennaLocation rhs = (AntennaLocation)obj;

        // TODO: also compare world
        if (x - rhs.x != 0) {
            return x - rhs.x;
        } else if (y - rhs.y != 0) {
            return y - rhs.y;
        } else if (z - rhs.z != 0) {
            return z - rhs.z;
        }

        return 0;
    }

    public boolean equals(Object obj) {
        return compareTo(obj) == 0;      // why do I have to do this myself?
    }

    public int hashCode() {
        // lame hashing TODO: improve?
        return x * y * z;   
    }
}

class Antenna {
    AntennaLocation at;
    boolean enabled;

    static Logger log = Logger.getLogger("Minecraft");

    static ConcurrentHashMap<AntennaLocation, Antenna> ants = new ConcurrentHashMap<AntennaLocation, Antenna>();

    public Antenna(Location loc) {
        at = new AntennaLocation(loc);
        enable();

        ants.put(at, this);
        log.info("New antenna at " + at);
    }

    public static Antenna getAntenna(AntennaLocation a) {
        return ants.get(a);
    }

    public static Antenna getAntenna(Location loc) {
        return getAntenna(new AntennaLocation(loc));
    }

    public static Antenna getAntenna(Block block) {
        return getAntenna(block.getLocation());
    }

    public static void destroy(Antenna ant) {
        if (ants.remove(ant.at) == null) {
            log.info("No antenna at found to destroy at " + ant.at);
        }
    }

    // Extend or shrink size of the antenna, updating the new center location
    public void move(Location newLoc) {
        log.info("Move from "+at+" to + " + newLoc);
        destroy(this);

        at = new AntennaLocation(newLoc);

        ants.put(at, this);
    }

    public Location getLocation() {
        return at.getLocation();
    }

    public void enable() {
        log.info("enabled antenna "+at);
        enabled = true;
    }

    public void disable() {
        log.info("disabled antenna "+at);
        enabled = false;
    }

    public String toString() {
        return "<Antenna at "+at+">";
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
                Location locBelow = existingAnt.getLocation().subtract(0, 1, 0);
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
        Block block = event.getClickedBlock();

        log.info("clicked " + event.getClickedBlock() + " using " + event.getItem() + ", action " + event.getAction());

        if (block.getType() == Material.IRON_BLOCK || block.getType() == Material.IRON_FENCE) {
            Antenna ant = Antenna.getAntenna(block);
            if (ant != null) {
                event.getPlayer().sendMessage("This is antenna " + ant);
                // TODO: also keep iron source block, so don't have to punch top of antenna
                // (i.e., get working with extended antennas)
            }
        }

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
            AntennaLocation at = (AntennaLocation)pair.getKey();
            Antenna ant = (Antenna)pair.getValue();

            sender.sendMessage("Antenna at " + ant);
            count += 1;
        }
        sender.sendMessage("Found " + count + " antennas");

        return true;
    }
}
