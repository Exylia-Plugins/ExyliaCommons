package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NoClanProvider implements ClanProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "None";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String id) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public Collection<Clan> getAllClans() {
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        return false;
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        return false;
    }

    @Override
    public void invalidateCache() {
    }
}
