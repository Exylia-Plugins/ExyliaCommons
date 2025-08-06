package net.exylia.commons.item.config;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder para ItemConfiguration (separado para mejor modularización)
 * ACTUALIZADO: Soporte para TriggerType
 */
public class ItemConfigurationBuilder {

    protected String material = "STONE";
    protected String name = null;
    protected List<String> lore = new ArrayList<>();
    protected int amount = 1;
    protected boolean glowing = false;
    protected boolean hideAttributes = true;
    protected int slot = -1;
    protected List<String> commands = new ArrayList<>();
    protected String action = null;
    protected boolean consumeOnUse = false;
    protected boolean cancelEvent = true;
    protected boolean stackable = true;
    protected int maxUses = 1;
    protected double cooldownSeconds = 0.0;
    protected boolean allowMovement = true;
    protected boolean allowShiftClick = true;
    protected boolean allowDrop = true;
    protected boolean allowSwapToOffhand = true;
    protected boolean allowNumberKeys = true;

    // Campos para efectos
    protected String soundOnUse = null;
    protected String particlesOnUse = null;
    protected String fireworkOnUse = null;

    protected Map<String, Object> actionConfig = new HashMap<>();
    protected boolean usePlaceholders = false;

    // Sistema de regiones
    protected RegionFilterType regionType = RegionFilterType.NONE;
    protected RegionCheckerType regionChecker = RegionCheckerType.CONTAINS;
    protected List<RegionEntry> regionEntries = new ArrayList<>();
    protected List<String> regionList = new ArrayList<>();
    protected Map<String, Double> regionCooldowns = new HashMap<>();

    // NUEVO: TriggerType
    protected TriggerType triggerType = TriggerType.IMMEDIATE;

    public ItemConfigurationBuilder material(String material) {
        this.material = material;
        return this;
    }

    public ItemConfigurationBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemConfigurationBuilder lore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    public ItemConfigurationBuilder lore(String... lore) {
        this.lore = List.of(lore);
        return this;
    }

    public ItemConfigurationBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public ItemConfigurationBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    public ItemConfigurationBuilder hideAttributes(boolean hideAttributes) {
        this.hideAttributes = hideAttributes;
        return this;
    }

    public ItemConfigurationBuilder slot(int slot) {
        this.slot = slot;
        return this;
    }

    public ItemConfigurationBuilder commands(List<String> commands) {
        this.commands = new ArrayList<>(commands);
        return this;
    }

    public ItemConfigurationBuilder commands(String... commands) {
        this.commands = List.of(commands);
        return this;
    }

    public ItemConfigurationBuilder action(String action) {
        this.action = action;
        return this;
    }

    public ItemConfigurationBuilder consumeOnUse(boolean consumeOnUse) {
        this.consumeOnUse = consumeOnUse;
        return this;
    }

    public ItemConfigurationBuilder cancelEvent(boolean cancelEvent) {
        this.cancelEvent = cancelEvent;
        return this;
    }

    public ItemConfigurationBuilder stackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public ItemConfigurationBuilder maxUses(int maxUses) {
        this.maxUses = maxUses;
        return this;
    }

    public ItemConfigurationBuilder cooldownSeconds(double seconds) {
        this.cooldownSeconds = seconds;
        return this;
    }

    public ItemConfigurationBuilder cooldownSeconds(int seconds) {
        this.cooldownSeconds = (double) seconds;
        return this;
    }

    public ItemConfigurationBuilder usePlaceholders(boolean use) {
        this.usePlaceholders = use;
        return this;
    }

    public ItemConfigurationBuilder actionConfig(Map<String, Object> config) {
        this.actionConfig = new HashMap<>(config);
        return this;
    }

    public ItemConfigurationBuilder actionConfigValue(String key, Object value) {
        this.actionConfig.put(key, value);
        return this;
    }

    public ItemConfigurationBuilder soundOnUse(String soundString) {
        this.soundOnUse = soundString;
        return this;
    }

    public ItemConfigurationBuilder particlesOnUse(String particleString) {
        this.particlesOnUse = particleString;
        return this;
    }

    public ItemConfigurationBuilder fireworkOnUse(String fireworkString) {
        this.fireworkOnUse = fireworkString;
        return this;
    }

    public ItemConfigurationBuilder allowMovement(boolean allow) {
        this.allowMovement = allow;
        return this;
    }

    public ItemConfigurationBuilder allowShiftClick(boolean allow) {
        this.allowShiftClick = allow;
        return this;
    }

    public ItemConfigurationBuilder allowDrop(boolean allow) {
        this.allowDrop = allow;
        return this;
    }

    public ItemConfigurationBuilder allowSwapToOffhand(boolean allow) {
        this.allowSwapToOffhand = allow;
        return this;
    }

    public ItemConfigurationBuilder allowNumberKeys(boolean allow) {
        this.allowNumberKeys = allow;
        return this;
    }

    public ItemConfigurationBuilder lobbyItem() {
        return allowMovement(false)
                .allowShiftClick(false)
                .allowDrop(false)
                .maxUses(-1)
                .allowSwapToOffhand(false)
                .allowNumberKeys(false);
    }

    public ItemConfigurationBuilder userItem() {
        return allowMovement(true)
                .allowShiftClick(true)
                .allowDrop(true)
                .allowSwapToOffhand(true)
                .allowNumberKeys(true);
    }

    public ItemConfigurationBuilder triggerType(TriggerType triggerType) {
        this.triggerType = triggerType != null ? triggerType : TriggerType.IMMEDIATE;
        return this;
    }

    public ItemConfigurationBuilder triggerType(String triggerType) {
        this.triggerType = TriggerType.fromString(triggerType);
        return this;
    }

    public ItemConfigurationBuilder regionType(RegionFilterType type) {
        this.regionType = type != null ? type : RegionFilterType.NONE;
        return this;
    }

    public ItemConfigurationBuilder regionType(String typeString) {
        this.regionType = RegionFilterType.fromString(typeString);
        return this;
    }

    public ItemConfigurationBuilder regionChecker(RegionCheckerType checker) {
        this.regionChecker = checker != null ? checker : RegionCheckerType.CONTAINS;
        return this;
    }

    public ItemConfigurationBuilder regionChecker(String checkerString) {
        this.regionChecker = RegionCheckerType.fromString(checkerString);
        return this;
    }

    public ItemConfigurationBuilder regionEntries(List<RegionEntry> entries) {
        this.regionEntries = new ArrayList<>(entries);
        this.regionList = entries.stream()
                .map(RegionEntry::getRegionName)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return this;
    }

    public ItemConfigurationBuilder regionEntriesFromStrings(List<String> entryStrings) {
        this.regionEntries = new ArrayList<>();
        this.regionList = new ArrayList<>();

        for (String entryString : entryStrings) {
            try {
                RegionEntry entry = RegionEntry.parse(entryString);
                this.regionEntries.add(entry);
                this.regionList.add(entry.getRegionName());
            } catch (IllegalArgumentException e) {
                DebugUtils.logInternalError("Warning: Invalid region entry '" + entryString + "': " + e.getMessage());
            }
        }
        return this;
    }

    public ItemConfigurationBuilder addRegionEntry(RegionEntry entry) {
        if (!this.regionEntries.contains(entry)) {
            this.regionEntries.add(entry);
            if (!this.regionList.contains(entry.getRegionName())) {
                this.regionList.add(entry.getRegionName());
            }
        }
        return this;
    }

    public ItemConfigurationBuilder addRegionEntry(String entryString) {
        try {
            RegionEntry entry = RegionEntry.parse(entryString);
            return addRegionEntry(entry);
        } catch (IllegalArgumentException e) {
            DebugUtils.logInternalError("Warning: Invalid region entry '" + entryString + "': " + e.getMessage());
            return this;
        }
    }

    public ItemConfigurationBuilder regionList(List<String> regions) {
        this.regionList = new ArrayList<>(regions);
        this.regionEntries = regions.stream()
                .map(RegionEntry::forAnyWorld)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return this;
    }

    public ItemConfigurationBuilder regionList(String... regions) {
        return regionList(List.of(regions));
    }

    public ItemConfigurationBuilder addRegion(String regionName) {
        if (!this.regionList.contains(regionName)) {
            this.regionList.add(regionName);
            this.regionEntries.add(RegionEntry.forAnyWorld(regionName));
        }
        return this;
    }

    public ItemConfigurationBuilder regionCooldowns(Map<String, Double> cooldowns) {
        this.regionCooldowns = new HashMap<>(cooldowns);
        return this;
    }

    public ItemConfigurationBuilder regionCooldown(String regionName, double cooldownSeconds) {
        this.regionCooldowns.put(regionName, cooldownSeconds);
        return this;
    }

    public ItemConfigurationBuilder regionCooldown(String regionName, int cooldownSeconds) {
        this.regionCooldowns.put(regionName, (double) cooldownSeconds);
        return this;
    }

    public ItemConfigurationBuilder whitelistRegions(String... regions) {
        return regionType(RegionFilterType.WHITELIST)
                .regionList(regions);
    }

    public ItemConfigurationBuilder blacklistRegions(String... regions) {
        return regionType(RegionFilterType.BLACKLIST)
                .regionList(regions);
    }

    public ItemConfigurationBuilder whitelistRegionsWithChecker(RegionCheckerType checker, String... regions) {
        return regionType(RegionFilterType.WHITELIST)
                .regionChecker(checker)
                .regionList(regions);
    }

    public ItemConfigurationBuilder blacklistRegionsWithChecker(RegionCheckerType checker, String... regions) {
        return regionType(RegionFilterType.BLACKLIST)
                .regionChecker(checker)
                .regionList(regions);
    }

    public ItemConfigurationBuilder radius(double radius) {
        return actionConfigValue("radius", radius);
    }

    public ItemConfigurationBuilder affectSelf(boolean affectSelf) {
        return actionConfigValue("affect-self", affectSelf);
    }

    public ItemConfigurationBuilder onlyPlayers(boolean onlyPlayers) {
        return actionConfigValue("only-players", onlyPlayers);
    }

    public ItemConfigurationBuilder requireLineOfSight(boolean requireLineOfSight) {
        return actionConfigValue("require-line-of-sight", requireLineOfSight);
    }

    public ItemConfigurationBuilder maxTargets(int maxTargets) {
        return actionConfigValue("max-targets", maxTargets);
    }

    public ItemConfigurationBuilder radiusConfig(double radius, boolean affectSelf, boolean onlyPlayers) {
        return radius(radius)
                .affectSelf(affectSelf)
                .onlyPlayers(onlyPlayers);
    }

    public ItemConfigurationBuilder radiusConfig(double radius, boolean affectSelf, boolean onlyPlayers,
                                                 boolean requireLineOfSight, int maxTargets) {
        return radius(radius)
                .affectSelf(affectSelf)
                .onlyPlayers(onlyPlayers)
                .requireLineOfSight(requireLineOfSight)
                .maxTargets(maxTargets);
    }

    public ItemConfigurationBuilder loadFromConfig(ConfigurationSection config) {
        if (config.contains("material")) {
            material(config.getString("material"));
        }

        if (config.contains("name")) {
            name(config.getString("name"));
        }

        if (config.contains("lore")) {
            if (config.isList("lore")) {
                lore(config.getStringList("lore"));
            } else {
                lore(config.getString("lore"));
            }
        }

        if (config.contains("amount")) {
            amount(config.getInt("amount"));
        }

        if (config.contains("glow") || config.contains("glowing")) {
            glowing(config.getBoolean("glow", config.getBoolean("glowing")));
        }

        if (config.contains("hide-attributes")) {
            hideAttributes(config.getBoolean("hide-attributes"));
        }

        if (config.contains("slot")) {
            slot(config.getInt("slot"));
        }

        if (config.contains("commands")) {
            if (config.isList("commands")) {
                commands(config.getStringList("commands"));
            } else {
                commands(config.getString("commands"));
            }
        }

        if (config.contains("action")) {
            action(config.getString("action"));
        }

        if (config.contains("consume-on-use")) {
            consumeOnUse(config.getBoolean("consume-on-use"));
        }

        if (config.contains("cancel-event")) {
            cancelEvent(config.getBoolean("cancel-event"));
        }

        if (config.contains("stackable")) {
            stackable(config.getBoolean("stackable"));
        }

        if (config.contains("max-uses")) {
            maxUses(config.getInt("max-uses"));
        }

        if (config.contains("cooldown")) {
            Object cooldownValue = config.get("cooldown");
            if (cooldownValue instanceof Number) {
                cooldownSeconds(((Number) cooldownValue).doubleValue());
            } else if (cooldownValue instanceof String) {
                try {
                    cooldownSeconds(Double.parseDouble((String) cooldownValue));
                } catch (NumberFormatException e) {
                    cooldownSeconds(config.getInt("cooldown", 0));
                }
            }
        }

        if (config.contains("sound-on-use")) {
            soundOnUse(config.getString("sound-on-use"));
        }

        if (config.contains("particles-on-use")) {
            particlesOnUse(config.getString("particles-on-use"));
        }

        if (config.contains("firework-on-use")) {
            fireworkOnUse(config.getString("firework-on-use"));
        }

        if (config.contains("allow-movement")) {
            allowMovement(config.getBoolean("allow-movement"));
        }

        if (config.contains("allow-shift-click")) {
            allowShiftClick(config.getBoolean("allow-shift-click"));
        }

        if (config.contains("allow-drop")) {
            allowDrop(config.getBoolean("allow-drop"));
        }

        if (config.contains("allow-swap-offhand")) {
            allowSwapToOffhand(config.getBoolean("allow-swap-offhand"));
        }

        if (config.contains("allow-number-keys")) {
            allowNumberKeys(config.getBoolean("allow-number-keys"));
        }

        if (config.contains("region.type")) {
            regionType(config.getString("region.type"));
        }

        if (config.contains("region.checker")) {
            regionChecker(config.getString("region.checker"));
        }

        if (config.contains("region.list")) {
            if (config.isList("region.list")) {
                List<String> regionStrings = config.getStringList("region.list");
                regionEntriesFromStrings(regionStrings);
            } else {
                String regionString = config.getString("region.list");
                if (regionString != null && !regionString.trim().isEmpty()) {
                    String[] regions = regionString.split(",");
                    List<String> regionList = new ArrayList<>();
                    for (String region : regions) {
                        String trimmed = region.trim();
                        if (!trimmed.isEmpty()) {
                            regionList.add(trimmed);
                        }
                    }
                    regionEntriesFromStrings(regionList);
                }
            }
        }

        if (config.contains("region.cooldowns")) {
            ConfigurationSection cooldownSection = config.getConfigurationSection("region.cooldowns");
            if (cooldownSection != null) {
                Map<String, Double> cooldowns = new HashMap<>();
                for (String regionName : cooldownSection.getKeys(false)) {
                    Object cooldownValue = cooldownSection.get(regionName);
                    if (cooldownValue instanceof Number) {
                        cooldowns.put(regionName, ((Number) cooldownValue).doubleValue());
                    } else if (cooldownValue instanceof String) {
                        try {
                            cooldowns.put(regionName, Double.parseDouble((String) cooldownValue));
                        } catch (NumberFormatException e) {
                            cooldowns.put(regionName, (double) cooldownSection.getInt(regionName));
                        }
                    }
                }
                regionCooldowns(cooldowns);
            }
        }

        // NUEVO: Cargar triggerType desde la configuración
        if (config.contains("trigger-type")) {
            triggerType(config.getString("trigger-type"));
        }

        boolean autoDetectPlaceholders = false;
        String nameText = config.getString("name", "");
        List<String> loreList = config.getStringList("lore");

        if (containsPlaceholders(nameText) ||
                loreList.stream().anyMatch(this::containsPlaceholders)) {
            autoDetectPlaceholders = true;
        }

        usePlaceholders(config.getBoolean("use-placeholders", autoDetectPlaceholders));

        if (config.contains("action-config")) {
            if (config.isConfigurationSection("action-config")) {
                Map<String, Object> actionConfigMap = new HashMap<>();
                ConfigurationSection actionConfigSection = config.getConfigurationSection("action-config");
                if (actionConfigSection != null) {
                    for (String key : actionConfigSection.getKeys(false)) {
                        actionConfigMap.put(key, actionConfigSection.get(key));
                    }
                actionConfig(actionConfigMap);

                if (actionConfigSection.contains("radius")) {
                    Object radiusValue = actionConfigSection.get("radius");
                    if (radiusValue instanceof Number) {
                        radius(((Number) radiusValue).doubleValue());
                    } else if (radiusValue instanceof String) {
                        try {
                            radius(Double.parseDouble((String) radiusValue));
                        } catch (NumberFormatException e) {
                            // Ignore invalid radius values
                        }
                    }
                }

                if (actionConfigSection.contains("affect-self")) {
                    affectSelf(actionConfigSection.getBoolean("affect-self"));
                }

                if (actionConfigSection.contains("only-players")) {
                    onlyPlayers(actionConfigSection.getBoolean("only-players"));
                }

                if (actionConfigSection.contains("require-line-of-sight")) {
                    requireLineOfSight(actionConfigSection.getBoolean("require-line-of-sight"));
                }

                if (actionConfigSection.contains("max-targets")) {
                    maxTargets(actionConfigSection.getInt("max-targets"));
                }
                }
            }
        }

        if (config.contains("item-type")) {
            String itemType = config.getString("item-type", "user");
            if (itemType.equalsIgnoreCase("lobby")) {
                lobbyItem();
            } else if (itemType.equalsIgnoreCase("user")) {
                userItem();
            }
        }
        return this;
    }

    protected boolean containsPlaceholders(String text) {
        return text != null && (text.contains("%") || text.contains("{") || text.contains("<"));
    }

    public ItemConfiguration build() {
        return new ItemConfiguration(this);
    }

    public static ItemConfigurationBuilder fromConfig(ConfigurationSection config) {
        return new ItemConfigurationBuilder().loadFromConfig(config);
    }
}