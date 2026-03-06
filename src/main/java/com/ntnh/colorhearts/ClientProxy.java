package com.ntnh.colorhearts;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    private HealthBarRenderer healthBarRenderer;

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event); // Call the parent method if CommonProxy has logic
        healthBarRenderer = new HealthBarRenderer();
        MinecraftForge.EVENT_BUS.register(healthBarRenderer); // Register the event handler
    }
}
