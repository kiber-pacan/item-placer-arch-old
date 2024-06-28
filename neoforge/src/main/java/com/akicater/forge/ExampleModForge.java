package com.akicater.forge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import com.akicater.ItemPlacer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(ItemPlacer.MODID)
public final class ExampleModForge {
    public ExampleModForge() {

        // Run our common setup.
        ItemPlacer.initialize();
    }

}

