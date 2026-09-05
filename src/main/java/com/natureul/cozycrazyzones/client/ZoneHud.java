package com.natureul.cozycrazyzones.client;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import com.natureul.cozycrazyzones.CozyZonesConfig;
import com.natureul.cozycrazyzones.MacroRegion;
import com.natureul.cozycrazyzones.Region;
import com.natureul.cozycrazyzones.RegionalInfluenceBand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ZoneHud {
    private static final ResourceLocation ATLAS_ID = new ResourceLocation("map_atlases", "atlas");

    private ZoneHud() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("region_badge", OVERLAY);
    }

    private static final IGuiOverlay OVERLAY = (ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        Region region = ClientRegionState.region();
        MacroRegion macro = ClientRegionState.macroRegion();
        RegionalInfluenceBand influence = ClientRegionState.influenceBand();
        if (region == null || influence == null || !shouldRender(minecraft)) return;

        String text = badgeText(region, macro, influence);
        int textWidth = minecraft.font.width(text);
        int x = (width - textWidth) / 2;
        int y = height - 61;

        graphics.fill(x - 6, y - 3, x + textWidth + 6, y + 11, 0x76000000);
        graphics.drawString(minecraft.font, text, x, y, region.formatting().getColor() == null ? 0xFFFFFF : region.formatting().getColor(), true);
    };

    static String badgeText(Region region, MacroRegion macro, RegionalInfluenceBand influence) {
        if (influence == RegionalInfluenceBand.SHARED_CORE || macro == null) {
            return "◆ " + region.displayName().toUpperCase();
        }
        if (influence == RegionalInfluenceBand.CARDINAL_TRANSITION) {
            return "◆ " + region.displayName().toUpperCase() + " • " + macro.displayName().toUpperCase() + " TRANSITION";
        }
        return "◆ " + macro.adjective().toUpperCase() + " " + region.displayName().toUpperCase();
    }

    private static boolean shouldRender(Minecraft minecraft) {
        return switch (CozyZonesConfig.HUD_MODE.get()) {
            case ALWAYS -> true;
            case OFF -> false;
            case ATLAS_OWNED -> ownsAtlas(minecraft);
        };
    }

    private static boolean ownsAtlas(Minecraft minecraft) {
        Item atlas = ForgeRegistries.ITEMS.getValue(ATLAS_ID);
        if (atlas == null || minecraft.player == null) return false;
        return minecraft.player.getInventory().contains(new ItemStack(atlas));
    }
}
