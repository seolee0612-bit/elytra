package com.seo.elytraboundary;

import java.util.ArrayList;
import java.util.List;

public final class ElytraBoundaryConfig {
    public boolean enabled = true;
    public double centerX = 1141.0;
    public double centerZ = 2548.0;
    public double radius = 3000.0;
    public int checkIntervalTicks = 5;
    public String kickMessage = "프리젠 영역 밖에서는 겉날개를 사용할 수 없습니다.";
    public boolean overworldOnly = true;
    public List<String> exemptPlayers = new ArrayList<>();

    public void sanitize() {
        if (!Double.isFinite(centerX)) centerX = 1141.0;
        if (!Double.isFinite(centerZ)) centerZ = 2548.0;
        if (!Double.isFinite(radius) || radius < 1.0) radius = 3000.0;
        if (checkIntervalTicks < 1) checkIntervalTicks = 1;
        if (checkIntervalTicks > 200) checkIntervalTicks = 200;
        if (kickMessage == null || kickMessage.isBlank()) {
            kickMessage = "프리젠 영역 밖에서는 겉날개를 사용할 수 없습니다.";
        }
        if (exemptPlayers == null) exemptPlayers = new ArrayList<>();
    }
}
