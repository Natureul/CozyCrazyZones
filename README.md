# CozyCrazyZones

Forge 1.20.1 world-progression substrate for CozyCrazyCraft.

## Region model

Distance is horizontal Euclidean distance from the Overworld's actual shared spawn point.

- **Hearthlands** — `< 2500`
- **Frontier** — `2500–5499`
- **Wildlands** — `5500–8999`
- **Dread Reaches** — `9000+`

Rules are cumulative: a Frontier-gated structure can also appear in Wildlands and Dread Reaches; it is only rejected inside Hearthlands.

## v0.1 scope

- authoritative `CozyZonesApi.regionAt(...)` classifier
- distance-gated structure generation
- distance-gated **NATURAL** mob spawning only
- raid/event/spawner/summoned mob firewall
- region-entry title/subtitle/sound
- persistent current-region HUD indicator, atlas-aware by default
- registry dump and runtime rule validation
- public rule/region API for future maps, rumors, bounties and quests

Quest logic itself is deliberately out of scope for this repository revision.

## Build target

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Gradle 8.8

GitHub Actions builds the reobfuscated mod jar and packages a `ROOT_OVERLAY` artifact containing `mods/CozyCrazyZones-<version>.jar`.

## Testing rule

Do not treat a green compile as proof that worldgen is correct. Before installing into the long-term world, test fresh chunks on both sides of each radial boundary and run:

```
/cozyzones dump_registry
/cozyzones where
```

The registry dump is written to `logs/cozycrazyzones-registry-dump.txt` and is used to close any remaining VERIFY entries.
