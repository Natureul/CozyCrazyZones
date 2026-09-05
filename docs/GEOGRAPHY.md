# CozyCrazyZones Geography Contract

CozyCrazyZones has two independent geographic axes.

## 1. Radial danger

Measured as horizontal Euclidean distance from the actual Overworld shared spawn:

- Hearthlands: < 2500
- Frontier: 2500-5499
- Wildlands: 5500-8999
- Dread Reaches: 9000+

## 2. Cardinal ecology

- North: frozen / alpine
- East: lush / jungle / overgrowth
- South: warm / savanna / desert / badlands
- West: temperate / autumn / redwood / old forest

Cardinal boundaries are not X=0/Z=0 quadrant seams. `CozyZonesApi` uses 90-degree cardinal sectors with deterministic low-frequency seed-dependent angular warping. This produces large coherent regions while making the diagonal borders curved and world-specific.

## Hearthlands internal bands

The radial Hearthlands tier is intentionally subdivided for ecology without changing its danger tier:

- 0-~700: Shared Core. Cardinal strength = 0. Starter countryside should be common, habitable, and broadly temperate.
- ~700-1200: Cardinal Transition. Directional ecology ramps in smoothly.
- ~1200-2500: Cardinal Hearthlands Proper. Regional identity is established while danger remains Tier 1.
- 2500+: Frontier begins; regional identity remains established and danger increases.

These values are configurable (`innerCoreRadius`, `cardinalEstablishedRadius`) and are clamped to remain inside Hearthlands.

## Regional cell API

`CozyZonesApi.regionalCellAt(...)` returns:

- radial zone
- macro region
- influence band
- distance from spawn
- radial cardinal-strength ramp (0..1)
- macro-boundary core strength (0..1)

Future biome generation, mob ecology, structures, cartographers, maps, rumors and bounties should consume this one authoritative answer rather than reimplement coordinates.

## Border behavior

A separate `macroBoundaryStrength` falls to zero on a warped boundary and reaches one deeper inside a cardinal sector. The biome layer should use this to allow compatible/common transition biomes along borders instead of producing hard ecological seams.

## Current hard boss/destination assignments

- North + Wildlands: Frostmaw
- South + Wildlands: Umvuthi / Umvuthana Grove
- West + Wildlands: Sir Pumpkinhead (spawn mechanism still under audit)
- North + Dread Reaches: Ice Maze / Captain Cornelia (worldgen mechanism still under audit)
- East + Dread Reaches: future Jungle Abomination arena
- South + Dread Reaches: Cataclysm Cursed Pyramid / Ancient Remnant
- West + Dread Reaches: Lord Pumpkinhead (spawn mechanism still under audit)

Ferrous Wroughtnaut remains non-cardinal for now.

## Cataclysm policy

When Cataclysm is installed, CozyCrazyZones treats it as a Cursed-Pyramid-only world-content dependency:

- `cataclysm:cursed_pyramid` is allowed only in South + Dread Reaches.
- other `cataclysm:*` registered structures are suppressed by the structure gate.
- all NATURAL `cataclysm:*` entity spawns are suppressed; structure/script/summon spawn types are untouched.

This deliberately leaves Cataclysm registrations and authored pyramid mechanics intact.
