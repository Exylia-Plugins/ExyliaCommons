package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ClanProvider {

    boolean isEnabled();

    String getProviderName();

    Optional<Clan> getPlayerClan(UUID playerId);

    CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId);

    Optional<Clan> getPlayerClan(Player player);

    CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player);

    Optional<Clan> getClanByTag(String tag);

    CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag);

    Optional<Clan> getClanById(String id);

    CompletableFuture<Optional<Clan>> getClanByIdAsync(String id);

    Collection<Clan> getAllClans();

    CompletableFuture<Collection<Clan>> getAllClansAsync();

    boolean hasPlayerClan(UUID playerId);

    boolean hasPlayerClan(Player player);

    void invalidateCache();
}
