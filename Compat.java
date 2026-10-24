package com.example.lushgrass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Every Minecraft call whose name/package has changed between 26.x snapshots lives HERE.
 * If the build fails on 26.3, the fix is almost certainly a rename in this one file.
 */
final class Compat {
    private Compat() {}

    private static RenderType blade;

    static Vec3 cameraPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
    }

    static int lightAt(ClientLevel level, BlockPos pos) {
        return LevelRenderer.getLightCoords(level, pos);
    }

    static int grassColor(ClientLevel level, BlockPos pos) {
        return 0xFF000000 | BiomeColors.getAverageGrassColor(level, pos);
    }

    static RenderType bladeType() {
        if (blade == null) {
            blade = RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/block/short_grass.png"));
        }
        return blade;
    }
}
