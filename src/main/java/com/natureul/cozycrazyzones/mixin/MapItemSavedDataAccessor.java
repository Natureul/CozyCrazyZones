package com.natureul.cozycrazyzones.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the shared starter-house guide use an exact midpoint instead of vanilla's coarse map grid. */
@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
    @Mutable
    @Accessor("centerX")
    void cozyzones$setCenterX(int value);

    @Mutable
    @Accessor("centerZ")
    void cozyzones$setCenterZ(int value);
}
