<h1 align="center">ClaimIt-Dynmap</h1>

<h5 align="center">Adds <a href="https://github.com/itsmeow/ClaimIt">ClaimIt</a> integration to <a href="https://www.curseforge.com/minecraft/mc-mods/dynmapforge">DynmapForge</a></h5>

## What does this do?
This addon adds markers on [DynmapForge](https://www.curseforge.com/minecraft/mc-mods/dynmapforge) for all ClaimIt claims.

## Commands

  * `/claimitdynmapreload` - Reloads all ClaimIt-Dynmap markers (can be used to update colors or names). Requires op level 4 or `claimitdynmap.command.claimitdynmapreload` permission node.

## Permissions

  * `claimitdynmap.command.claimitdynmapreload` - Grants permission to use `/claimitdynmapreload`

## Preferences
This addon adds two user configs to ClaimIt than can be used to configure the color of your claims on the Dynmap. These can be configured using `/claimit config (configname) (value)`.

  * `fill_color` (Default: 0x545454): Hex color to fill your claims with on dynmap (if changed applies after server restart)
  * `border_color` (Default: 0x545454): Hex color to outline your claims with on dynmap (if changed applies after server restart)