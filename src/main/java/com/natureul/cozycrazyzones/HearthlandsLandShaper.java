package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Physical companion to RegionalBiomePostProcessor.
 *
 * This pass is deliberately narrow: it may only alter a 4x4 biome cell when the BIOMES-stage
 * postprocessor proved that the cell was native ocean and CozyCrazyZones intentionally converted
 * it to land. Ordinary rivers, lakes, ravines, wetlands, beaches and land-biome aquifers are never
 * candidates anymore.
 *
 * The target elevation is continuous with the same broad ocean policy used by biome conversion.
 * Near a retained-ocean edge the former basin becomes shallow shelf; farther inland it rises above
 * sea level. That removes the old binary sea-floor -> plateau walls.
 */
public final class HearthlandsLandShaper {
    private HearthlandsLandShaper() {}

    public static void shape(ChunkAccess chunk, int seaLevel) {
        if (!WorldGeographyContext.prepared()) return;

        boolean[] convertedOcean = RegionalBiomePostProcessor.takeConvertedOceanMask(chunk);
        if (convertedOcean == null) return;

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int blockX = minX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int biomeCell = (localZ >> 2) * 4 + (localX >> 2);
                if (!convertedOcean[biomeCell]) continue;

                int blockZ = minZ + localZ;
                RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);
                if (cell.radialZone() != Region.HEARTHLANDS) continue;

                double strength = HearthlandsOceanPolicy.convertedLandStrength(
                        cell,
                        WorldGeographyContext.worldSeed(),
                        blockX,
                        blockZ
                );
                if (strength <= 0.015D) continue;

                pos.set(blockX, seaLevel - 1, blockZ);
                if (!chunk.getBlockState(pos).is(Blocks.WATER)) continue;

                int floorY = seaLevel - 1;
                while (floorY > chunk.getMinBuildHeight()) {
                    pos.setY(floorY);
                    if (!chunk.getBlockState(pos).is(Blocks.WATER)) break;
                    floorY--;
                }

                int waterDepth = (seaLevel - 1) - floorY;
                if (waterDepth < 3) continue;

                // Broad, low-amplitude relief prevents a featureless reclaimed-ocean plate while
                // keeping adjacent columns close enough in elevation to read as terrain, not walls.
                double rolling = RegionalNoise.fractal(
                        WorldGeographyContext.worldSeed() ^ 0xA24BAED4963EE407L,
                        blockX,
                        blockZ,
                        420.0D
                );
                double relative = -2.0D + strength * 6.0D + rolling * 1.15D * strength;
                int targetTopY = seaLevel + (int) Math.round(relative);
                targetTopY = Math.max(seaLevel - 2, Math.min(seaLevel + 5, targetTopY));
                if (targetTopY <= floorY) continue;

                for (int y = floorY + 1; y <= targetTopY; y++) {
                    pos.set(blockX, y, blockZ);
                    // At this generation stage the basin above the floor should be water/air.
                    // Refuse to bulldoze any unexpected solid state from another generator/mod.
                    if (chunk.getBlockState(pos).is(Blocks.WATER) || chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}
