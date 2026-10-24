package com.example.lushgrass;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Submits one batch of custom geometry per frame through vanilla's submit-node system.
 * Nothing here touches chunk meshing, so Sodium (and Iris) are unaffected.
 */
final class GrassRenderer {
    private GrassRenderer() {}

    private static final float HW = 0.45f;       // half width of each blade plane
    private static final float V_TIP = 0f, V_BASE = 0.75f; // top 12px of short_grass.png
    private static final float S = 0.70710677f;

    static void render(LevelRenderContext ctx) {
        if (!Config.enabled) return;
        double time = System.nanoTime() / 1.0e9;
        int n = GrassField.update(Compat.cameraPos(), time);
        if (n == 0) return;
        // Fresh pose stack: all coordinates are already camera-relative.
        PoseStack poseStack = new PoseStack();
        ctx.submitNodeCollector().submitCustomGeometry(poseStack, Compat.bladeType(),
                (pose, buffer) -> emit(pose, buffer, n));
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer b, int n) {
        float[] d = GrassField.data;
        for (int i = 0; i < n; i++) {
            int o = i * GrassField.STRIDE;
            float x = d[o], y = d[o + 1], z = d[o + 2], bx = d[o + 3], bz = d[o + 4], h = d[o + 5];
            int color = GrassField.colors[i], light = GrassField.lights[i];
            plane(pose, b, x, y, z, S, S, bx, bz, h, color, light);
            plane(pose, b, x, y, z, S, -S, bx, bz, h, color, light);
        }
    }

    /** One double-sided blade plane along direction (ux, uz), tip sheared by (bx, bz). */
    private static void plane(PoseStack.Pose p, VertexConsumer b, float x, float y, float z,
                              float ux, float uz, float bx, float bz, float h, int c, int l) {
        float blx = x - ux * HW, blz = z - uz * HW;
        float brx = x + ux * HW, brz = z + uz * HW;
        float tlx = blx + bx, tlz = blz + bz, trx = brx + bx, trz = brz + bz;
        float ty = y + h;
        // front
        v(p, b, blx, y, blz, 0f, V_BASE, c, l);
        v(p, b, brx, y, brz, 1f, V_BASE, c, l);
        v(p, b, trx, ty, trz, 1f, V_TIP, c, l);
        v(p, b, tlx, ty, tlz, 0f, V_TIP, c, l);
        // back (reverse winding so it survives backface culling)
        v(p, b, blx, y, blz, 0f, V_BASE, c, l);
        v(p, b, tlx, ty, tlz, 0f, V_TIP, c, l);
        v(p, b, trx, ty, trz, 1f, V_TIP, c, l);
        v(p, b, brx, y, brz, 1f, V_BASE, c, l);
    }

    private static void v(PoseStack.Pose p, VertexConsumer b, float x, float y, float z,
                          float u, float vv, int color, int light) {
        b.addVertex(p, x, y, z)
                .setColor(color)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, 0f, 1f, 0f);
    }
}
