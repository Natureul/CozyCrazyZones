# Biome Regionality — v0.3

Source of truth: full CozyCrazyCraft registry dump captured 2026-09-05 after biome modifiers.

## Player-facing macro-region names

- `NORTH` (internal coordinate identity) → **Frostmarch**
- `EAST` → **Greenveil**
- `SOUTH` → **Sunscar**
- `WEST` → **Amberwood**

The internal cardinal enum values and IDs are intentionally stable for integrations. Player-facing text uses place names.

## Hearthlands bands

- `0–700`: Shared Core. Ordinary/common countryside. No required macro-region identity.
- `700–1200`: Cardinal transition. Mild regional ecology begins to appear.
- `1200–2500`: Established regional Hearthlands. Clear identity, still habitable.
- `2500+`: Frontier and beyond progressively strengthen the regional biome palette.

## Managed surface-biome families

### Shared/Common
Vanilla plains, sunflower plains, meadow, forest, birch/old-growth birch, flower forest, windswept forest/hills,
beach/stony shore, plus BOP grassland, highland, lavender field, pasture, prairie, rocky shrubland, shrubland,
aspen glade, forested field, woodland, crag, gravel beach and Origin Valley.

### Frostmarch
Taiga, old-growth spruce/pine, snowy taiga, grove, snowy plains/slopes, jagged/frozen peaks, ice spikes,
snowy beach, frozen river, plus BOP coniferous forest, fir clearing, field, bog, snowblossom grove,
snowy coniferous/fir/maple, tundra, muskeg, hot springs, cold desert, Auroral Garden and Wintry Origin Valley.

### Greenveil
Sparse jungle, jungle, bamboo jungle, swamp, mangrove swamp, plus BOP jacaranda glade, orchard,
overgrown greens, marsh, rainforest, bayou, floodplain, wetland, jade cliffs, rocky rainforest,
fungal jungle and tropics.

### Sunscar
Savanna, savanna plateau, windswept savanna, desert, badlands/wooded/eroded badlands, plus BOP
lush savanna, scrubland, Mediterranean forest, dryland, lush desert, dune beach, wasteland,
wasteland steppe, volcano and volcanic plains.

### Amberwood
Cherry grove and dark forest, plus BOP maple woods, seasonal forest, pumpkin patch, redwood forest,
old-growth woodland, dead forest, old-growth dead forest, ominous woods and Mystic Grove.

### Water
Rivers remain connective geography. Ocean temperature is regionalized farther out:
Frostmarch → cold/frozen; Greenveil → lukewarm/warm; Sunscar → warm; Amberwood → temperate.

## Ocean policy

Hearthlands is intentionally land-dominant.

Biome remapping alone cannot physically remove an ocean basin in modern Minecraft, because biome identity and
terrain density are separate. v0.3 therefore uses two cooperating layers:

1. A low-frequency coherent mask decides which Hearthlands ocean sectors survive as actual bays/coasts.
2. Ocean columns whose remapped surface biome is land are gently raised before surface building.

Rules:
- no intentionally retained broad ocean inside the first ~800 blocks;
- rivers, wetlands, coasts and shallow ponds remain;
- retained ocean probability rises gradually toward the 2,500-block Frontier boundary;
- raised columns become low rolling land, not a flat sea-level plate;
- ocean freedom returns outside Hearthlands, including frozen outer Frostmarch for Aquamirae/Cornelia.

## Worldgen method

The remapper runs at `MultiNoiseBiomeSource#getNoiseBiome` return. It preserves the original large native
biome-shape tessellation and substitutes a compatible biome holder. It does **not** add per-block random biome
noise, so there is no checkerboard geography.

A second `NoiseBasedChunkGenerator#fillFromNoise` hook performs the Hearthlands land-bias pass before surface
building, allowing normal vanilla/BOP surface rules to dress the raised terrain.

Cave biomes and non-managed Nether/End/special biomes are not remapped.
