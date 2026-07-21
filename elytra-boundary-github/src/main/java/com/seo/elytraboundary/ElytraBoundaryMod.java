package com.seo.elytraboundary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ElytraBoundaryMod implements ModInitializer {
    public static final String MOD_ID = "elytra_boundary";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("elytra-boundary.json");

    private ElytraBoundaryConfig config;
    private Set<String> exemptPlayersLowercase = Set.of();
    private int tickCounter;

    @Override
    public void onInitialize() {
        config = loadConfig();
        rebuildExemptionCache();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!config.enabled) return;
            if (++tickCounter < config.checkIntervalTicks) return;
            tickCounter = 0;

            final double radiusSquared = config.radius * config.radius;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.isFallFlying()) continue;
                if (config.overworldOnly && !player.getWorld().getRegistryKey().equals(World.OVERWORLD)) continue;
                if (exemptPlayersLowercase.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT))) continue;

                final double dx = player.getX() - config.centerX;
                final double dz = player.getZ() - config.centerZ;

                if (dx * dx + dz * dz > radiusSquared) {
                    LOGGER.info("Kicking {} for elytra flight outside boundary at x={}, z={}",
                            player.getGameProfile().getName(), player.getX(), player.getZ());
                    player.networkHandler.disconnect(Text.literal(config.kickMessage));
                }
            }
        });

        LOGGER.info("Elytra Boundary enabled: center=({}, {}), radius={}, interval={} ticks, overworldOnly={}",
                config.centerX, config.centerZ, config.radius, config.checkIntervalTicks, config.overworldOnly);
    }

    private ElytraBoundaryConfig loadConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    ElytraBoundaryConfig loaded = GSON.fromJson(reader, ElytraBoundaryConfig.class);
                    if (loaded != null) {
                        loaded.sanitize();
                        writeConfig(loaded);
                        return loaded;
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Could not read {}. Defaults will be used.", CONFIG_PATH, exception);
        }

        ElytraBoundaryConfig defaults = new ElytraBoundaryConfig();
        defaults.sanitize();
        writeConfig(defaults);
        return defaults;
    }

    private void writeConfig(ElytraBoundaryConfig value) {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(value, writer);
        } catch (IOException exception) {
            LOGGER.error("Could not write {}", CONFIG_PATH, exception);
        }
    }

    private void rebuildExemptionCache() {
        Set<String> normalized = new HashSet<>();
        for (String name : config.exemptPlayers) {
            if (name != null && !name.isBlank()) normalized.add(name.toLowerCase(Locale.ROOT));
        }
        exemptPlayersLowercase = Set.copyOf(normalized);
    }
}
