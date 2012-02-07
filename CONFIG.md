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

**fixedBaseMinY**: Require antennas to be built above this Y level. 0 for anywhere, 64 for
sea level, etc

**fixedBaseMaterial**: Material to use for tower base. Must be a 
[recognized Bukkit Material name](http://jd.bukkit.org/apidocs/org/bukkit/Material.html),
such as "iron_block". You can change this value to make antennas easier or more difficult
to acquire. Iron blocks only require 9 ingots, fairly easy to obtain; other blocks to
consider: jukebox (more difficult, requires 1 diamond + 8 planks), gold block
(somewhat more difficult), diamond block (very expensive), or non-obtainable blocks to
completely control who can build antennas.

**fixedAntennaMaterial**: Material to use for antenna. Example: "iron_fence" for the
recognizable iron bars.

**fixedRadiateFrom**: Either "tip" or "base". Since the transmissions cover a 3D sphere,
transmitting from the tip adds a tradeoff in building higher antennas.

**fixedUnpoweredNagMessage**: Message to send to players who place unpowered iron blocks,
possibly a failed attempt at creating an antenna (must place redstone *before* the
iron block). By default, sends a helpful tip about how to create an antenna. Set to an 
empty string or null to disable if it becomes annoying.



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

**fixedBlastSetFire**: Whether blasts should set exploded blocks aflame or not. Recommended
true since the explosion is electrical in nature.

Explosions, whether from lightning or entities (TNT, creepers), also affects antennas:

**fixedExplosionReactionDelayTicks**: The delay, in ticks (1/20 second) after the explosion
before the antenna updates itself.


## Mobile Compass Radios
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

**mobileTaskStartDelaySeconds**: Delay after the server starts up before beginning the mobile radio
scanning task.

**mobileTaskPeriodSeconds**: Period of the scanning task. Every so many seconds, players holding
compasses will receive reports of signals within range. Set lower to make compasses receive faster,
higher to reduce server load.

**mobileTaskSync**: Whether to run the task on the main thread. Required for thread-safety.

