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

- **North / Frostmarch** — frozen / alpine
- **East / Greenveil** — lush / jungle / overgrowth
- **South / Sunscar** — warm / savanna / desert / badlands
- **West / Harvestwood** — temperate / autumn / pumpkin / redwood / old forest

Cardinal borders are large seed-dependent warped sectors, not straight X=0/Z=0 quadrant seams.

### Hearthlands internal ecology

Hearthlands remains one radial danger tier, but its ecological identity ramps in deliberately:

- **0–~700:** Shared Core — ordinary/common temperate starter countryside; no strong cardinal identity.
- **~700–1,200:** Cardinal Transition — directional ecology emerges organically. Generic forest/plains are increasingly replaced during the latter half of this band.
- **~1,200–2,500:** Cardinal Hearthlands Proper — clearly Frostmarch/Greenveil/Sunscar/Harvestwood while remaining Tier 1.
- **2,500+:** Frontier — regional identity is established and danger escalates.

Warped macro borders retain a narrow neutral/common seam so transitions remain natural rather than becoming hard biome walls.

### Regional finals

Dread Reaches remain open-ended exploration territory, but a regional **final destination is not allowed to drift infinitely outward**. The default final-destination expedition belt is:

- inner edge: **9,000** blocks (Dread Reaches)
- outer edge: **15,000** blocks (`finalDestinationMaxRadius`, configurable)

Current enforced finals:

- **North / Frostmarch:** Aquamirae Ice-Maze territory and its registered Maze structures
- **South / Sunscar:** Cataclysm Cursed Pyramid / Ancient Remnant

Aquamirae is handled at both layers: its registered structures are distance/cardinal gated, while frozen/deep-frozen ocean is reserved as Ice-Maze biome territory only inside the legal northern final belt.

## Current scope (v0.3.11)

- authoritative `CozyZonesApi.regionalCellAt(...)` classifier
- radial danger + cardinal macro-region + influence band
- seed-dependent warped macro-region borders
- strict second-pass cardinal biome identity for COMMON forest/plains residue
- distance/cardinal-gated structure generation
- finite final-destination expedition belt
- Aquamirae Ice-Maze biome/feature territory gating
- **NATURAL** mob regionalization only; raid/event/spawner/summon/scripted spawns remain exempt
- region-entry title/subtitle/sound
- persistent atlas-aware HUD showing shared core, transition, or full regional cell
- starter Atlas plus one real guaranteed Hearthlands village anchor in each cardinal region
- real filled starter-house Hearthlands overview map; automatic item-frame map marker disabled
- starter-land selection biased toward a usable natural continent, including a stronger land-availability preference for early Frostmarch
- `/cozyzones where` debug output for both geography axes
- registry dump including structures, entities, biomes, biome tags, and final natural spawn pools
- public geography/rule API for future maps, rumors, bounties and quests
- Cataclysm world-content firewall: Cursed Pyramid is the retained Cataclysm Overworld destination; unrelated Cataclysm world structures and NATURAL spawns are suppressed
- root-overlay integration files, including a client override that suppresses Alex's Mobs Interaction's repeated Farseer login reminder without disabling the Farseer visual feature itself

Quest logic remains deliberately separate from this worldgen substrate.

## Build target

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Gradle 8.8

GitHub Actions builds the reobfuscated mod jar and packages a versioned `ROOT_OVERLAY` artifact. The overlay contains the CozyCrazyZones jar plus any pack-integration files under `root_overlay/`.

## Testing rule

Do not treat a green compile as proof that worldgen is correct. Test a fresh world/fresh chunks and run:

```
/cozyzones where
/cozyzones dump_registry
```

Sample ~500, ~900, ~1,400, ~2,500, ~5,500, ~9,000, ~12,000 and ~15,500 blocks in all four cardinal regions. In particular verify:

- generic Forest/Plains do not dominate established cardinal country;
- warped cardinal borders still blend rather than forming hard straight seams;
- all four Hearthlands regional village anchors exist on usable land;
- Frostmarch is no longer routinely dominated by a giant early ocean;
- no Aquamirae Ice-Maze territory exists before Dread or beyond the final belt;
- Cursed Pyramid cannot generate beyond the final belt;
- North Dread still contains enough ocean inside the belt for Aquamirae to generate naturally.

The registry dump is written to `logs/cozycrazyzones-registry-dump.txt`.

Current CI verifies compilation and root-overlay packaging. Actual seed distribution still requires an in-game fresh-world pass before the 9k–15k final belt and regional biome strictness should be treated as permanently tuned.
