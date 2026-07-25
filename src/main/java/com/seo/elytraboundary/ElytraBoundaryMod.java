package com.seo.elytraboundary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class ElytraBoundaryMod implements ModInitializer {

    /*
     * Minecraft 서버는 초당 20틱이므로,
     * velocity의 블록/틱 값을 블록/초로 바꿀 때 20을 곱한다.
     */
    private static final double TICKS_PER_SECOND = 20.0;

    // =========================================================
    // 오버월드 설정
    // =========================================================

    private static final double OVERWORLD_CENTER_X = 1141.0;
    private static final double OVERWORLD_CENTER_Z = 2548.0;

    private static final double OVERWORLD_RADIUS = 5000.0;
    private static final double OVERWORLD_RADIUS_SQUARED =
            OVERWORLD_RADIUS * OVERWORLD_RADIUS;

    private static final double OVERWORLD_SOFT_SPEED_LIMIT = 40.0;
    private static final double OVERWORLD_HARD_SPEED_LIMIT = 55.0;

    // =========================================================
    // 네더 설정
    // =========================================================

    /*
     * 기존 오버월드 중심 좌표를 8로 나눈 대응 좌표.
     */
    private static final double NETHER_CENTER_X = 142.625;
    private static final double NETHER_CENTER_Z = 318.5;

    private static final double NETHER_RADIUS = 1500.0;
    private static final double NETHER_RADIUS_SQUARED =
            NETHER_RADIUS * NETHER_RADIUS;

    private static final double NETHER_SOFT_SPEED_LIMIT = 25.0;
    private static final double NETHER_HARD_SPEED_LIMIT = 35.0;

    // =========================================================
    // 엔드 설정
    // =========================================================

    /*
     * 엔드는 0, 0을 기준으로 반경 3000블록으로 설정.
     * 다른 중심을 원하면 아래 두 값을 변경하면 된다.
     */
    private static final double END_CENTER_X = 0.0;
    private static final double END_CENTER_Z = 0.0;

    private static final double END_RADIUS = 3000.0;
    private static final double END_RADIUS_SQUARED =
            END_RADIUS * END_RADIUS;

    private static final double END_SOFT_SPEED_LIMIT = 40.0;
    private static final double END_HARD_SPEED_LIMIT = 55.0;

    // =========================================================
    // Aether 설정
    // =========================================================

    /*
     * The Aether의 일반적인 차원 ID:
     * the_aether:the_aether
     */
    private static final ResourceKey<Level> AETHER_DIMENSION =
            ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(
                            "aether",
                            "the_aether"
                    )
            );

    /*
     * Aether도 0, 0을 기준으로 반경 3000블록으로 설정.
     */
    private static final double AETHER_CENTER_X = 0.0;
    private static final double AETHER_CENTER_Z = 0.0;

    private static final double AETHER_RADIUS = 3000.0;
    private static final double AETHER_RADIUS_SQUARED =
            AETHER_RADIUS * AETHER_RADIUS;

    private static final double AETHER_SOFT_SPEED_LIMIT = 40.0;
    private static final double AETHER_HARD_SPEED_LIMIT = 55.0;

    /*
     * 속도가 soft limit을 넘었을 때 초과분을 매 틱 얼마나
     * 유지할 것인지 결정한다.
     *
     * 0.90이면 제한을 초과한 속도 부분이 매 틱 90%로 줄어든다.
     * 값이 작을수록 감속이 강해진다.
     */
    private static final double SOFT_LIMIT_EXCESS_RETENTION = 0.90;

    /*
     * Elytra를 먼저 접고 다음 서버 틱에 킥하기 위한 목록.
     *
     * 이렇게 하면 재접속했을 때 활공 상태가 남아서
     * 반복적으로 킥되는 문제를 줄일 수 있다.
     */
    private static final Set<UUID> PENDING_KICKS = new HashSet<>();

    private static int tickCounter = 0;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            processPendingKicks(server);

            tickCounter++;

            if ((tickCounter & 1) != 0) {
                return;
            }

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
            ServerPlayer player =
                    server.getPlayerList().getPlayer(playerUuid);

            if (player != null && player.connection != null) {
                player.connection.disconnect(
                        Component.literal(
                                "제한 범위 밖에서의 겉날개 비행은 금지되어 있습니다."
                        )
                );
            }

            iterator.remove();
        }
    }

    /**
     * 플레이어의 차원, 경계, 겉날개 속도를 검사한다.
     */
    private static void checkPlayer(ServerPlayer player) {
        if (PENDING_KICKS.contains(player.getUUID())) {
            return;
        }

        /*
         * Elytra 활공 중이 아니면 경계 및 속도 제한을 적용하지 않는다.
         */
        if (!player.isFallFlying()) {
            return;
        }

        ResourceKey<Level> dimension = player.level().dimension();

        if (dimension == Level.OVERWORLD) {
            applyDimensionRules(
                    player,
                    OVERWORLD_CENTER_X,
                    OVERWORLD_CENTER_Z,
                    OVERWORLD_RADIUS_SQUARED,
                    OVERWORLD_SOFT_SPEED_LIMIT,
                    OVERWORLD_HARD_SPEED_LIMIT
            );

        } else if (dimension == Level.NETHER) {
            applyDimensionRules(
                    player,
                    NETHER_CENTER_X,
                    NETHER_CENTER_Z,
                    NETHER_RADIUS_SQUARED,
                    NETHER_SOFT_SPEED_LIMIT,
                    NETHER_HARD_SPEED_LIMIT
            );

        } else if (dimension == Level.END) {
            applyDimensionRules(
                    player,
                    END_CENTER_X,
                    END_CENTER_Z,
                    END_RADIUS_SQUARED,
                    END_SOFT_SPEED_LIMIT,
                    END_HARD_SPEED_LIMIT
            );

        } else if (dimension.equals(AETHER_DIMENSION)) {
            applyDimensionRules(
                    player,
                    AETHER_CENTER_X,
                    AETHER_CENTER_Z,
                    AETHER_RADIUS_SQUARED,
                    AETHER_SOFT_SPEED_LIMIT,
                    AETHER_HARD_SPEED_LIMIT
            );
        }

        /*
         * 그 외 모드 차원에서는 아무 제한도 적용하지 않는다.
         */
    }

    /**
     * 해당 차원의 경계 검사와 속도 제한을 함께 적용한다.
     */
    private static void applyDimensionRules(
            ServerPlayer player,
            double centerX,
            double centerZ,
            double radiusSquared,
            double softSpeedLimit,
            double hardSpeedLimit
    ) {
        /*
         * 경계를 먼저 검사한다.
         * 경계 밖이라면 겉날개를 접고 킥 목록에 추가한다.
         */
        if (isOutsideBoundary(
                player,
                centerX,
                centerZ,
                radiusSquared
        )) {
            player.stopFallFlying();
            PENDING_KICKS.add(player.getUUID());
            return;
        }

        /*
         * 경계 안에서는 수평 겉날개 속도를 제한한다.
         */
        clampHorizontalElytraSpeed(
                player,
                softSpeedLimit,
                hardSpeedLimit
        );
    }

    /**
     * Y 좌표는 무시하고 XZ 평면상의 원형 경계를 검사한다.
     */
    private static boolean isOutsideBoundary(
            ServerPlayer player,
            double centerX,
            double centerZ,
            double radiusSquared
    ) {
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;

        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared > radiusSquared;
    }

    /**
     * 플레이어의 수평 겉날개 속도를 제한한다.
     *
     * 수직 속도 Y는 변경하지 않기 때문에,
     * 급강하나 상승 동작은 가능한 한 그대로 유지된다.
     */
    private static void clampHorizontalElytraSpeed(
            ServerPlayer player,
            double softLimitBlocksPerSecond,
            double hardLimitBlocksPerSecond
    ) {
        Vec3 velocity = player.getDeltaMovement();

        double velocityX = velocity.x;
        double velocityZ = velocity.z;

        /*
         * Minecraft velocity는 블록/틱 단위다.
         */
        double horizontalSpeedPerTick = Math.sqrt(
                velocityX * velocityX
                        + velocityZ * velocityZ
        );

        double horizontalSpeedPerSecond =
                horizontalSpeedPerTick * TICKS_PER_SECOND;

        /*
         * Soft limit 이하라면 변경하지 않는다.
         */
        if (horizontalSpeedPerSecond <= softLimitBlocksPerSecond) {
            return;
        }

        double targetSpeedPerSecond;

        if (horizontalSpeedPerSecond > hardLimitBlocksPerSecond) {
            /*
             * Hard limit을 넘으면 즉시 hard limit까지 제한한다.
             */
            targetSpeedPerSecond = hardLimitBlocksPerSecond;

        } else {
            /*
             * Soft limit과 hard limit 사이에서는 초과분만
             * 점진적으로 줄인다.
             *
             * 예:
             * 현재 50, soft 40이면 초과분은 10이다.
             * 초과분 유지율이 0.9이면 목표 속도는 49가 된다.
             */
            double excessSpeed =
                    horizontalSpeedPerSecond
                            - softLimitBlocksPerSecond;

            targetSpeedPerSecond =
                    softLimitBlocksPerSecond
                            + excessSpeed
                            * SOFT_LIMIT_EXCESS_RETENTION;
        }

        double scale =
                targetSpeedPerSecond / horizontalSpeedPerSecond;

        Vec3 clampedVelocity = new Vec3(
                velocityX * scale,
                velocity.y,
                velocityZ * scale
        );

        player.setDeltaMovement(clampedVelocity);

        /*
         * 변경된 속도를 클라이언트에도 즉시 알려서
         * 클라이언트 예측과 서버 속도 간의 차이를 줄인다.
         */
        if (player.connection != null) {
            player.connection.send(
                    new ClientboundSetEntityMotionPacket(player)
            );
        }
    }
}
