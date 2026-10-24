# Lush Grass (Fabric, MC 26.3)
1. Baked model (assets/): fuller side fringe + 4px stubble. Free, Sodium-friendly.
2. Animated blades (src/main/java): within `radius` blocks of you, grass blocks with air above
   get crossed blades that spring away from entities and sway in wind.

Build: JDK 25, `gradle wrapper` once, then `./gradlew build` -> build/libs/lushgrass-1.0.0.jar
Config: .minecraft/config/lushgrass.properties (enabled, radius, maxTufts, bend, wind)

If compilation fails, fix Compat.java first (all version-fragile calls are there),
then the imports in GrassRenderer/LushGrassClient (LevelRenderEvents / LevelRenderContext).
