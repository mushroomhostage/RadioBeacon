
package me.exphc.RadioBeacon;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.*;
import org.bukkit.Material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.*;

class Log {
    static Logger log = Logger.getLogger("Minecraft");
}


// 2D integral location (unlike Bukkit's location)
class AntennaXZ implements Comparable {
    World world;
    int x, z;

    public AntennaXZ(World w, int x0,  int z0) {
        world = w;
        x = x0;
        z = z0;
    }

    public AntennaXZ(Location loc) {
        world = loc.getWorld();
        x = loc.getBlockX();
        z = loc.getBlockZ();
    }

    public Location getLocation(double y) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public String toString() {
        return x + "," + z;
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof AntennaXZ)) {
            return -1;
        }
        AntennaXZ rhs = (AntennaXZ)obj;

        if (!world.equals(rhs.world)) {
            return world.getName().compareTo(rhs.world.getName());
        }

        if (x - rhs.x != 0) {
            return x - rhs.x;
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
        return x * z;
    }
}


class Antenna {
    static Logger log = Logger.getLogger("Minecraft");

    // TODO: map by world first! Multi-world support
    static public ConcurrentHashMap<AntennaXZ, Antenna> xz2Ant = new ConcurrentHashMap<AntennaXZ, Antenna>();

    AntennaXZ xz;
    int baseY, tipY;

    String message;

    // Normal antenna creation method
    public Antenna(Location loc) {
        xz = new AntennaXZ(loc);
        baseY = (int)loc.getY();
        tipY = baseY;

        xz2Ant.put(xz, this);

        log.info("New antenna " + this);
    }

    // Load from serialized format (from disk)
    public Antenna(Map<String,Object> d) {
        World world;

        if (d.get("world") != null) {
            // Legacy world
            world = Bukkit.getWorld(UUID.fromString((String)d.get("world")));
            if (world == null) {
                // TODO: gracefully handle, and skip this antenna (maybe its world was deleted, no big deal)
                throw new RuntimeException("Antenna loading failed, no world with UUID: " + d.get("world"));
            }
        } else {
            world = Bukkit.getWorld((String)d.get("W"));
            if (world == null) {
                throw new RuntimeException("Antenna loading failed, no world with name: "+ d.get("W"));
            }
        }

        if (d.get("baseX") != null) {
            // Legacy format, tipX and tipZ are redundant
            xz = new AntennaXZ(world, (Integer)d.get("baseX"), (Integer)d.get("baseZ"));
        } else {
            xz = new AntennaXZ(world, (Integer)d.get("X"), (Integer)d.get("Z"));
        }
        baseY = (Integer)d.get("baseY");
        tipY = (Integer)d.get("tipY");

        setMessage((String)d.get("message"));

        xz2Ant.put(xz, this);

        log.info("Loaded antenna " + this);
    }

    // Dump to serialized format (to disk)
    public HashMap<String,Object> dump() {
        HashMap<String,Object> d = new HashMap<String,Object>();

        // For simplicity, dump as a flat data structure

        //d.put("world", xz.world.getUID().toString());
        d.put("W", xz.world.getName());

        d.put("X", xz.x);
        d.put("Z", xz.z);
        d.put("baseY", baseY);
        d.put("tipY", tipY);

        d.put("message", message);
        // TODO: other user data?

        return d;
    }

    public String toString() {
        return "<Antenna r="+getBroadcastRadius()+" height="+getHeight()+" xz="+xz+" baseY="+baseY+" tipY="+tipY+" w="+xz.world.getName()+" m="+message+">";
    }

    public static Antenna getAntenna(Location loc) {
        return getAntenna(new AntennaXZ(loc));
    }
    public static Antenna getAntenna(AntennaXZ loc) {
        Antenna a = xz2Ant.get(loc);
        return a;
    }

    // Get an antenna by base directly adjacent to given location
    public static Antenna getAntennaByAdjacent(Location loc) {
        for (int x = -1; x <= 1; x += 1) {
            for (int z = -1; z <= 1; z += 1) {
                Antenna ant = getAntenna(loc.clone().add(x+0.5, 0, z+0.5));
                if (ant != null) {
                    return ant;
                }
            }
        }
        return null;
    }

    public static void destroy(Antenna ant) {
        //destroyTip(ant);
        //destroyBase(ant);

        if (xz2Ant.remove(ant.xz) == null) {
            throw new RuntimeException("No antenna at "+ant.xz+" to destroy!");
        }

        log.info("Destroyed antenna " + ant);
    }

    /*
    public static void destroyTip(Antenna ant) {
        if (tipsAt.remove(ant.tipAt) == null) {
            throw new RuntimeException("No antenna tip found to destroy at " + ant.tipAt);
        }
    }

    public static void destroyBase(Antenna ant) {
        if (basesAt.remove(ant.baseAt) == null) {
            throw new RuntimeException("No antenna base found to destroy at " + ant.baseAt);
        }
    }*/

    // Set or get textual message being broadcasted (may be null for none)
    public void setMessage(String m) {
        message = m;
    }

    public String getMessage() {
        return message;
    }

    // Extend or shrink size of the antenna, updating the new center location
    public void setTipLocation(Location newLoc) {
        setTipY(newLoc.getBlockY());
    }
    public void setTipY(int newTipY) {
        log.info("Move tip from "+tipY+" to + " +newTipY);
        tipY = newTipY;
    }

    public Location getTipLocation() {
        return xz.getLocation(tipY);
    }

    public Location getSourceLocation() {
        return Configurator.fixedRadiateFromTip ? getTipLocation() : getBaseLocation();
    }
    
    public Location getBaseLocation() {
        return xz.getLocation(baseY);
    }

    public int getHeight() {
        return tipY - baseY;
    }

    public int getBroadcastRadius() {
        int height = getHeight();
        if (Configurator.fixedMaxHeight != 0 && height > Configurator.fixedMaxHeight) {
            // Above max will not extend range
            height = Configurator.fixedMaxHeight;
        } 


        // TODO: exponential not multiplicative?
        int radius = Configurator.fixedInitialRadius + height * Configurator.fixedRadiusIncreasePerBlock;

        if (xz.world.hasStorm()) {
            radius = (int)((double)radius * Configurator.fixedRadiusStormFactor);
        }
        if (xz.world.isThundering()) {
            radius = (int)((double)radius * Configurator.fixedRadiusThunderFactor);
        }

        return radius;
    }

    // 2D radius within lightning strike will strike base
    public int getLightningAttractRadius() {
        int attractRadius = Configurator.fixedLightningAttractRadiusInitial + getHeight() * Configurator.fixedLightningAttractRadiusIncreasePerBlock;

        return Math.min(attractRadius, Configurator.fixedLightningAttractRadiusMax);
    }

    // Explosive power on direct lightning strike
    public float getBlastPower() {
        float power = Configurator.fixedBlastPowerInitial + getHeight() * Configurator.fixedBlastPowerIncreasePerBlock;

        return Math.min(power, Configurator.fixedBlastPowerMax);
    }

    public boolean withinReceiveRange(Location receptionLoc, int receptionRadius) {
        if (!xz.world.equals(receptionLoc.getWorld())) {
            // No cross-world communicatio... yet! TODO: how?
            return false;
        }
       
        // Sphere intersection of broadcast range from source
        // TODO: asymmetric send/receive radii?
        return getSourceLocation().distanceSquared(receptionLoc) < square(getBroadcastRadius() + receptionRadius);
    }

    // Return whether location is within a two-dimensional radius of antenna
    public boolean within2DRadius(Location otherLoc, int radius) {
        if (!xz.world.equals(otherLoc.getWorld())) {
            // No cross-world communicatio... yet! TODO: how?
            return false;
        }
 
        Location otherLoc2d = otherLoc.clone();
        Location baseLoc = getBaseLocation();

        otherLoc2d.setY(baseLoc.getY());

        return baseLoc.distance(otherLoc2d) < radius;
    }

    // Square a number, returning a double as to not overflow if x>sqrt(2**31)
    private static double square(int x) {
        return (double)x * (double)x;
    }

    public int getDistance(Location receptionLoc) {
        return (int)Math.sqrt(getSourceLocation().distanceSquared(receptionLoc));
    }

    // Receive antenna signals (to this antenna) and show to player
    public void receiveSignals(Player player) {
        player.sendMessage("Antenna range: " + getBroadcastRadius() + " m, lightning attraction: " + getLightningAttractRadius() + " m" + ", blast power: " + getBlastPower());

        receiveSignals(player, getSourceLocation(), getBroadcastRadius(), false);
    }

    // Receive signals from mobile radio held by player
    static public void receiveSignalsAtPlayer(Player player) {
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.COMPASS) {
            // Compass = mobile radio
            return;
        }

        Location receptionLoc = player.getLocation();
        int receptionRadius = getCompassRadius(item);

        Antenna.receiveSignals(player, receptionLoc, receptionRadius, true);
    }

    // Get reception radius for a stack of compasses
    // The default of one compass has a radius of 0, meaning you must be directly within range,
    // but more compasses can increase the range further
    static public int getCompassRadius(ItemStack item) {
        // Bigger stack of compasses = better reception!
        int n = item.getAmount() - 1;
        int receptionRadius = Configurator.mobileInitialRadius + n * Configurator.mobileIncreaseRadius;

        return receptionRadius;
    }


    // Receive signals from standing at any location
    static public void receiveSignals(Player player, Location receptionLoc, int receptionRadius, boolean signalLock) {
        Iterator it = Antenna.xz2Ant.entrySet().iterator();
        int count = 0;
        List<Antenna> nearbyAnts = new ArrayList<Antenna>();

        // TODO: can we get deterministic iteration order? for target index
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Antenna otherAnt = (Antenna)pair.getValue();

            if (otherAnt.withinReceiveRange(receptionLoc, receptionRadius)) {
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

            Antenna antLoc = nearbyAnts.get(targetInt);
            targetLoc = antLoc.getSourceLocation();
            player.setCompassTarget(targetLoc);

            player.sendMessage("Locked onto signal at " + antLoc.getDistance(player.getLocation()) + " m: " + antLoc.getMessage());
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

class BlockPlaceListener implements Listener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    public BlockPlaceListener(RadioBeacon pl) {
        plugin = pl;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Building an antenna
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Configurator.fixedBaseMaterial) {
            // Base material for antenna, if powered
            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                if (block.getY() < Configurator.fixedBaseMinY) {
                    player.sendMessage("Not creating antenna below depth of " + Configurator.fixedBaseMinY + " m");
                } else {
                    new Antenna(block.getLocation());
                    player.sendMessage("New antenna created");
                }
            } else {
                if (Configurator.fixedUnpoweredNagMessage != null && !Configurator.fixedUnpoweredNagMessage.equals("")) {
                    player.sendMessage(Configurator.fixedUnpoweredNagMessage);
                }
            }
        } else if (block.getType() == Configurator.fixedAntennaMaterial) {
            Antenna ant = Antenna.getAntenna(block.getLocation());
            if (ant == null) {
                // No antenna at this xz column to extend
                return;
            }

            World world = block.getLocation().getWorld();
            int x = block.getLocation().getBlockX();
            int z = block.getLocation().getBlockZ();
            int placedY = block.getLocation().getBlockY();
            if (placedY < ant.baseY) {
                // Coincidental placement below antenna
                return;
            }

            // Look up until hit first non-antenna material
            // If pillaring up, won't enter loop at all
            // But we have to check, so antennas with gaps can be 'repaired' to extend their
            // range to their full tip
            int newTipY = placedY;
            while(world.getBlockTypeIdAt(x, newTipY, z) == Configurator.fixedAntennaMaterial.getId()) {
                newTipY += 1;
            }

            ant.setTipY(newTipY);
            player.sendMessage("Extended antenna range to " + ant.getBroadcastRadius() + " m");
        } 
    }

    // Destroying an antenna
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        World world = block.getWorld();

        if (block.getType() == Configurator.fixedBaseMaterial) {
            Antenna ant = Antenna.getAntenna(block.getLocation());
            
            if (ant == null) {
                // No antenna at this xz column
                return;
            }

            if (ant.baseY != block.getLocation().getBlockY()) {
                // A coincidental iron block above or below the actual antenna base
                return;
            }

            ant.destroy(ant);
            event.getPlayer().sendMessage("Destroyed antenna");
        } else if (block.getType() == Configurator.fixedAntennaMaterial) {
            Antenna ant = Antenna.getAntenna(block.getLocation());

            if (ant == null) {
                return;
            }

            int destroyedY = block.getLocation().getBlockY();

            if (destroyedY < ant.baseY || destroyedY > ant.tipY) {
                // A coincidental antenna block below or above the antenna, ignore
                return;
            } 

            // Look down from the broken tip, to the first intact antenna/base piece
            int newTipY = destroyedY;
            int pieceType;
            int x = block.getLocation().getBlockX();
            int z = block.getLocation().getBlockZ();
            // Nearly always, this will only execute once, but if the antenna changed 
            // without us knowing, just be sure, and check the block(s) below until
            // we find valid antenna material. Note, this will only find the first
            // gap--if somehow other blocks below get destroyed, we won't know.
            do {
                newTipY -= 1;

                pieceType = world.getBlockTypeIdAt(x, newTipY, z);
            } while(pieceType != Configurator.fixedBaseMaterial.getId() &&
                    pieceType != Configurator.fixedAntennaMaterial.getId() &&
                    newTipY > 0);

            ant.setTipY(newTipY);
            event.getPlayer().sendMessage("Shrunk antenna range to " + ant.getBroadcastRadius() + " m");

            // TODO: also check when destroyed by explosions or other means!
        } else if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
            Antenna ant = Antenna.getAntennaByAdjacent(block.getLocation());
            if (ant == null) {
                return;
            }
            event.getPlayer().sendMessage("Cleared antenna message");
            ant.message = null;
        }
    }

    // Signs to set transmission message
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        String[] text = event.getLines();

        Antenna ant = Antenna.getAntennaByAdjacent(block.getLocation());
        if (ant != null) {
            ant.message = joinString(text);
            event.getPlayer().sendMessage("Set transmission message: " + ant.message);
        }
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
    @EventHandler(priority = EventPriority.LOWEST)
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

class PlayerInteractListener implements Listener {
    Logger log = Logger.getLogger("Minecraft");
    RadioBeacon plugin;

    // Compass targets index selection
    static ConcurrentHashMap<Player, Integer> playerTargets = new ConcurrentHashMap<Player, Integer>();

    public PlayerInteractListener(RadioBeacon pl) {
        plugin = pl;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (block != null && block.getType() == Configurator.fixedBaseMaterial) {
            Antenna ant = Antenna.getAntenna(block.getLocation());
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
                // Tune up or down
                int delta;
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                    delta = -1;
                } else {
                    delta = 1;
                }
                // TODO: show direction in message?

                targetInt = targetInteger.intValue() + delta;
                // TODO: cycle back if right-click
                playerTargets.put(player, targetInt);
            }
            int receptionRadius = Antenna.getCompassRadius(item);
            player.sendMessage("Tuned radio" + (receptionRadius == 0 ? "" : " (range " + receptionRadius + " m)"));

        }
        /*
        // For testing purposes, strike lightning
        // TODO: remove
        else if (item != null && item.getType() == Material.DIAMOND_SWORD && block != null && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            World world = block.getWorld();

            world.setStorm(true);

            world.strikeLightning(block.getLocation());
        }*/
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && item.getType() == Material.COMPASS) {
            Antenna.receiveSignalsAtPlayer(player);
        }
    } 
}


// Periodically check for nearby signals to receive at mobile compass radios
class ReceptionTask implements Runnable {
    Logger log = Logger.getLogger("Minecraft");
    Plugin plugin;
    int taskId;

    public ReceptionTask(Plugin p) {
        plugin = p;
    }

    public void run() {
        for (Player player: Bukkit.getOnlinePlayers()) {
            ItemStack item = player.getItemInHand();

            if (item != null && item.getType() == Material.COMPASS) {
                // Compass = mobile radio
                Antenna.receiveSignalsAtPlayer(player);
            }
        }

        Configurator.saveAntennas(plugin); 
    }
}

class Configurator {
    static Logger log = Logger.getLogger("Minecraft");

    // Configuration options
    static int fixedInitialRadius;
    static int fixedRadiusIncreasePerBlock;
    static int fixedLightningAttractRadiusInitial;
    static int fixedLightningAttractRadiusIncreasePerBlock;
    static int fixedLightningAttractRadiusMax;
    static boolean fixedWeatherListener;
    static boolean fixedBlastSetFire;
    static float fixedBlastPowerInitial;
    static float fixedBlastPowerIncreasePerBlock;
    static float fixedBlastPowerMax;
    static double fixedRadiusStormFactor;
    static double fixedRadiusThunderFactor;
    static int fixedMaxHeight;
    static int fixedBaseMinY;
    static Material fixedBaseMaterial;
    static Material fixedAntennaMaterial;
    static boolean fixedRadiateFromTip;
    static String fixedUnpoweredNagMessage;

    static int mobileInitialRadius;
    static int mobileIncreaseRadius;
    static int mobileTaskStartDelaySeconds;
    static int mobileTaskPeriodSeconds;


    static public boolean load(Plugin plugin) {
        if (plugin.getConfig().getInt("version", -1) < 0) {
            // Not present, initialize
            createNew(plugin);
        }


        fixedInitialRadius = plugin.getConfig().getInt("fixedInitialRadius", 100);
        fixedRadiusIncreasePerBlock = plugin.getConfig().getInt("fixedRadiusIncreasePerBlock", 100);
        
        fixedLightningAttractRadiusInitial = plugin.getConfig().getInt("fixedLightningAttractRadiusInitial", 10);
        fixedLightningAttractRadiusIncreasePerBlock = plugin.getConfig().getInt("fixedLightningAttractRadiusIncreasePerBlock", 1);
        fixedLightningAttractRadiusMax = plugin.getConfig().getInt("fixedLightningAttractRadiusMax", 15);
        fixedWeatherListener = plugin.getConfig().getBoolean("fixedWeatherListener", true);

        fixedBlastSetFire = plugin.getConfig().getBoolean("fixedBlastSetFire", true);
        fixedBlastPowerInitial = (float)plugin.getConfig().getDouble("fixedBlastPowerInitial", 2);
        fixedBlastPowerIncreasePerBlock = (float)plugin.getConfig().getDouble("fixedBlastPowerIncreasePerBlock", 0.4);
        fixedBlastPowerMax = (float)plugin.getConfig().getDouble("fixedBlastPowerMax", 10);


        fixedRadiusStormFactor = plugin.getConfig().getDouble("fixedRadiusStormFactor", 0.7);
        fixedRadiusThunderFactor = plugin.getConfig().getDouble("fixedRadiusThunderFactor", 1.1);

        fixedMaxHeight = plugin.getConfig().getInt("fixedMaxHeightMeters", 0);

        //if (config.getString("fixedBaseMinY") != null && config.getString("fixedBaseMinY").equals("sealevel")) {  
        // TODO: sea level option? but depends on world
        fixedBaseMinY = plugin.getConfig().getInt("fixedBaseMinY", 0);

        fixedBaseMaterial = Material.matchMaterial(plugin.getConfig().getString("fixedBaseMaterial"));
        if (fixedBaseMaterial == null) {
            log.severe("Failed to match fixedBaseMaterial");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        fixedAntennaMaterial = Material.matchMaterial(plugin.getConfig().getString("fixedAntennaMaterial"));
        if (fixedAntennaMaterial == null) {
            log.severe("Failed to match fixedAntennaMaterial");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        String f = plugin.getConfig().getString("fixedRadiateFrom", "tip");
        if (f.equals("tip")) {
            fixedRadiateFromTip = true;
        } else if (f.equals("base")) {
            fixedRadiateFromTip = false;
        } else {
            log.severe("fixedRadiateFrom not 'tip' nor 'base'");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }          

        fixedUnpoweredNagMessage = plugin.getConfig().getString("fixedUnpoweredNagMessage", "Tip: remove and place this block near redstone current to build an antenna");



        mobileInitialRadius = plugin.getConfig().getInt("mobileInitialRadius", 0);
        mobileIncreaseRadius = plugin.getConfig().getInt("mobileIncreaseRadius", 10);
        int TICKS_PER_SECOND = 20;
        mobileTaskStartDelaySeconds = plugin.getConfig().getInt("mobileTaskStartDelaySeconds", 0) * TICKS_PER_SECOND;
        mobileTaskPeriodSeconds = plugin.getConfig().getInt("mobileTaskPeriodSeconds", 20) * TICKS_PER_SECOND;
        
        loadAntennas(plugin);


        return true;
    }
   
    // Copy the default config.yml to user's config
    // This is a direct file copy, unlike Bukkit plugin.getConfig().options().copyDefaults(true),
    // so it preserves file comments
    static public boolean createNew(Plugin plugin) {
        String filename = plugin.getDataFolder() + System.getProperty("file.separator") + "config.yml";
        File file = new File(filename);
 
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
    
    static private YamlConfiguration getAntennaConfig(Plugin plugin) {
        String filename = plugin.getDataFolder() + System.getProperty("file.separator") + "antennas.yml";
        File file = new File(filename);
        return YamlConfiguration.loadConfiguration(file);
    }

    // Load saved antenna information
    static public void loadAntennas(Plugin plugin) {
        YamlConfiguration antennaConfig = getAntennaConfig(plugin);

        List<Map<String,Object>> all;
    
        Antenna.xz2Ant = new ConcurrentHashMap<AntennaXZ, Antenna>();   // clear existing

        if (antennaConfig == null || !antennaConfig.isSet("antennas")) {
            log.info("No antennas loaded");
            return;
        }

        all = antennaConfig.getMapList("antennas");

        int i = 0;
        for (Map<String,Object> d: all) {
            new Antenna(d); 
            i += 1;
        }

        log.info("Loaded " + i + " antennas");
    }

    static int lastCount = 0;
    // Save existing antennas
    static public void saveAntennas(Plugin plugin) {
        ArrayList<HashMap<String,Object>> all = new ArrayList<HashMap<String,Object>>();
        YamlConfiguration antennaConfig = getAntennaConfig(plugin);

        //Iterator it = Antenna.tipsAt.entrySet().iterator();
        Iterator it = Antenna.xz2Ant.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Antenna ant = (Antenna)pair.getValue();
    
            all.add(ant.dump());
            count += 1;
        }

        antennaConfig.set("antennas", all);

        try {
            antennaConfig.save(plugin.getDataFolder() + System.getProperty("file.separator") + "antennas.yml");
        } catch (IOException e) {
            log.severe("Failed to save antennas.yml");
        }

        if (count != lastCount) {
            log.info("Saved " + count + " antennas");
            lastCount = count;
        }
    }
}

class RadioWeatherListener implements Listener {
    Logger log = Logger.getLogger("Minecraft");
    Plugin plugin;

    public RadioWeatherListener(Plugin pl) {
        plugin = pl;
            
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLightningStrike(LightningStrikeEvent event) { 
        World world = event.getWorld();
        Location strikeLocation = event.getLightning().getLocation();

        Antenna directAnt = Antenna.getAntenna(strikeLocation);
        if (directAnt != null) {

            // Direct hit!
            log.info("directly hit "+directAnt);

            float power = directAnt.getBlastPower();
            Location baseLoc = directAnt.getBaseLocation();

            if (power > 0) {
                world.createExplosion(baseLoc, power, Configurator.fixedBlastSetFire);
            }

            // Ensure antenna is destroyed
            Block baseBlock = world.getBlockAt(baseLoc);
            if (baseBlock.getType() == Configurator.fixedBaseMaterial) {
                baseBlock.setType(Material.AIR);

                // TODO: log destroyed by lightning
                Antenna.destroy(directAnt);
            }
            // TODO: move destruction check to explosion event? There's ENTITY_EXPODE,
            // but is it fired for any createExplosion? Should make explosions destroy
            // antennas, regardless. If possible.

            return;
        }

        //Iterator it = Antenna.basesAt.entrySet().iterator();
        Iterator it = Antenna.xz2Ant.entrySet().iterator();

        // Find nearby antennas
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Antenna ant = (Antenna)pair.getValue();

            // Within strike range?
            if (ant.within2DRadius(strikeLocation, ant.getLightningAttractRadius())) {
                log.info("striking antenna "+ant+", within range "+ant.getLightningAttractRadius()+" of "+strikeLocation);
                //world.strikeLightning(ant.baseAt.getLocation());
                world.strikeLightning(ant.getBaseLocation());
            }
        }
 
    }
}


public class RadioBeacon extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    Listener blockListener;
    Listener playerListener;
    Listener weatherListener;
    ReceptionTask receptionTask;

    public void onEnable() {
        log.info(getDescription().getFullName() + " loading");

        if (!Configurator.load(this)) {
            return;
        }


        blockListener = new BlockPlaceListener(this);
        playerListener = new PlayerInteractListener(this);

        if (Configurator.fixedWeatherListener) {
            weatherListener = new RadioWeatherListener(this);
        } else {
            weatherListener = null;
        }

        receptionTask = new ReceptionTask(this);

        // Compass notification task
        int taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, receptionTask, 
            Configurator.mobileTaskStartDelaySeconds,
            Configurator.mobileTaskPeriodSeconds);

        if (taskId == -1) {
            log.severe("Failed to schedule radio signal reception task");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        receptionTask.taskId = taskId;
    }

    public void onDisable() {
        log.info(getDescription().getFullName() + " shutting down");

        Configurator.saveAntennas(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("antennas")) {
            return false;
        }

        if (args.length > 0) {
            if (args[0].equals("list")) {
                listAntennas(sender);
            } else if (args[0].equals("save")) {
                if (!(sender instanceof Player) || ((Player)sender).hasPermission("radiobeacon.saveload")) {
                    Configurator.saveAntennas(this);
                } else {
                    sender.sendMessage("You do not have permission to save antennas");
                }
            } else if (args[0].equals("load")) {
                if (!(sender instanceof Player) || ((Player)sender).hasPermission("radiobeacon.saveload")) {
                    Configurator.loadAntennas(this);
                } else {
                    sender.sendMessage("You do not have permission to load antennas");
                }
            }
        } else {
            listAntennas(sender);
        }

        return true;
    }

    // Show either all antennas information, if have permission, or count only if not
    private void listAntennas(CommandSender sender) {
        //Iterator it = Antenna.tipsAt.entrySet().iterator();
        Iterator it = Antenna.xz2Ant.entrySet().iterator();
        int count = 0;
        boolean reveal = !(sender instanceof Player) || ((Player)sender).hasPermission("radiobeacon.reveal");

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            AntennaXZ xz = (AntennaXZ)pair.getKey();
            Antenna ant = (Antenna)pair.getValue();

            if (reveal) {
                sender.sendMessage("Antenna: " + ant);
            }
            count += 1;
        }

        sender.sendMessage("There are " + count + " antennas" + (!reveal ? " out there somewhere" : ""));
    }
}
