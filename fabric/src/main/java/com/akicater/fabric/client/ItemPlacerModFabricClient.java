package com.akicater.fabric.client;

import com.akicater.ItemPlacer;
import com.akicater.layingItemBER;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class ItemPlacerModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ItemPlacer.LAYING_ITEM_BLOCK_ENTITY.get(), layingItemBER::new);
    }
}
