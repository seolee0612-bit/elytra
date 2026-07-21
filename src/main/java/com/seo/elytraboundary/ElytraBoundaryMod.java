package com.seo.elytraboundary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class ElytraBoundaryMod implements ModInitializer {
    private static final double CENTER_X = 1141.0;
    private static final double CENTER_Z = 2548.0;
    private static final double RADIUS = 3000.0;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayer(player);
            }
        });
    }

    private static void checkPlayer(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() != World.OVERWORLD) {
            return;
        }

        if (!player.isFallFlying()) {
            return;
        }

        double dx = player.getX() - CENTER_X;
        double dz = player.getZ() - CENTER_Z;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared > RADIUS_SQUARED) {
            player.networkHandler.disconnect(Text.literal(
                "Elytra flight is not allowed outside the 3000-block boundary."
            ));
        }
    }
}
