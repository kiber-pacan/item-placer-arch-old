package com.akicater.forge;

import com.akicater.ItemPlacer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ItemPlacer.MODID, bus = EventBusSubscriber.Bus.MOD)
public class EventHandler {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ItemPlacer.LAYING_ITEM_BLOCK_ENTITY.get(), layingItemBER::new);
    }
}