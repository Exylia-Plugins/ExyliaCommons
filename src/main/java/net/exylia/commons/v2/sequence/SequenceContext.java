package net.exylia.commons.v2.sequence;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiPredicate;

@Getter
@Builder
public class SequenceContext {

    private final Location location;

    @Nullable
    private final Player sourcePlayer;

    @Nullable
    private final Entity targetEntity;

    /**
     * Optional per-observer particle visibility predicate.
     * Receives (observer, sourcePlayerUUID) → true if the particle should be shown.
     * When null, particles are spawned normally via world.spawnParticle().
     */
    @Nullable
    private final BiPredicate<Player, UUID> particleFilter;
}
