
package com.exphc.RadioBeacon;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.Material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
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
    String message;

    static int initialFixedRadius;
    static int radiusIncreasePerBlock;
    static int compassRadius;

    static void loadConfig(YamlConfiguration config) {
        initialFixedRadius = config.getInt("initialFixedRadius");
        radiusIncreasePerBlock = config.getInt("radiusIncreasePerBlock");
        compassRadius = config.getInt("compassRadius");
    }

    public Antenna(Location loc) {
        tipAt = new AntennaLocation(loc);
        baseAt = new AntennaLocation(loc);

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

    // Get an antenna by base directly adjacent to given location
    public static Antenna getAntennaByBaseAdjacent(Location loc) {
        for (int x = -1; x <= 1; x += 1) {
            for (int z = -1; z <= 1; z += 1) {
                Antenna ant = getAntennaByBase(loc.clone().add(x+0.5, 0, z+0.5));
                if (ant != null) {
                    return ant;
                }
            }
        }
        return null;
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
        log.info("Move tip from "+tipAt+" to + " + newLoc);
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
        // TODO: exponential not multiplicative?
        return initialFixedRadius + getHeight() * radiusIncreasePerBlock;
    }

    public String toString() {
        return "<Antenna r="+getBroadcastRadius()+", height="+getHeight()+", tip="+tipAt+", base="+baseAt+">";
    }

    public boolean withinRange(Location receptionLoc, int receptionRadius) {
        // Sphere intersection of broadcast range from tip
        return getTipLocation().distanceSquared(receptionLoc) < square(getBroadcastRadius() + receptionRadius);
    }

    private static int square(int x) {
        return x * x;
    }

    public int getDistance(Location receptionLoc) {
        return (int)Math.sqrt(getTipLocation().distanceSquared(receptionLoc));
    }

    // Receive antenna signals (to this antenna) and show to player
    public void receiveSignals(Player player) {
        player.sendMessage("Antenna range: " + getBroadcastRadius() + " m");

        receiveSignals(player, getTipLocation(), getBroadcastRadius(), false);
    }

    // Receive signals from portable radio held by player
    static public void receiveSignalsAtPlayer(Player player) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.COMPASS) {
            // Compass = portable radio
            return;
        }

        Location receptionLoc = player.getLocation();
        // Bigger stack of compasses = better reception!
        int receptionRadius = item.getAmount() * compassRadius;

        Antenna.receiveSignals(player, receptionLoc, receptionRadius, true);
    }


    // Receive signals from standing at any location
    static public void receiveSignals(Player player, Location receptionLoc, int receptionRadius, boolean signalLock) {
        Iterator it = Antenna.tipsAt.entrySet().iterator();
        int count = 0;
        List<Antenna> nearbyAnts = new ArrayList<Antenna>();

        // TODO: can we get deterministic iteration order? for target index
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Antenna otherAnt = (Antenna)pair.getValue();

            if (otherAnt.withinRange(receptionLoc, receptionRadius)) {
                //log.info("Received transmission from " + otherAnt);

                nearbyAnts.add(otherAnt);

                notifySignal(player, receptionLoc, otherAnt);
            }
        }
        count = nearbyAnts.size();
        if (count == 0) {
            player.sendMessage("No signals within " + receptionRadius + " m");
        } else if (signalLock) {
            // Player radio compass targetting
            Integer targetInteger = PlayerInteractListener.playerTargets.get(player);
            Location targetLoc;
            int targetInt;
            if (targetInteger == null) {
                targetInt = 0;
            } else {
                targetInt = Math.abs(targetInteger.intValue()) % count;
            }

            targetLoc = nearbyAnts.get(targetInt).getTipLocation();
            player.setCompassTarget(targetLoc);

            player.sendMessage("Locked onto signal #" + targetInt);
            //log.info("Targetting " + targetLoc);
        }
    }

    // Tell player about an incoming signal from an antenna
    static private void notifySignal(Player player, Location receptionLoc, Antenna ant) {
        String message = "";
        if (ant.message != null) {
            message = ": " + ant.message;
        }

        int distance = ant.getDistance(receptionLoc);
        if (distance == 0) {
            // Squelch self-transmissions to avoid interference
            return;
        }

        player.sendMessage("Received transmission (" + distance + " m)" + message);
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
                new Antenna(block.getLocation());
                player.sendMessage("New antenna created");
            }
        } else if (block.getType() == Material.IRON_FENCE) {
            Block against = event.getBlockAgainst();

            Antenna existingAnt = Antenna.getAntennaByTip(against.getLocation());
            if (existingAnt != null) {
                existingAnt.setTipLocation(block.getLocation());
                player.sendMessage("Extended antenna range to " + existingAnt.getBroadcastRadius() + " m");
            }
        } 
    }

    // Destroying an antenna
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();

        if (block.getType() == Material.IRON_BLOCK) {
            Antenna ant = Antenna.getAntennaByBase(block.getLocation());
            
            if (ant != null) {
                event.getPlayer().sendMessage("Destroyed antenna " + ant);
                ant.destroy(ant);
            }
        } else if (block.getType() == Material.IRON_FENCE) {
            Antenna ant = Antenna.getAntennaByTip(block.getLocation());

            if (ant != null) {
                // Verify whole length of antenna is intact
                int i = ant.getHeight();
                boolean destroy = false;
                while(i > 0) {
                    Location locBelow = ant.getTipLocation().subtract(0, i, 0); 
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
                    event.getPlayer().sendMessage("Destroyed antenna " + ant);
                    Antenna.destroy(ant);
                } else {
                    ant.setTipLocation(ant.getTipLocation().subtract(0, 1, 0));
                    event.getPlayer().sendMessage("Shrunk antenna range to " + ant.getBroadcastRadius() + " m");
                }
            }
        } else if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
            Antenna ant = Antenna.getAntennaByBaseAdjacent(block.getLocation());
            if (ant != null) {
                event.getPlayer().sendMessage("Cleared antenna message");
                ant.message = null;
            }
        }
    }

    // Signs to set transmission message
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        String[] text = event.getLines();

        Antenna ant = Antenna.getAntennaByBaseAdjacent(block.getLocation());
        if (ant != null) {
            ant.message = joinString(text);
        }
        event.getPlayer().sendMessage("Set transmission message: " + ant.message);
    }

    public static String joinString(String[] a) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < a.length; i+= 1) {
            buffer.append(a[i]);
            buffer.append(" ");
        }
        return buffer.toString();
    }

    // Currently antennas retain their magnetized properties even when redstone current is removed
/*
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
         // TODO: find out how to disable antennas, get when block becomes unpowered
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
    }
        */
}

class PlayerInteractListener extends PlayerListener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    // Compass targets index selection
    static ConcurrentHashMap<Player, Integer> playerTargets = new ConcurrentHashMap<Player, Integer>();

    public PlayerInteractListener(RadioBeacon pl) {
        plugin = pl;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (block != null && block.getType() == Material.IRON_BLOCK) {
            Antenna ant = Antenna.getAntennaByBase(block.getLocation());
            if (ant == null) {
                return;
            }

            ant.receiveSignals(player);
            // TODO: also activate if click the _sign_ adjacent to the base
            // TODO: and if click anywhere within antenna? maybe not unless holding compass
        } else if (item != null && item.getType() == Material.COMPASS) {
            // Increment target index
            Integer targetInteger = playerTargets.get(player);
            int targetInt;
            if (targetInteger == null) {
                playerTargets.put(player, 0);
                targetInt = 0;
            } else {
                targetInt = targetInteger.intValue() + 1;
                playerTargets.put(player, targetInt);
            }
            player.sendMessage("Tuned radio");
        }
    }

    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && item.getType() == Material.COMPASS) {
            Antenna.receiveSignalsAtPlayer(player);
        }
    } 
}


class ReceptionTask implements Runnable {
    Logger log = Logger.getLogger("Minecraft");
    int taskId;

    public void run() {
        for (Player player: Bukkit.getOnlinePlayers()) {
            ItemStack item = player.getItemInHand();

            if (item != null && item.getType() == Material.COMPASS) {
                // Compass = portable radio
                Antenna.receiveSignalsAtPlayer(player);
            }
        }
    }
}

class Configurator {
    static Logger log = Logger.getLogger("Minecraft");
    static Plugin plugin;
    static YamlConfiguration config;

    static public boolean load() {
        String filename = plugin.getDataFolder() + System.getProperty("file.separator") + "config.yml";
        File file = new File(filename);
        
        if (!file.exists()) {
            if (!createNew(file)) {
                throw new RuntimeException("Could not create new configuration file: " + filename);
            }
        }

        config = YamlConfiguration.loadConfiguration(new File(filename));
        if (config == null) {
            log.severe("Failed to load configuration file " + filename);
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        log.info("load config="+config);

        Antenna.loadConfig(config);

        return true;
    }
    
    static public boolean createNew(File file) {
        FileWriter fileWriter;
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }

        try {
            fileWriter = new FileWriter(file);
        } catch (IOException e) {
            log.severe("Couldn't write config file: " + e.getMessage());
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(plugin.getResource("config.yml"))));
        BufferedWriter writer = new BufferedWriter(fileWriter);
        try {
            String line = reader.readLine();
            while (line != null) {
                writer.write(line + System.getProperty("line.separator"));
                line = reader.readLine();
            }
            log.info("Wrote default config");
        } catch (IOException e) {
            log.severe("Error writing config: " + e.getMessage());
            return false;
        } finally {
            try {
                writer.close();
                reader.close();
            } catch (IOException e) {
                log.severe("Error saving config: " + e.getMessage());
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
        return true;
    }
}

public class RadioBeacon extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    BlockListener blockListener;
    PlayerListener playerListener;
    ReceptionTask receptionTask;

    public void onEnable() {
        Configurator.plugin = this;
        if (!Configurator.load()) {
            return;
        }


        blockListener = new BlockPlaceListener(this);
        playerListener = new PlayerInteractListener(this);
        receptionTask = new ReceptionTask();

        Bukkit.getPluginManager().registerEvent(org.bukkit.event.Event.Type.BLOCK_PLACE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        Bukkit.getPluginManager().registerEvent(org.bukkit.event.Event.Type.BLOCK_BREAK, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        Bukkit.getPluginManager().registerEvent(org.bukkit.event.Event.Type.SIGN_CHANGE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        //TODO? getServer().getPluginManager().registerEvent(org.bukkit.event.Event.Type.REDSTONE_CHANGE, blockListener, org.bukkit.event.Event.Priority.Lowest, this);
        
        Bukkit.getPluginManager().registerEvent(org.bukkit.event.Event.Type.PLAYER_INTERACT, playerListener, org.bukkit.event.Event.Priority.Lowest, this);
        Bukkit.getPluginManager().registerEvent(org.bukkit.event.Event.Type.PLAYER_ITEM_HELD, playerListener, org.bukkit.event.Event.Priority.Lowest, this);

        // Compass notification task
        int TICKS_PER_SECOND = 20;
        int taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, receptionTask, 
            Configurator.config.getInt("compassTaskStartDelaySeconds", 0) * TICKS_PER_SECOND, 
            Configurator.config.getInt("compassTaskPeriodSeconds", 20) * TICKS_PER_SECOND);

        if (taskId == -1) {
            log.severe("Failed to schedule radio signal reception task");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        receptionTask.taskId = taskId;


        // TODO: load saved antennas
        log.info("beacon enable");
    }

    public void onDisable() {
        // TODO: load saved antennas
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
