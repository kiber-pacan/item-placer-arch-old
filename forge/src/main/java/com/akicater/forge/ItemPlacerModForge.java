package com.akicater.forge;

import com.akicater.ItemPlacer;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ItemPlacer.MODID)
public final class ItemPlacerModForge {
    public ItemPlacerModForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ItemPlacer.MODID, FMLJavaModLoadingContext.get().getModEventBus());


        // Run our common setup.
        ItemPlacer.initialize();
    }
}

