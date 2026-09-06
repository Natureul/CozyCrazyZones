package com.natureul.cozycrazyzones.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.Map;

/** Internal hooks used by CozyCrazyZones-authored map markers. */
@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
    @Mutable
    @Accessor("centerX")
    void cozyzones$setCenterX(int value);

    @Mutable
    @Accessor("centerZ")
    void cozyzones$setCenterZ(int value);

    @Accessor("decorations")
    Map<String, MapDecoration> cozyzones$getDecorations();

    @Invoker("addDecoration")
    void cozyzones$addNamedDecoration(MapDecoration.Type type,
                                      @Nullable LevelAccessor level,
                                      String id,
                                      double x,
                                      double z,
                                      double rotation,
                                      @Nullable Component name);
}
