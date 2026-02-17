package net.exylia.commons.v2.combat.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CombatData {

    private final UUID playerId;
    private final String playerName;
    private final int kills;
    private final int deaths;
    private final double kdr;
    private final int streak;
    private final int highestStreak;
    private final int combatLogs;
    private final int points;
    private final String providerName;

    public boolean hasKills() {
        return kills > 0;
    }

    public boolean hasDeaths() {
        return deaths > 0;
    }

    public boolean isPositiveKDR() {
        return kdr >= 1.0;
    }
}
