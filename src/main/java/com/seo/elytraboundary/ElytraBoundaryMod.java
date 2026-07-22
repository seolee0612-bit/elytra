package com.seo.elytraboundary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class ElytraBoundaryMod implements ModInitializer {

    // 오버월드 설정
    private static final double OVERWORLD_CENTER_X = 1141.0;
    private static final double OVERWORLD_CENTER_Z = 2548.0;
    private static final double OVERWORLD_RADIUS = 1000.0;
    private static final double OVERWORLD_RADIUS_SQUARED =
            OVERWORLD_RADIUS * OVERWORLD_RADIUS;

    // 네더 설정
    // 오버월드 좌표를 8로 나눈 대응 좌표
    private static final double NETHER_CENTER_X = 142.625;
    private static final double NETHER_CENTER_Z = 318.5;
    private static final double NETHER_RADIUS = 500.0;
    private static final double NETHER_RADIUS_SQUARED =
            NETHER_RADIUS * NETHER_RADIUS;

    /*
     * Elytra를 먼저 접고 다음 서버 틱에 킥하기 위한 목록.
     * 이렇게 해야 재접속했을 때 활공 상태가 남아 반복 킥되는 문제를 방지할 수 있다.
     */
    private static final Set<UUID> PENDING_KICKS = new HashSet<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            processPendingKicks(server);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayer(player);
            }
        });
    }

    /**
     * 이전 틱에서 Elytra가 강제로 접힌 플레이어를 킥한다.
     */
    private static void processPendingKicks(MinecraftServer server) {
        Iterator<UUID> iterator = PENDING_KICKS.iterator();

        while (iterator.hasNext()) {
            UUID playerUuid = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);

            if (player != null && player.connection != null) {
                player.connection.disconnect(
                        Component.literal(
                                "베이스 범위 밖에서의 겉날개 비행 금지"
                        )
                );
            }

            iterator.remove();
        }
    }

    /**
     * 플레이어가 제한 구역 밖에서 Elytra를 사용하는지 검사한다.
     */
    private static void checkPlayer(ServerPlayer player) {
        // 이미 다음 틱 킥 대상으로 등록되었으면 다시 처리하지 않음
        if (PENDING_KICKS.contains(player.getUUID())) {
            return;
        }

        // Elytra 활공 중이 아니면 제한하지 않음
        if (!player.isFallFlying()) {
            return;
        }

        ResourceKey<Level> dimension = player.level().dimension();

        if (dimension == Level.OVERWORLD) {
            checkBoundary(
                    player,
                    OVERWORLD_CENTER_X,
                    OVERWORLD_CENTER_Z,
                    OVERWORLD_RADIUS_SQUARED
            );
        } else if (dimension == Level.NETHER) {
            checkBoundary(
                    player,
                    NETHER_CENTER_X,
                    NETHER_CENTER_Z,
                    NETHER_RADIUS_SQUARED
            );
        }

        // 엔드 및 기타 차원에서는 아무것도 하지 않음
    }

    /**
     * Y 좌표는 무시하고 XZ 평면상의 원형 경계를 검사한다.
     */
    private static void checkBoundary(
            ServerPlayer player,
            double centerX,
            double centerZ,
            double radiusSquared
    ) {
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        if (distanceSquared > radiusSquared) {
            // 먼저 Elytra 활공 상태를 강제로 해제
            player.stopFallFlying();

            // 다음 서버 틱에 킥
            PENDING_KICKS.add(player.getUUID());
        }
    }
}
