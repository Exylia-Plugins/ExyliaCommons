package net.exylia.commons.v2.clan.provider;

import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.exyliaclans.api.ClanData;
import net.exylia.exyliaclans.api.ExyliaClansAPI;
import org.bukkit.Bukkit;

import java.util.*;

public class ExyliaClansProvider extends AbstractClanProvider {

    private final boolean enabled;

    public ExyliaClansProvider() {
        this.enabled = ExyliaClansAPI.isAvailable();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "ExyliaClans";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        if (!enabled) return Optional.empty();
        try {
            return ExyliaClansAPI.getClan(playerId).map(this::toClan);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ExyliaClans] Exception resolving clan for player " + playerId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return getClanByName(tag);
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        if (!enabled) return Optional.empty();
        try {
            return ExyliaClansAPI.getClanById(id).map(this::toClan);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ExyliaClans] Exception resolving clan by id '" + id + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public Collection<Clan> getAllClans() {
        if (!enabled) return Collections.emptyList();
        try {
            Collection<ClanData> all = ExyliaClansAPI.getAllClans();
            List<Clan> result = new ArrayList<>(all.size());
            for (ClanData data : all) {
                result.add(toClan(data));
            }
            return result;
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ExyliaClans] Exception retrieving all clans", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasPlayerClan(UUID playerId) {
        return enabled && ExyliaClansAPI.isInClan(playerId);
    }

    private Optional<Clan> getClanByName(String name) {
        if (!enabled) return Optional.empty();
        try {
            return ExyliaClansAPI.getClanByName(name).map(this::toClan);
        } catch (Exception e) {
            DebugAPI.logLibError("[ClanAPI/ExyliaClans] Exception resolving clan by name '" + name + "'", e);
            return Optional.empty();
        }
    }

    private Clan toClan(ClanData data) {
        UUID leaderId = data.leaderId();
        Set<UUID> leaders = Collections.singleton(leaderId);
        Set<UUID> members = new HashSet<>();
        List<UUID> online = new ArrayList<>();

        if (Bukkit.getPlayer(leaderId) != null) online.add(leaderId);

        for (UUID uuid : data.memberUuids()) {
            if (!uuid.equals(leaderId)) members.add(uuid);
            if (Bukkit.getPlayer(uuid) != null) online.add(uuid);
        }

        Set<UUID> allMembers = new HashSet<>(members);
        allMembers.add(leaderId);

        Clan.ClanBuilder builder = Clan.builder()
                .id(data.id())
                .name(data.name())
                .tag(data.name())
                .displayName(data.name())
                .level(0)
                .balance(data.balance())
                .createdAt(0L)
                .verified(true)
                .description("")
                .maxMembers(0)
                .killDeathRatio(data.kdr())
                .providerName("ExyliaClans");

        leaders.forEach(builder::leader);
        members.forEach(builder::member);
        allMembers.forEach(builder::allMember);
        online.forEach(builder::onlineMember);

        return builder.build();
    }
}
