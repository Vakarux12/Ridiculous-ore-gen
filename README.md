# Ridiculous Ore Generation

> A NeoForge 1.21.1 port of the [Ridiculous Ore Generation](https://modrinth.com/mod/rediculous-ore-generation) / [BOG](https://www.curseforge.com/minecraft/mc-mods/bog) mod.

Recommended for those who likes to play modded but has no time to do so or wants to progress faster

I'd also recommend adding veinminer lol

---

## What it does

Multiplies every ore vein in the game (editable in config). Default is **20x** meaning 20 times more iron, diamond, coal and so on and any ore added by other mods.

Already generated chunks can be retrofitted with two commands if you decided to install this mod later.

---

## Config

Config is located at:
```
config/ridiculousoregen-common.toml
```

```toml
# How many times more ore veins to generate compared to vanilla.
# 1 = vanilla, 20 = twenty times more (default), max 100.
oreMultiplier = 20

# Maximum chunk radius allowed for the /ridiculousores populate command.
retrofitMaxRadius = 32
```

Use `/ridiculousores populate` to retrofit existing chunks.

---

## Commands

Requires operator / permission level 2.

### `/ridiculousores fill <x> <y> <z>`
1. Set the coordinates to a ore which links to a vein.
2. Calculates how many blocks are needed to reach `vein_size x multiplier`
3. Places the extra ore blocks around the vein


### `/ridiculousores populate <radius>`
Re-runs ore generation `(multiplier - 1)` within `<radius>` chunks of you. Use this to retrofit an existing world IF you installed the mod AFTER generating the world.

```
/ridiculousores populate 5  (5-chunk radius, around 121 chunks)
/ridiculousores populate 16  (16-chunk radius, around 1089 chunks)
```
> [!NOTE]
> Large radius on a slow server may cause a brief lag spike.

---

## Mod compatibility

The biome modifier runs in `Phase.MODIFY`, after all other mods have finished their `Phase.ADD`. This means **any ore added by another mod will also be multiplied** (yay!).

---

## Credit

Honestly, this is a straight up copy of the mods done by the creators of:

- [Ridiculous Ore Generation](https://modrinth.com/mod/rediculous-ore-generation) on Modrinth
- [BOG (Better ore Generation)](https://www.curseforge.com/minecraft/mc-mods/bog) on CurseForge

I really wanted this mod updated for NeoForge 1.21.1 and the original hadn't been, so I remade it. All credit goes to them for the original concept and implementation. Thanks.

---

Also LLM assisted for GitHub actions (I ain't learning that)

## License

MIT
