
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
    static Logger log = Logger.getLogger("Minecraft");

    // TODO: change location to store x,z for lookup, and tip/base (y) separately. 
    // (Then can detect destroying middle of antenna)
    static ConcurrentHashMap<AntennaLocation, Antenna> tipsAt = new ConcurrentHashMap<AntennaLocation, Antenna>();
    static ConcurrentHashMap<AntennaLocation, Antenna> basesAt = new ConcurrentHashMap<AntennaLocation, Antenna>();

    AntennaLocation tipAt;      // broadcast tip
    AntennaLocation baseAt;     // control station
    boolean enabled;


    public Antenna(Location loc) {
        tipAt = new AntennaLocation(loc);
        baseAt = new AntennaLocation(loc);
        enable();

        tipsAt.put(tipAt, this);
        basesAt.put(baseAt, this);
        log.info("New antenna at " + tipAt);
    }

    public static Antenna getAntennaByTip(Location loc) {
        return tipsAt.get(new AntennaLocation(loc));
    }
    
    public static Antenna getAntennaByBase(Location loc) {
        return basesAt.get(new AntennaLocation(loc));
    }

    public static void destroy(Antenna ant) {
        destroyTip(ant);
        destroyBase(ant);
    }

    public static void destroyTip(Antenna ant) {
        if (tipsAt.remove(ant.tipAt) == null) {
            throw new RuntimeException("No antenna tip found to destroy at " + ant.tipAt);
        }
    }

    public static void destroyBase(Antenna ant) {
        if (basesAt.remove(ant.baseAt) == null) {
            throw new RuntimeException("No antenna base found to destroy at " + ant.baseAt);
        }
    }

    // Extend or shrink size of the antenna, updating the new center location
    public void setTipLocation(Location newLoc) {
        log.info("Move from "+tipAt+" to + " + newLoc);
        destroyTip(this);

        tipAt = new AntennaLocation(newLoc);

        tipsAt.put(tipAt, this);
    }

    public Location getTipLocation() {
        return tipAt.getLocation();
    }
    
    public Location getBaseLocation() {
        return baseAt.getLocation();
    }

    public int getHeight() {
        return tipAt.y - baseAt.y;
    }

    public int getBroadcastRadius() {
        return getHeight() * 100;  // TODO: configurable, tweak
    }

    public void enable() {
        log.info("enabled antenna "+this);
        enabled = true;
    }

    public void disable() {
        log.info("disabled antenna "+this);
        enabled = false;
    }

    public String toString() {
        return "<Antenna r="+getBroadcastRadius()+", height="+getHeight()+", tip="+tipAt+", base="+baseAt+">";
    }

    public boolean withinRange(Antenna rhs) {
        // Sphere intersection of broadcast range from tip
        return distance2(rhs) < square(getBroadcastRadius() + rhs.getBroadcastRadius());
    }

    // Get distance squared to another antenna tip
    private int distance2(Antenna rhs) {
        // d^2 = (x2-x1)^2 + (y2-y1)^2 + (z2-z1)^2
        return square(tipAt.x - rhs.tipAt.x) +
               square(tipAt.y - rhs.tipAt.y) + 
               square(tipAt.z - rhs.tipAt.z);
    }

    public int getDistance(Antenna rhs) {
        return (int)Math.sqrt(distance2(rhs));
    }

    private static int square(int x) {
        return x * x;
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

            Antenna existingAnt = Antenna.getAntennaByTip(against.getLocation());
            if (existingAnt != null) {
                existingAnt.setTipLocation(block.getLocation());
                player.sendMessage("Extended antenna to " + existingAnt);
            }
        }
    }

    // Destroying an antenna
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();

        if (block.getType() == Material.IRON_BLOCK) {
            Antenna existingAnt = Antenna.getAntennaByTip(block.getLocation());
            
            if (existingAnt != null) {
                event.getPlayer().sendMessage("Destroyed antenna " + existingAnt);
                existingAnt.destroy(existingAnt);
            }
        } else if (block.getType() == Material.IRON_FENCE) {
            Antenna existingAnt = Antenna.getAntennaByTip(block.getLocation());

            if (existingAnt != null) {
                // Verify whole length of antenna is intact
                int i = existingAnt.getHeight();
                boolean destroy = false;
                while(i > 0) {
                    Location locBelow = existingAnt.getTipLocation().subtract(0, i, 0); 
                    Block blockBelow = world.getBlockAt(locBelow);

                    if (blockBelow.getType() != Material.IRON_BLOCK && blockBelow.getType() != Material.IRON_FENCE) {
                        destroy = true;
                        break;
                    }
                    
                    i -= 1;
                }
                if (destroy) {
                    // Tip became disconnected from base, so destroy
                    // Note: won't detect all cases, only if tip is destroyed (not connecting blocks)
                    event.getPlayer().sendMessage("Destroyed antenna " + existingAnt);
                    Antenna.destroy(existingAnt);
                } else {
                    existingAnt.setTipLocation(existingAnt.getTipLocation().subtract(0, 1, 0));
                    event.getPlayer().sendMessage("Shrunk antenna to " + existingAnt);
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
        // TODO: require compass? ('radio')

        if (block.getType() == Material.IRON_BLOCK) {
            Antenna ant = Antenna.getAntennaByBase(block.getLocation());
            if (ant != null) {
                event.getPlayer().sendMessage("Antenna: " + ant);

                Iterator it = Antenna.tipsAt.entrySet().iterator();
                int count = 0;
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    Antenna otherAnt = (Antenna)pair.getValue();

                    if (otherAnt == ant) {
                        // self-interference
                        continue;
                    }

                    if (ant.withinRange(otherAnt)) {
                        log.info("Received transmission from " + otherAnt);
                        event.getPlayer().sendMessage("Received transmission " + ant.getDistance(otherAnt) + " m away");
                    }
                }
            }
        }
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

        Iterator it = Antenna.tipsAt.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            AntennaLocation at = (AntennaLocation)pair.getKey();
            Antenna ant = (Antenna)pair.getValue();

            sender.sendMessage("Antenna tip at " + ant);
            // TODO: bases
            count += 1;
        }
        sender.sendMessage("Found " + count + " antennas");

        return true;
    }
}
