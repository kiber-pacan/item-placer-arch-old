package com.akicater.fabric;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.fabricmc.api.ModInitializer;

import com.akicater.ItemPlacer;

public final class ItemPlacerModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemPlacer.initialize();
    }
}
