package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ClanBridgeAdapter extends AbstractClanProvider {

    private final ClanProviderBridge bridge;

    public ClanBridgeAdapter(ClanProviderBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getProviderName() {
        return bridge.getProviderName();
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        ClanProviderBridge.ClanSnapshot snap = bridge.getPlayerClan(playerId);
        return Optional.ofNullable(snap).map(this::fromSnapshot);
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        ClanProviderBridge.ClanSnapshot snap = bridge.getClanByTag(tag);
        return Optional.ofNullable(snap).map(this::fromSnapshot);
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        ClanProviderBridge.ClanSnapshot snap = bridge.getClanById(id);
        return Optional.ofNullable(snap).map(this::fromSnapshot);
    }

    @Override
    public Collection<Clan> getAllClans() {
        return bridge.getAllClans().stream()
                .map(this::fromSnapshot)
                .collect(Collectors.toList());
    }

    private Clan fromSnapshot(ClanProviderBridge.ClanSnapshot snap) {
        Set<UUID> allMembers = new HashSet<>();
        allMembers.addAll(snap.leaders());
        allMembers.addAll(snap.moderators());
        allMembers.addAll(snap.members());

        Clan.ClanBuilder builder = Clan.builder()
                .id(snap.id())
                .name(snap.name())
                .tag(snap.tag())
                .displayName(snap.displayName() != null ? snap.displayName() : snap.name())
                .level(snap.level())
                .balance(snap.balance())
                .createdAt(snap.createdAt())
                .verified(snap.verified())
                .description(snap.description() != null ? snap.description() : "")
                .maxMembers(snap.maxMembers())
                .killDeathRatio(snap.killDeathRatio())
                .providerName(bridge.getProviderName());

        snap.leaders().forEach(builder::leader);
        snap.moderators().forEach(builder::moderator);
        snap.members().forEach(builder::member);
        allMembers.forEach(builder::allMember);

        List<UUID> online = snap.onlineMembers() != null
                ? snap.onlineMembers().stream().toList()
                : List.of();
        online.forEach(builder::onlineMember);

        return builder.build();
    }
}
