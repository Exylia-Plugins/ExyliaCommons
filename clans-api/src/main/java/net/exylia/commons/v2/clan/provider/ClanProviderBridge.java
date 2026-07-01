package net.exylia.commons.v2.clan.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Lightweight integration interface for external plugins that want to register
 * a custom clan provider without depending on the full ExyliaCommons API.
 *
 * <p>Uses only standard Java types — no Bukkit or Exylia imports required.
 * Return {@code null} from any lookup method to indicate "not found".
 *
 * <p>Register via:
 * <pre>{@code
 * ClanIntegration.register(new MyPluginBridge());
 * }</pre>
 */
public interface ClanProviderBridge {

    String getProviderName();

    ClanSnapshot getPlayerClan(UUID playerId);

    ClanSnapshot getClanByTag(String tag);

    ClanSnapshot getClanById(String id);

    Collection<ClanSnapshot> getAllClans();

    record ClanSnapshot(
            String id,
            String name,
            String tag,
            String displayName,
            Set<UUID> leaders,
            Set<UUID> moderators,
            Set<UUID> members,
            Set<UUID> onlineMembers,
            int level,
            double balance,
            long createdAt,
            boolean verified,
            String description,
            int maxMembers,
            double killDeathRatio
    ) {
        public static ClanSnapshot of(String id, String name, String tag,
                                      Set<UUID> leaders, Set<UUID> members) {
            return new ClanSnapshot(
                    id, name, tag, name,
                    leaders, Collections.emptySet(), members, Collections.emptySet(),
                    0, 0.0, 0L, false, "", 0, 0.0
            );
        }

        public static ClanSnapshot of(String id, String name, String tag,
                                      Set<UUID> leaders, Set<UUID> moderators,
                                      Set<UUID> members) {
            return new ClanSnapshot(
                    id, name, tag, name,
                    leaders, moderators, members, Collections.emptySet(),
                    0, 0.0, 0L, false, "", 0, 0.0
            );
        }
    }
}
