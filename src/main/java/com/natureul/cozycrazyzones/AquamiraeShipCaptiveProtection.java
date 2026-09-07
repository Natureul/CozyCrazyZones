package com.natureul.cozycrazyzones;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Keeps the Aquamirae ship's authored prison intact until the player chooses to intervene.
 *
 * The structure audit found three normal villagers imprisoned alongside pillagers/vindicators, and
 * cross-mod block-breaking AI can otherwise let the captors destroy their own cage and kill the
 * prisoners before the player meaningfully encounters the rescue. We therefore deny mob-griefing
 * checks for those captor types while the captor is physically inside an Aquamirae ship.
 *
 * This does not make the villagers invulnerable and does not disable raider combat. If the player
 * opens/breaks the prison, the rescue becomes live normally; the captors simply cannot pre-break the
 * authored ship themselves.
 */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AquamiraeShipCaptiveProtection {
    private static final ResourceLocation SHIP = new ResourceLocation("aquamirae", "ship");
    private static final Set<ResourceLocation> CAPTOR_TYPES = Set.of(
            new ResourceLocation("minecraft", "pillager"),
            new ResourceLocation("minecraft", "vindicator"),
            new ResourceLocation("aquamirae", "pillagers_patrol")
    );

    private AquamiraeShipCaptiveProtection() {}

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null || !CAPTOR_TYPES.contains(entityId)) return;
        if (!insideAquamiraeShip(level, entity)) return;

        event.setResult(Event.Result.DENY);
    }

    private static boolean insideAquamiraeShip(ServerLevel level, Entity entity) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SHIP);
        if (structure == null) return false;
        StructureStart start = level.structureManager().getStructureAt(entity.blockPosition(), structure);
        return start != null && start.isValid() && start.getBoundingBox().isInside(entity.blockPosition());
    }
}
