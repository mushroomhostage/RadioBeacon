RadioBeacon - build radio towers, navigate to them using compasses!

Help your players find each other or other places of interest by building radio towers,
broadcasting signals to be picked up by anyone within range and the proper equipment. 

Works great on "random spawn point" (solitude/apocalypse) themed servers.

# Features
* Uses existing in-game items (iron blocks, bars, compasses)
* No commands or client mods needed
* Antennas continuously transmit, larger antennas transmit further
* Message can be included in transmissions
* Tune compasses to received transmission, navigate to the source
* High-performance and light-weight implementation
* Receive transmissions even if chunk is unloaded
* Antennas are affected by weather 
* Multi-world aware

# Usage

### Fixed Antenna Towers
[Screenshots](http://imgur.com/a/Ft06F)

To create a new radio beacon tower, place an iron block next to powered redstone.

The redstone current activates the antenna and it begins broadcasting signals within
a three-dimensional spherical range, available to be received by other fixed antennas
(simply click the iron block to show the received signals) or mobile radios
(compasses held in your hand automatically scan for signals periodically).

**Extending Range**: To transmit and receive further, place any number of iron bars on top of the iron block.

As you build up, the new antenna radius will be shown to you in the chat area. The 
antenna tip can be destroyed to decrease the radius, or broken in the middle, and it 
behaves as you would expect, broadcasting from the highest contiguous iron bar.

**Setting Messages**: Attach a sign to the side of your fixed antenna base, and the text of the sign will 
automatically be included in the transmission.

**Weather**: Rain decreases the transmission and reception radius. Thundering slightly increases it,
but lightning has a chance to strike antennas, causing their destruction.

[Screenshots](http://imgur.com/a/qrh1A)


### Mobile Radios
Fixed antennas can receive and transmit, but are limited by their lack of mobility.
Compasses serve as mobile radios, allowing for both receiving signals and navigating
to their origin.

To receive transmissions, hold a compass in your hand. It will continuously scan and
report any nearby signals, their range, and message (if any). Switching to another item
will turn off scanning.

To tune into a signal, left- or right-click the compass. The next scan will report
which signal you have locked onto, and the compass needle will point in its direction.

### Tips & Tricks
Enderpearls work very well for maintaining tall antennas. Thrown just right,
they will let you teleport inside of the iron bars, without falling, and also help you safely get
back down. Other teleportation mechanisms (such as the Bow + Feather Falling enchantment
from [EnchantMore](http://dev.bukkit.org/server-mods/enchantmore/)) can also be helpful,
or worst case, you could always pillar up.

If you are worried about lightning strikes (note: the lightning attraction can be
tweaked in the configuration, and the damage can be disabled entirely if desired), 
you can build an additional, taller antenna to act as a sort of "lightning rod" and detract lightning 
away from your other antennas. Encase the iron block in obsidian or water to contain the
explosion damage.

Antennas work great underground. Increase the range in the configuration file and have your
players use compasses to locate buried treasure. Or want to encourage highly visible above-ground 
towers? Set the minimum base antenna Y in the configuration, and lower the radius increase per
block.

## Configuration
RadioBeacon is highly configurable. For a full list of configuration options, see config.yml.

## Permissions and Commands
RadioBeacon only adds one new command, /antennas. Players can use it to see the total number of
antennas on the server, but no other information. Ops (by default) can see detailed information.

* radiobeacon.reveal (op): Allows you to list the coordinates of all antennas

* radiobeacon.admin (op): Allows you to save/load/repair antennas

Subcommands:

* /antennas save: Force saving antennas to disk (antennas.yml)
* /antennas load: Force reload from disk
* /antennas check: Compare all antennas against the actual blocks in the world, rebuilding the internal state if necessary. Should not be necessary during normal gameplay, but if blocks are updated without RadioBeacon receiving any events (for example, by editing the world in MCEdit), admins can use this command to repair any affected antennas.


## Servers
* [Solitude - An Apocalypse Server (Blocktopia)](http://blocktopia.net/forum/threads/solitude-an-apocalypse-server.4712/)

## See Also

* [Cake's Miner Apocalypse](http://dev.bukkit.org/server-mods/cakes-miner-apocalypse/) - a significant inspiration for RadioBeacon, with a somewhat different realization of radios; also includes many other apocalypse-related features

* [ApocalypseCraft](http://forums.bukkit.org/threads/wgen-rpg-mech-apocalypsecraft-instances-radiation-realism-perks-factions-and-more.23197/) - 
 ([notes](http://www.reddit.com/r/minerapocalypse/comments/oyhpq/anyone_else_see_this_plugin_apocalypsecraft/))

