package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractClanProvider implements ClanProvider {

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> getPlayerClan(playerId));
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        return getPlayerClan(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return getPlayerClanAsync(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> getClanByTag(tag));
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        return CompletableFuture.supplyAsync(() -> getClanById(id));
    }

    @Override
    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return CompletableFuture.supplyAsync(this::getAllClans);
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        return getPlayerClan(playerId).isPresent();
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        return hasPlayerClan(player.getUniqueId());
    }

    @Override
    public void invalidateCache() {}
}
