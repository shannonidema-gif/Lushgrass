package com.example.lushgrass;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Cache of nearby grass blocks (rescanned every 10 ticks, never per frame) plus a per-blade
 * spring simulation. Only grass within Config.radius of the player is animated; everything
 * farther away keeps the free baked stubble from the block model.
 */
final class GrassField {
    private GrassField() {}

    static final int STRIDE = 6; // x, y, z (camera-relative), bendX, bendZ, height

    private static final class Tuft {
        final int x, y, z;
        float bx, bz, vx, vz;
        final float h;
        int color, light;
        Tuft(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            int hash = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
            this.h = 0.42f + ((hash >>> 3) & 255) / 255f * 0.20f;
        }
    }

    private static Long2ObjectOpenHashMap<Tuft> tufts = new Long2ObjectOpenHashMap<>();
    private static Long2ObjectOpenHashMap<Tuft> spare = new Long2ObjectOpenHashMap<>();

    // nearby entities, refreshed each tick
    private static double[] ex = new double[48], ey = new double[48], ez = new double[48],
            er = new double[48], eh = new double[48];
    private static int en;

    // per-frame output, read by the render lambda
    static float[] data = new float[0];
    static int[] colors = new int[0];
    static int[] lights = new int[0];

    private static int tickCounter;
    private static long lastNanos;

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !Config.enabled) {
            tufts.clear();
            en = 0;
            return;
        }
        collectEntities(level, mc);
        if (tickCounter++ % 10 == 0) rescan(level, mc);
    }

    private static void collectEntities(ClientLevel level, Minecraft mc) {
        en = 0;
        AABB box = mc.player.getBoundingBox().inflate(Config.radius);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity e : list) {
            if (en >= ex.length) break;
            ex[en] = e.getX();
            ey[en] = e.getY();
            ez[en] = e.getZ();
            er[en] = 0.35 + e.getBbWidth() * 0.6;
            eh[en] = e.getBbHeight();
            en++;
        }
    }

    private static void rescan(ClientLevel level, Minecraft mc) {
        int R = Config.radius;
        int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
        int r2 = R * R;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        spare.clear();
        int n = 0;
        scan:
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                for (int y = py + 4; y >= py - 6; y--) {
                    pos.set(px + dx, y, pz + dz);
                    if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;
                    above.set(px + dx, y + 1, pz + dz);
                    if (!level.getBlockState(above).isAir()) continue;
                    long key = pos.asLong();
                    Tuft t = tufts.remove(key);
                    if (t == null) t = new Tuft(pos.getX(), pos.getY(), pos.getZ());
                    t.color = Compat.grassColor(level, pos);
                    t.light = Compat.lightAt(level, above);
                    spare.put(key, t);
                    if (++n >= Config.maxTufts) break scan;
                }
            }
        }
        Long2ObjectOpenHashMap<Tuft> swap = tufts;
        tufts = spare;
        spare = swap;
        spare.clear();
    }

    /** Advances the spring sim one frame and fills data/colors/lights. Returns blade count. */
    static int update(Vec3 cam, double time) {
        int size = tufts.size();
        if (size == 0) return 0;
        if (data.length < size * STRIDE) {
            data = new float[size * STRIDE];
            colors = new int[size];
            lights = new int[size];
        }
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.05f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;

        final float K = 70f, C = 6.5f, MAX = 0.6f;
        final float wind = Config.wind, bend = Config.bend;
        int i = 0;
        for (Tuft t : tufts.values()) {
            float cx = t.x + 0.5f, cz = t.z + 0.5f, topY = t.y + 1f;

            float tx = (float) (Math.sin(time * 1.7 + t.x * 0.55 + t.z * 0.35)
                    + 0.6 * Math.sin(time * 0.6 + t.z * 0.21 - t.x * 0.13)) * wind;
            float tz = (float) (0.8 * Math.cos(time * 1.4 + t.z * 0.50 + t.x * 0.30)) * wind;

            for (int e = 0; e < en; e++) {
                if (ey[e] > topY + 0.6 || ey[e] + eh[e] < topY) continue;
                float dx = (float) (cx - ex[e]), dz = (float) (cz - ez[e]);
                float d2 = dx * dx + dz * dz;
                float r = (float) er[e];
                if (d2 >= r * r) continue;
                float d = (float) Math.sqrt(d2);
                if (d < 1.0e-4f) { dx = 1f; dz = 0f; d = 1f; }
                float push = (1f - d / r) * bend;
                tx += dx / d * push;
                tz += dz / d * push;
            }

            t.vx += ((tx - t.bx) * K - t.vx * C) * dt;
            t.vz += ((tz - t.bz) * K - t.vz * C) * dt;
            t.bx += t.vx * dt;
            t.bz += t.vz * dt;
            float len = (float) Math.sqrt(t.bx * t.bx + t.bz * t.bz);
            if (len > MAX) { t.bx *= MAX / len; t.bz *= MAX / len; }

            int o = i * STRIDE;
            data[o]     = (float) (cx - cam.x);
            data[o + 1] = (float) (topY + 0.2 - cam.y);
            data[o + 2] = (float) (cz - cam.z);
            data[o + 3] = t.bx;
            data[o + 4] = t.bz;
            data[o + 5] = t.h;
            colors[i] = t.color;
            lights[i] = t.light;
            i++;
        }
        return i;
    }
}
