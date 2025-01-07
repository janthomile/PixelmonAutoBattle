# Pixelmon Auto Battle
Brings Let's Go Autobattling into Pixelmon! (Reforged 1.16.5)

## Features
* Toggle Auto Battle by sending out a Pokemon and pressing your 'Select Target Pokemon' key while sneaking (default key is V).
* Autobattling Pokemon will seek out nearby wild Pokemon and engage them in a short sequence which will end in victory or defeat, depending largely on level difference and type matching.
* Autobattling Pokemon will ignore Legendary, Shiny, and Boss wild Pokemon.
* EXP Share and EXP All are taken into account when distributing XP.
### Configurables
* Defeated Pixelmon will have their dropped EXP and Items sent to the Autobattling Pokemon's owner.
* EXP gain multiplier from Autobattling (Halved by default).
* The difference in levels needed to overwhelm wild pokemon (25 by default).
* The amount of damage Autobattling Pokemon receive during a Draw, Defeat, or Pyrrhic Victory.
* Whether Pokemon can levelup from Autobattling XP.
* Whether Pokemon can faint from damage received during Autobattling.
* Make hitting a Pokemon target them, the same way as pressing "V" (Target Pokemon keybind).

## Commands:
* `/autobattle toggle` acts as an alternative to the Sneak+Key method described above. (No permission requirement)
* `/autobattle set <player> <off>` disables autobattle for any of the player's current Pokemon. (Operator permission requirement)
* `/autobattle set <player> <on> <forced>`enables autobattle for the player's currently selected Pokemon. The forced option determines whether the Pokemon will be sent out if it isn't already. (Operator permission requirement)

## Permission Nodes:
* `minecraft.command.autobattle`must be given as a baseline.
* `pixelmonautobattle.toggle` only has an effect when set to `false`, otherwise players can use it freely.
* `pixelmonautobattle.set` is off by default, and generally is for moderators or admins.