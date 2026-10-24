package com.example.lushgrass;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties config: .minecraft/config/lushgrass.properties */
final class Config {
    static boolean enabled = true;
    static int radius = 12;        // blocks around the player that get animated blades
    static int maxTufts = 1400;    // hard cap on animated clusters per frame
    static float bend = 0.55f;     // how far entities push blades (blocks at the tip)
    static float wind = 0.10f;     // wind sway strength (blocks at the tip)

    static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("lushgrass.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
            }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            radius = clamp(Integer.parseInt(p.getProperty("radius", "12")), 2, 32);
            maxTufts = clamp(Integer.parseInt(p.getProperty("maxTufts", "1400")), 50, 6000);
            bend = Float.parseFloat(p.getProperty("bend", "0.55"));
            wind = Float.parseFloat(p.getProperty("wind", "0.10"));
            p.setProperty("enabled", String.valueOf(enabled));
            p.setProperty("radius", String.valueOf(radius));
            p.setProperty("maxTufts", String.valueOf(maxTufts));
            p.setProperty("bend", String.valueOf(bend));
            p.setProperty("wind", String.valueOf(wind));
            try (Writer w = Files.newBufferedWriter(file)) { p.store(w, "Lush Grass"); }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[lushgrass] bad config, using defaults: " + e);
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
