package net.exylia.commons.v2.items.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public final class RegionFilter {

    private static Boolean worldGuardPresent = null;

    private static final RegionFilter NONE_INSTANCE = new RegionFilter(
            RegionFilterType.NONE, RegionFilterChecker.CONTAINS,
            Collections.emptyList(), Collections.emptyMap(), false
    );

    private static final RegionFilter PVP_CHECK_INSTANCE = new RegionFilter(
            RegionFilterType.NONE, RegionFilterChecker.CONTAINS,
            Collections.emptyList(), Collections.emptyMap(), true
    );

    private final RegionFilterType type;
    private final RegionFilterChecker checker;
    private final List<RegionEntry> entries;
    private final Map<String, Double> cooldowns;
    private final boolean checkPvp;

    private RegionFilter(RegionFilterType type, RegionFilterChecker checker,
                         List<RegionEntry> entries, Map<String, Double> cooldowns, boolean checkPvp) {
        this.type = type;
        this.checker = checker;
        this.entries = entries;
        this.cooldowns = cooldowns;
        this.checkPvp = checkPvp;
    }

    public static RegionFilter none() {
        return NONE_INSTANCE;
    }

    public static RegionFilter fromConfig(ConfigurationSection section) {
        if (section == null) return NONE_INSTANCE;

        boolean checkPvp = section.getBoolean("disable-in-non-pvp-regions", true);
        RegionFilterType type = RegionFilterType.fromString(section.getString("type", "NONE"));

        if (type == RegionFilterType.NONE) {
            return checkPvp ? PVP_CHECK_INSTANCE : NONE_INSTANCE;
        }

        RegionFilterChecker checker = RegionFilterChecker.fromString(section.getString("checker", "CONTAINS"));

        List<RegionEntry> entries = new ArrayList<>();
        for (String raw : section.getStringList("list")) {
            String[] parts = raw.split("\\|", 2);
            String regionId = parts[0].trim().toLowerCase();
            String worldName = parts.length > 1 ? parts[1].trim() : null;
            entries.add(new RegionEntry(regionId, worldName));
        }

        Map<String, Double> cooldowns = new HashMap<>();
        ConfigurationSection cooldownSection = section.getConfigurationSection("cooldowns");
        if (cooldownSection != null) {
            for (String key : cooldownSection.getKeys(false)) {
                cooldowns.put(key.toLowerCase(), cooldownSection.getDouble(key));
            }
        }

        return new RegionFilter(type, checker, entries, cooldowns, checkPvp);
    }

    public boolean allows(Player player) {
        if (checkPvp && isWorldGuardAvailable() && !isPvpAllowed(player)) return false;
        if (type == RegionFilterType.NONE) return true;
        boolean matches = matchesFilter(player.getLocation(), player.getWorld().getName(), player);
        return type == RegionFilterType.WHITELIST ? matches : !matches;
    }

    public boolean allowsTarget(LivingEntity entity) {
        if (!(entity instanceof Player target)) return true;
        if (checkPvp && isWorldGuardAvailable() && !isPvpAllowed(target)) return false;
        if (type == RegionFilterType.NONE) return true;
        boolean matches = matchesFilter(target.getLocation(), target.getWorld().getName(), target);
        return type == RegionFilterType.WHITELIST ? matches : !matches;
    }

    public double getApplicableCooldown(Player player, double defaultCooldown) {
        if (cooldowns.isEmpty() || !isWorldGuardAvailable()) return defaultCooldown;
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            for (ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()))) {
                Double override = cooldowns.get(region.getId().toLowerCase());
                if (override != null) return override;
            }
        } catch (Exception ignored) {}
        return defaultCooldown;
    }

    public boolean isActive() {
        return type != RegionFilterType.NONE || checkPvp;
    }

    private boolean matchesFilter(Location location, String worldName, Player player) {
        if (!isWorldGuardAvailable()) return false;
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

            List<ProtectedRegion> regions = new ArrayList<>();
            query.getApplicableRegions(BukkitAdapter.adapt(location)).forEach(regions::add);

            if (checker == RegionFilterChecker.PRIORITY) {
                if (regions.isEmpty()) return false;
                return matchesEntry(regions.get(0).getId(), worldName);
            }

            for (ProtectedRegion region : regions) {
                if (matchesEntry(region.getId(), worldName)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesEntry(String regionId, String worldName) {
        for (RegionEntry entry : entries) {
            if (!entry.regionId().equalsIgnoreCase(regionId)) continue;
            if (entry.worldName() == null || entry.worldName().equalsIgnoreCase(worldName)) return true;
        }
        return false;
    }

    private boolean isPvpAllowed(Player player) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            return query.testState(BukkitAdapter.adapt(player.getLocation()),
                    WorldGuardPlugin.inst().wrapPlayer(player), Flags.PVP);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isWorldGuardAvailable() {
        if (worldGuardPresent == null) {
            worldGuardPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        }
        return worldGuardPresent;
    }

    private record RegionEntry(String regionId, String worldName) {}
}
