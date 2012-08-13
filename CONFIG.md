# RadioBeacon Configuration

config.yml is created on first launch, populated with reasonable defaults. However,
it is recommended you tweak the settings as appropriate for your server.

## Fixed Antenna Towers
Signals emanate in a 3D spherical radius from each antenna. 

**fixedInitialRadius**: The radius, in meters, of newly-built antennas consisting of a lone iron block
with no iron fences above it.

**fixedRadiusIncreasePerBlock**: The radius increase for each iron fence added above the iron block.

**fixedMaxHeightMeters**: The highest effective height of antenna towers. Antennas can be built higher,
but the additional iron fences will have no effect on the radius. Set to 0 for no limit.

**fixedRadiusStormFactor**: Multiply the radius by this amount during a storm. Set >1.0 
to amplify, <1.0 to attenuate.

**fixedRadiusThunderFactor**: Multiply radius when thundering.

**fixedRadiusRelayFactor**: Multiply radius when antenna is a relay.

**fixedReceptionRadiusDivisor**: Divide broadcast radius by this value to obtain the
reception radius. If 1, reception radius is equal to broadcast radius, if 0,
reception radius is fixed to 0 (special case -- antenna must be directly within
broadcast range of other antenna), other integer values greater than 1 will result
in a lower reception radius (asymmetric broadcast/reception), e.g. 2 for a reception
radius half of the broadcast radius.

**fixedBaseMinY**: Require antennas to be built above this Y level. 0 for anywhere, 64 for
sea level, etc

**fixedBaseMaterial**: Material to use for tower base. Must be a 
[recognized Bukkit Material name](http://jd.bukkit.org/apidocs/org/bukkit/Material.html),
such as "iron_block". You can change this value to make antennas easier or more difficult
to acquire. Iron blocks only require 9 ingots, fairly easy to obtain; other blocks to
consider: jukebox (more difficult, requires 1 diamond + 8 planks), gold block
(somewhat more difficult), diamond block (very expensive), or non-obtainable blocks to
completely control who can build antennas.

**fixedBaseRelayMaterial**: Material to use for tower base of relays. Example: "gold_block".

**fixedAntennaMaterial**: Material to use for antenna. Example: "iron_fence" for the
recognizable iron bars.

**fixedRadiateFrom**: Either "tip" or "base". Since the transmissions cover a 3D sphere,
transmitting from the tip adds a tradeoff in building higher antennas.

**fixedUnpoweredNagMessage**: Message to send to players who place unpowered iron blocks,
possibly a failed attempt at creating an antenna (must place redstone *before* the
iron block). By default, sends a helpful tip about how to create an antenna. Set to an 
empty string or null to disable if it becomes annoying.

**fixedDenyCreateMessage**: Message to send to players who do not have permission
to build radio towers (lacking the "radiobeacon.create" permission node).

**fixedDenyAddMessageBreak**: Whether to break the sign block, returning it to the player,
if they attempt to place a sign on a tower they are not allowed to set the message for.

**fixedDenyAddMessageMessage**: Message to send to players who do not have permission
to add transmission messages to existing radio towers (lacking "radiobeacon.addmessage"
permission node).




Lightning probability can be tuned by adjusting the "attraction radius":

**fixedLightningAttractRadiusInitial**: The lightning attraction radius for newly-built antennas.
Lightning strikes within this 2D distance of the antenna x/z will be attracted. For comparison,
the attraction radius of 
[creepers](http://www.minecraftwiki.net/wiki/Charged_creeper#Charged_Creepers)
is 3-4 blocks.

**fixedLightningAttractRadiusIncreasePerBlock**: For each iron fence added, increase the lightning
attraction radius by this amount. Floating-point.

**fixedLightningAttractRadiusMax**: The maximum attraction radius. Building higher will have no
effect. 

**fixedLightningDamage**: Whether to cause real damage on attracted lightning strikes, or
otherwise to only generate non-damaging lightning strike effects. 

**fixedLightningStrikeOne**: Whether lightning within the attraction radius of multiple antennas
should only strike one antenna (the tallest), or otherwise all simultaneously. When enabled,
allows "lightning rod" antennas to be built attracting lightning away from other antennas,
but prevents double/triple/etc. lightning strikes.

**fixedWeatherListener**: Whether to listen for lightning events.

Lightning causes a varying amount of damage, controlled by:

**fixedBlastPowerInitial**: Power of the explosive blast, centered on the antenna base,
when struck by lightning. For comparison: ghast fireball=1, creeper=3, TNT=4, charged creeper=6.
Note that very high values may cause excessive lag or damage and are not recommended.

**fixedBlastPowerIncreasePerBlock**: The blast power increase for each iron fence. Floating-point.

**fixedBlastPowerMax**: The maximum blast power. Building higher will have no effect.
For reference, 6.0 is a the same power as a charged creeper.

**fixedBlastSetFire**: Whether blasts should set exploded blocks aflame or not. Recommended
true since the explosion is electrical in nature.

Explosions, whether from lightning or entities (TNT, creepers), also affects antennas:

**fixedExplosionReactionDelayTicks**: The delay, in ticks (1/20 second) after the explosion
before the antenna updates itself.


## Mobile Compass Radios
**mobileRadioItem**: The [item ID](http://www.minecraftwiki.net/wiki/Data_values) used for
mobile radios. Defaults to a compass (345).

**mobileInitialRadius**: The "reception radius" in meters when you hold a single compass.
0 means you must be directly within the spherical radius of the transmitting antenna, anything higher
allows you to receive signals when further away (beyond the normal radius). 

**mobileIncreaseRadius**: Reception radius increase for each extra compass held in a stack
by the player. 

**mobileMaxRadius**: The highest reception radius allowed. Note that players can stack compasses
up to 64. If the range is too high you can cap it with this option.

**mobileRadiusStormFactor**: Multiply reception radius by this floating-point number during a storm,
just like fixedRadiusStormFactor but for compasses.

**mobileRadiusThunderFactor**: Same as fixedRadiusThunderFactor but for compasses.

**mobileScanBonusRadius**: If non-zero, increase reception radius by this amount each time the
player is holding the compass during the scan period. If the player switches items, the increase
is reset. This is meant to provide a "bonus" for players holding onto their compasses for
longer periods of time.

**mobileScanBonusMaxRadius**: The maximum scan bonus range. After it reaches this amount,
the range will not increase further no matter how long the player holds onto their compass.

**mobileTaskStartDelaySeconds**: Delay after the server starts up before beginning the mobile radio
scanning task.

**mobileTaskPeriodSeconds**: Period of the scanning task. Every so many seconds, players holding
compasses will receive reports of signals within range. Set lower to make compasses receive faster,
higher to reduce server load.

**mobileRightClickTuneDown**, **mobileLeftClickTuneUp**: Whether to allow right-clicked compasses to tune the radio down,
and/or left-clicked compasses to tune the radio up.
You can disable either one if you want other plugins (such as [Cakes Miner Apocalypse](http://dev.bukkit.org/server-mods/cakes-miner-apocalypse/))
to exclusively react to the clicks. The tuning automatically wraps around as needed, so only up or down
is required. At least one should be enabled.

**mobileShiftTune**: If true, require shift-clicking (sneaking) to tune the compass. 

**mobileSetCompassTarget**: Set to false to disable setting the compass targetting the locked-on signal. Requires mobileSignalLock.

**mobileSignalLock**: Set to false to disable locking onto nearby signals. When disabled, you'll still be able to receive signals
with a compass, but their distance and the "locked on" message will no longer be shown.

**mobileNoSignalsMessage**: Message to display when the periodic scan completes but no radio beacon signals
are picked up. "%d" is replaced with the reception radius. Set to null or an empty string to disable this message.


## Debugging
**verbose**: If true, detailed information will be logged using log.info(). 
