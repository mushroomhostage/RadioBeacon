RadioBeacon - build radio towers, navigate to them using compasses!

Help your players find each other or other places of interest by building radio towers,
broadcasting signals to be picked up by anyone within range and the proper equipment. 

Works great on "random spawn point" (solitude/apocalypse) themed servers.

*[Download RadioBeacon 1.2.5](http://dev.bukkit.org/server-mods/radiobeacon/files/8-radio-beacon-1-2-5/)* - released 2012/05/19 for 1.2.5

# Features
* Uses existing in-game items (iron blocks, bars, compasses)
* No commands or client mods needed
* Antennas continuously transmit, larger antennas transmit further
* Message can be included in transmissions
* Tune compasses to received transmission, navigate to the source
* High-performance and light-weight implementation
* Receive transmissions even if chunk is unloaded
* Antennas are affected by weather 
* Relay antennas for retransmitting messages
* Multi-world aware

# Usage

### Fixed Antenna Towers
***[Screenshots and photo tutorial](http://imgur.com/a/Ft06F)***

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

**Relay Antennas**: Built using a gold block instead of an iron block, relays
retransmit messages from nearby antennas. To set a relay message, build a normal antenna
within range and set its message. All relays within range will be notified using a special signal.
Relays retain their message even if the original antenna is destroyed, so they can be used to add redundancy
or extra range.

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

If it interferes with other plugins which use compasses, players can turn off their
radios using the /toggleradio command.

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

Playing on a hardcore PvP server and having trouble with people stealing your radio tower iron blocks?
Why not [rig it with TNT](http://blocktopia.net/forum/threads/solitude-an-apocalypse-server.4712/page-6#post-77730)?

You can navigate to antennas on foot of course, but RadioBeacon also works very well with other
plugins or mods offering alternative modes of transportation, *especially* flight:

[Screenshot](http://i.imgur.com/8xUJK.jpg) of RadioBeacon being used while landing at an airstrip (Flan's Plane mod)

## Configuration
RadioBeacon is highly configurable for a variety of applications. 
For a full list of configuration options, see [Configuration](http://dev.bukkit.org/server-mods/radiobeacon/pages/configuration/).

## Permissions and Commands
Permission nodes:

* radiobeacon.create (true): Allows you to build new radio beacon antenna towers

* radiobeacon.addmessage (true): Allows you to add messages to towers by placing signs

* radiobeacon.reveal (op): Allows you to list the coordinates of all antennas

* radiobeacon.admin (op): Allows you to save/load/repair antennas

RadioBeacon adds two new commands: /toggleradio for disabling/enabling per-player portable
compass radios, and /antennas for fixed radio tower antennas. Players can use /antennas to see the total number of
antennas on the server, but no other information. Ops (by default) can see detailed information.

Subcommands:

* /antennas save: Force saving antennas to disk (antennas.yml)
* /antennas load: Force reload from disk
* /antennas check: Compare all antennas against the actual blocks in the world, rebuilding the internal state if necessary. Should not be necessary during normal gameplay, but if blocks are updated without RadioBeacon receiving any events (for example, by editing the world in MCEdit), admins can use this command to repair any affected antennas.

## See Also
Servers running RadioBeacon:

* [Solitude at Blocktopia](http://blocktopia.net/forum/forums/solitude.137/) (defunct)
* [Survivocolypse](http://www.survivocolypse.com/)
* [Mortuus Terra](http://mortuusterra.com/)
* [ExpHC](http://www.reddit.com/r/cakesminerapocalypse/comments/qfh0v/exphc_new_experimental_hardcore_apocalypse_server/)
* [Do What Thou Wilt](http://forums.bukkit.org/threads/do-what-thou-wilt.59867/)

Got a server running this plugin? Feel free to post about it below. Also check out
[Top HCSMP](http://tophcsmp.com/index.php?cat=APOC) and [this list](http://www.reddit.com/r/cakesminerapocalypse/comments/q0hh2/list_of_apocalypsehardcore_servers/) for other apocalypse servers,
which may or may not be running RadioBeacon or related plugins.

Reviews of RadioBeacon:

* [Plugin Showcase - RadioBeacon - WoopaGaming](http://www.youtube.com/watch?v=s4jUOxoJmNg) 


Other plugins:

* [Cake's Miner Apocalypse](http://dev.bukkit.org/server-mods/cakes-miner-apocalypse/) - a significant inspiration for RadioBeacon, with a somewhat different realization of radios; also includes many other apocalypse-related features

* [ApocalypseCraft](http://forums.bukkit.org/threads/wgen-rpg-mech-apocalypsecraft-instances-radiation-realism-perks-factions-and-more.23197/) - ([notes](http://www.reddit.com/r/minerapocalypse/comments/oyhpq/anyone_else_see_this_plugin_apocalypsecraft/))

* [RealisticChat](http://dev.bukkit.org/server-mods/realisticchat) - walkie-talkies

***[Fork me on GitHub](https://github.com/mushroomhostage/RadioBeacon)***
