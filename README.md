# CozyCrazyZones

Forge 1.20.1 world-progression substrate for CozyCrazyCraft.

## Two-axis geography

Distance is horizontal Euclidean distance from the Overworld's actual shared spawn point.

**Radial danger**

- **Hearthlands** — `< 2500`
- **Frontier** — `2500–5499`
- **Wildlands** — `5500–8999`
- **Dread Reaches** — `9000+`

**Cardinal ecology**

- **North** — frozen / alpine
- **East** — lush / jungle / overgrowth
- **South** — warm / savanna / desert / badlands
- **West** — temperate / autumn / redwood / old forest

Cardinal borders are large seed-dependent warped sectors, not straight X=0/Z=0 quadrant seams.

### Hearthlands internal ecology

Hearthlands remains one radial danger tier, but its ecological identity ramps in deliberately:

- **0–~700:** Shared Core — ordinary/common temperate starter countryside; no strong cardinal identity.
- **~700–1,200:** Cardinal Transition — directional ecology emerges organically.
- **~1,200–2,500:** Cardinal Hearthlands Proper — clearly Northern/Eastern/Southern/Western while remaining Tier 1.
- **2,500+:** Frontier — regional identity is established and danger escalates.

See `docs/GEOGRAPHY.md` for the authoritative geography contract.

## v0.2 scope

- authoritative `CozyZonesApi.regionalCellAt(...)` classifier
- radial danger + cardinal macro-region + influence band
- seed-dependent warped macro-region borders
- distance/cardinal-gated structure generation
- **NATURAL** mob regionalization only; raid/event/spawner/summon/scripted spawns remain exempt
- region-entry title/subtitle/sound
- persistent atlas-aware HUD showing shared core, transition, or full regional cell
- `/cozyzones where` debug output for both geography axes
- registry dump including structures, entities, **biomes, biome tags, and final natural spawn pools**
- public geography/rule API for future maps, rumors, bounties and quests
- Cataclysm world-content firewall: Cursed Pyramid allowed only in Southern Dread Reaches; unrelated Cataclysm world structures and NATURAL spawns suppressed

Quest logic and the final biome-remapping implementation are deliberately out of scope for this revision.

## Build target

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Gradle 8.8

GitHub Actions builds the reobfuscated mod jar and packages a versioned `ROOT_OVERLAY` artifact containing `mods/CozyCrazyZones-<version>.jar`.

## Testing rule

Do not treat a green compile as proof that worldgen is correct. Test fresh chunks and run:

```
/cozyzones where
/cozyzones dump_registry
```

For v0.2, especially sample around ~0, ~900, ~1,400, ~2,500, ~5,500, and ~9,000 blocks in multiple cardinal directions. The registry dump is written to `logs/cozycrazyzones-registry-dump.txt`; its new biome/spawn section is the input for the next biome-regionality implementation pass.
