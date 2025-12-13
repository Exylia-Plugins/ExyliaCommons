package net.exylia.commons.v2.clan.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.v2.clan.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class KingdomsProvider implements ClanProvider {

    private final boolean enabled;
    private final Cache<UUID, String> playerKingdomCache;

    public KingdomsProvider() {
        boolean tempEnabled = false;

        try {
            Class.forName("org.kingdoms.constants.group.Kingdom");
            tempEnabled = true;
        } catch (ClassNotFoundException e) {
            tempEnabled = false;
        }

        this.enabled = tempEnabled;
        this.playerKingdomCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(5000)
                .build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "KingdomsX";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();

        try {
            KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(playerId);
            if (kp == null || !kp.hasKingdom()) {
                return Optional.empty();
            }

            Kingdom kingdom = kp.getKingdom();
            return Optional.of(convertToClan(kingdom));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID playerId) {
        return AsyncExecutor.getInstance().supplyAsync(() -> getPlayerClan(playerId), false);
    }

    @Override
    public Optional<Clan> getPlayerClan(Player player) {
        if (!enabled) return Optional.empty();

        try {
            KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
            if (kp == null || !kp.hasKingdom()) {
                return Optional.empty();
            }

            Kingdom kingdom = kp.getKingdom();
            return Optional.of(convertToClan(kingdom));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getPlayerClanAsync(Player player) {
        return AsyncExecutor.getInstance().supplyAsync(() -> getPlayerClan(player), false);
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return getClanById(tag);
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByTagAsync(String tag) {
        return getClanByIdAsync(tag);
    }

    @Override
    public Optional<Clan> getClanById(String name) {
        if (!enabled) return Optional.empty();

        try {
            Kingdom kingdom = Kingdom.getKingdom(name);
            if (kingdom == null) {
                return Optional.empty();
            }

            return Optional.of(convertToClan(kingdom));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Optional<Clan>> getClanByIdAsync(String name) {
        return AsyncExecutor.getInstance().supplyAsync(() -> getClanById(name), false);
    }

    @Override
    public Collection<Clan> getAllClans() {
        if (!enabled) return Collections.emptyList();

        try {
            Collection<Kingdom> kingdoms = org.kingdoms.main.Kingdoms.get()
                    .getDataCenter()
                    .getKingdomManager()
                    .getKingdoms();
            return kingdoms.stream()
                    .map(this::convertToClan)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public CompletableFuture<Collection<Clan>> getAllClansAsync() {
        return AsyncExecutor.getInstance().supplyAsync(this::getAllClans, false);
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        return getPlayerClan(playerId).isPresent();
    }

    @Override
    public boolean hasPlayerClan(Player player) {
        if (!enabled) return false;

        try {
            KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
            return kp != null && kp.hasKingdom();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void invalidateCache() {
        playerKingdomCache.invalidateAll();
    }

    private Clan convertToClan(Kingdom kingdom) {
        Set<UUID> allMembers = new HashSet<>();
        List<UUID> onlineMembers = new ArrayList<>();

        try {
            Collection<?> members = kingdom.getMembers();
            if (members != null) {
                for (Object obj : members) {
                    if (obj instanceof KingdomPlayer) {
                        KingdomPlayer kp = (KingdomPlayer) obj;
                        UUID playerId = kp.getId();
                        if (playerId != null) {
                            allMembers.add(playerId);

                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                onlineMembers.add(playerId);
                            }
                        }
                    } else if (obj instanceof UUID) {
                        UUID playerId = (UUID) obj;
                        allMembers.add(playerId);

                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            onlineMembers.add(playerId);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        return Clan.builder()
                .id(kingdom.getName())
                .name(kingdom.getName())
                .tag(kingdom.getName())
                .displayName(kingdom.getName())
                .leader(kingdom.getKingId())
                .allMembers(allMembers)
                .onlineMembers(onlineMembers)
                .level(0)
                .balance(0.0)
                .createdAt(0L)
                .verified(false)
                .description("")
                .maxMembers(0)
                .killDeathRatio(0.0)
                .providerName("KingdomsX")
                .build();
    }
}
