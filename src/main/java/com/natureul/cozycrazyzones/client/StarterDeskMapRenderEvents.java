package com.natureul.cozycrazyzones.client;

import com.natureul.cozycrazyzones.CozyCrazyZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the authored starter guide visually humble while it is sitting on the desk.
 *
 * The server-side stack remains a real filled map with its four village decorations. We only replace
 * the item-frame render with the ordinary unused-map item model. The moment the player removes it
 * from the frame, vanilla renders/uses the actual filled map again, so no marker data is sacrificed.
 */
@Mod.EventBusSubscriber(modid = CozyCrazyZones.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StarterDeskMapRenderEvents {
    private static final String GUIDE_TAG = "CozyCrazyZonesDeskVillageGuide";
    private static final ItemStack UNUSED_MAP = new ItemStack(Items.MAP);

    private StarterDeskMapRenderEvents() {}

    @SubscribeEvent
    public static void onRenderItemInFrame(RenderItemInFrameEvent event) {
        ItemStack realStack = event.getItemStack();
        CompoundTag tag = realStack.getTag();
        if (tag == null || !tag.getBoolean(GUIDE_TAG)) return;

        ItemFrame frame = event.getItemFrameEntity();
        int light = frame.getType() == EntityType.GLOW_ITEM_FRAME ? 15728880 : event.getPackedLight();

        // RenderItemInFrameEvent fires after the frame-facing translation/rotation has already been
        // applied and before vanilla chooses its filled-map branch. Mimic vanilla's ordinary-item
        // branch exactly, but feed it minecraft:map instead of the real filled-map stack.
        event.getPoseStack().scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                UNUSED_MAP,
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                event.getPoseStack(),
                event.getMultiBufferSource(),
                frame.level(),
                frame.getId()
        );
        event.setCanceled(true);
    }
}
