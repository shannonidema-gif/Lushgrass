package com.example.lushgrass;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class LushGrassClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(GrassField::tick);
        LevelRenderEvents.AFTER_ENTITIES.register(GrassRenderer::render);
    }
}
