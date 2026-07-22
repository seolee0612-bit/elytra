package com.seo.elytraboundary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ElytraBoundaryMod implements ModInitializer {
    private static final double CENTER_X = 1141.0;
    private static final double CENTER_Z = 2548.0;
    private static final double RADIUS = 3000.0;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level().dimension() != Level.OVERWORLD || !player.isFallFlying()) {
                    continue;
                }

                double dx = player.getX() - CENTER_X;
                double dz = player.getZ() - CENTER_Z;

                if ((dx * dx) + (dz * dz) > RADIUS_SQUARED) {
                    player.connection.disconnect(Component.literal(
                            "Elytra flight is not allowed more than 3000 blocks from (1141, 2548)."
                    ));
                }
            }
        });
    }
}
