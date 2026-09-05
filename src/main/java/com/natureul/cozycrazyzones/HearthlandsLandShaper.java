package com.natureul.cozycrazyzones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.biome.Biome;

/**
 * Gentle Hearthlands land bias.
 *
 * Modern biome replacement alone does not raise an ocean basin because terrain density and biome
 * identity are separate. This pass only acts on columns that are still water at sea level after
 * noise fill, and only when the remapped surface biome is land. Rivers, wetlands, coasts and the
 * intentionally retained ocean mask are left alone.
 *
 * It runs before surface building, so vanilla/BOP surface rules can still dress the raised stone
 * into grass, dirt, sand, etc.
 */
public final class HearthlandsLandShaper {
    private HearthlandsLandShaper() {}

    public static void shape(ChunkAccess chunk, int seaLevel) {
        if (!WorldGeographyContext.prepared()) return;

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int blockX = minX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = minZ + localZ;
                RegionalCell cell = WorldGeographyContext.cellAt(blockX, blockZ);
                if (cell.radialZone() != Region.HEARTHLANDS) continue;

                Holder<Biome> biomeHolder = chunk.getNoiseBiome(
                        QuartPos.fromBlock(blockX),
                        QuartPos.fromBlock(seaLevel),
                        QuartPos.fromBlock(blockZ)
                );
                ResourceLocation biomeId = biomeHolder.unwrapKey().map(key -> key.location()).orElse(null);
                if (biomeId == null || !BiomeRegionality.shouldUpliftHearthlandsWater(cell, biomeId)) continue;

                pos.set(blockX, seaLevel - 1, blockZ);
                if (!chunk.getBlockState(pos).is(Blocks.WATER)) continue;

                int floorY = seaLevel - 1;
                while (floorY > chunk.getMinBuildHeight()) {
                    pos.setY(floorY);
                    if (!chunk.getBlockState(pos).is(Blocks.WATER)) break;
                    floorY--;
                }

                int waterDepth = (seaLevel - 1) - floorY;
                if (waterDepth < 4) continue;

                double rolling = RegionalNoise.fractal(
                        WorldGeographyContext.worldSeed() ^ 0xA24BAED4963EE407L,
                        blockX,
                        blockZ,
                        360.0D
                );
                int rise = 1 + (int) Math.round((rolling + 1.0D) * 2.0D);
                int topY = seaLevel + Math.max(1, Math.min(5, rise));

                for (int y = floorY + 1; y <= topY; y++) {
                    pos.set(blockX, y, blockZ);
                    chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                }
            }
        }
    }
}
