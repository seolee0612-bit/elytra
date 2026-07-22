package com.example.elytraboundary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ElytraBoundaryMod implements ModInitializer {

    private static final double CENTER_X = 1141.0;
    private static final double CENTER_Z = 2548.0;

    private static final double RADIUS = 1000.0;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayer(player);
            }
        });
    }

    private static void checkPlayer(ServerPlayer player) {
        // 오버월드에서만 작동
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        // Elytra 활공 중이 아니면 아무것도 하지 않음
        if (!player.isFallFlying()) {
            return;
        }

        // Y 좌표는 무시하고 XZ 거리만 계산
        double dx = player.getX() - CENTER_X;
        double dz = player.getZ() - CENTER_Z;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared > RADIUS_SQUARED) {
            // 재접속 시 다시 킥되지 않도록 먼저 활공 해제
            player.stopFallFlying();

            // 활공 상태가 해제된 데이터를 즉시 저장
            player.serverLevel().getServer().getPlayerList().save(player);

            // 그다음 킥
            player.connection.disconnect(
                Component.literal(
                    "베이스 주변 밖에서의 겉날개 이용은 금지입니다."
                )
            );
        }
    }
}
