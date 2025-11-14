package net.exylia.commons.item.config;

import net.exylia.commons.item.ExpirationBehavior;
import net.exylia.commons.items.model.ItemData;
import net.exylia.commons.items.processor.ConfigurationParser;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ItemConfigurationBuilder {

    protected ItemData itemData = ItemData.builder().build();

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

    protected ConfigurationSection effectsOnUse = null;

    protected Map<String, Object> actionConfig = new HashMap<>();
    protected boolean usePlaceholders = false;

    protected RegionFilterType regionType = RegionFilterType.NONE;
    protected RegionCheckerType regionChecker = RegionCheckerType.CONTAINS;
    protected List<RegionEntry> regionEntries = new ArrayList<>();
    protected List<String> regionList = new ArrayList<>();
    protected Map<String, Double> regionCooldowns = new HashMap<>();

    protected WorldFilterType worldType = WorldFilterType.NONE;
    protected List<WorldEntry> worldEntries = new ArrayList<>();
    protected List<String> worldList = new ArrayList<>();
    protected Map<String, Double> worldCooldowns = new HashMap<>();

    protected TriggerType triggerType = TriggerType.IMMEDIATE;
    protected String forceId = null;

    protected long expirationTimeMillis = 0L;
    protected String expirationBehavior = "keep";

    public ItemConfigurationBuilder material(String material) {
        itemData.setRawMaterial(material);
        return this;
    }

    public ItemConfigurationBuilder name(String name) {
        itemData.setRawName(name);
        return this;
    }

    public ItemConfigurationBuilder displayName(String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            itemData.setRawDisplayName(displayName);
        }
        return this;
    }

    public ItemConfigurationBuilder lore(List<String> lore) {
        itemData.setRawLore(new ArrayList<>(lore));
        return this;
    }

    public ItemConfigurationBuilder lore(String... lore) {
        itemData.setRawLore(new ArrayList<>(Arrays.asList(lore)));
        return this;
    }

    public ItemConfigurationBuilder amount(int amount) {
        itemData.setRawAmount(String.valueOf(amount));
        return this;
    }

    public ItemConfigurationBuilder glowing(boolean glowing) {
        itemData.setGlowing(glowing);
        return this;
    }

    public ItemConfigurationBuilder hideAttributes(boolean hideAttributes) {
        itemData.setHideAttributes(hideAttributes);
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
        this.commands = new ArrayList<>(Arrays.asList(commands));
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

    public ItemConfigurationBuilder effectsOnUse(ConfigurationSection effectsSection) {
        this.effectsOnUse = effectsSection;
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

    public ItemConfigurationBuilder forceId(String forceId) {
        this.forceId = forceId;
        return this;
    }

    public ItemConfigurationBuilder enchantments(Map<Enchantment, Integer> enchantments) {
        itemData.getRawEnchantments().clear();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            itemData.getRawEnchantments().put(entry.getKey().getKey().getKey(), entry.getValue());
        }
        return this;
    }

    public ItemConfigurationBuilder enchantment(Enchantment enchantment, int level) {
        itemData.getRawEnchantments().put(enchantment.getKey().getKey(), level);
        return this;
    }

    public ItemConfigurationBuilder addEnchantment(Enchantment enchantment, int level) {
        itemData.getRawEnchantments().put(enchantment.getKey().getKey(), level);
        return this;
    }

    public ItemConfigurationBuilder removeEnchantment(Enchantment enchantment) {
        itemData.getRawEnchantments().remove(enchantment.getKey().getKey());
        return this;
    }

    public ItemConfigurationBuilder clearEnchantments() {
        itemData.getRawEnchantments().clear();
        return this;
    }

    public ItemConfigurationBuilder expirationTime(long expirationTimeMillis) {
        this.expirationTimeMillis = expirationTimeMillis;
        return this;
    }

    public ItemConfigurationBuilder expirationFromNow(long durationMillis) {
        this.expirationTimeMillis = System.currentTimeMillis() + durationMillis;
        return this;
    }

    public ItemConfigurationBuilder noExpiration() {
        this.expirationTimeMillis = 0L;
        return this;
    }

    public ItemConfigurationBuilder expirationDate(String dateString) {
        try {
            long timestamp = parseDateString(dateString);
            this.expirationTimeMillis = timestamp;
        } catch (DateTimeParseException e) {
            DebugUtils.logInternalError("Invalid expiration date format: " + dateString + ". Use formats like '24/12/2025 15:00' or '2025-12-24 15:00:00'");
        }
        return this;
    }

    public ItemConfigurationBuilder expirationBehavior(ExpirationBehavior behavior) {
        this.expirationBehavior = behavior != null ? behavior.getConfigName() : ExpirationBehavior.KEEP.getConfigName();
        return this;
    }

    public ItemConfigurationBuilder expirationBehavior(String behaviorString) {
        ExpirationBehavior behavior = ExpirationBehavior.fromString(behaviorString);
        this.expirationBehavior = behavior.getConfigName();
        return this;
    }

    public ItemConfigurationBuilder expirationWithBehavior(long expirationTimeMillis, ExpirationBehavior behavior) {
        this.expirationTimeMillis = expirationTimeMillis;
        this.expirationBehavior = behavior != null ? behavior.getConfigName() : ExpirationBehavior.KEEP.getConfigName();
        return this;
    }

    public ItemConfigurationBuilder expirationFromNowWithBehavior(long durationMillis, ExpirationBehavior behavior) {
        this.expirationTimeMillis = System.currentTimeMillis() + durationMillis;
        this.expirationBehavior = behavior != null ? behavior.getConfigName() : ExpirationBehavior.KEEP.getConfigName();
        return this;
    }

    public ItemConfigurationBuilder expirationDateWithBehavior(String dateString, ExpirationBehavior behavior) {
        try {
            long timestamp = parseDateString(dateString);
            this.expirationTimeMillis = timestamp;
            this.expirationBehavior = behavior != null ? behavior.getConfigName() : ExpirationBehavior.KEEP.getConfigName();
        } catch (DateTimeParseException e) {
            DebugUtils.logInternalError("Invalid expiration date format: " + dateString + ". Use formats like '24/12/2025 15:00'");
        }
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
                .toList();
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
                .toList();
        return this;
    }

    public ItemConfigurationBuilder regionList(String... regions) {
        return regionList(Arrays.asList(regions));
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

    public ItemConfigurationBuilder worldType(WorldFilterType type) {
        this.worldType = type != null ? type : WorldFilterType.NONE;
        return this;
    }

    public ItemConfigurationBuilder worldType(String typeString) {
        this.worldType = WorldFilterType.fromString(typeString);
        return this;
    }

    public ItemConfigurationBuilder worldEntries(List<WorldEntry> entries) {
        this.worldEntries = new ArrayList<>(entries);
        this.worldList = entries.stream()
                .map(WorldEntry::getWorldName)
                .toList();
        return this;
    }

    public ItemConfigurationBuilder worldEntriesFromStrings(List<String> entryStrings) {
        this.worldEntries = new ArrayList<>();
        this.worldList = new ArrayList<>();

        for (String entryString : entryStrings) {
            try {
                WorldEntry entry = WorldEntry.parse(entryString);
                this.worldEntries.add(entry);
                this.worldList.add(entry.getWorldName());
            } catch (IllegalArgumentException e) {
                DebugUtils.logInternalError("Warning: Invalid world entry '" + entryString + "': " + e.getMessage());
            }
        }
        return this;
    }

    public ItemConfigurationBuilder addWorldEntry(WorldEntry entry) {
        if (!this.worldEntries.contains(entry)) {
            this.worldEntries.add(entry);
            if (!this.worldList.contains(entry.getWorldName())) {
                this.worldList.add(entry.getWorldName());
            }
        }
        return this;
    }

    public ItemConfigurationBuilder addWorldEntry(String entryString) {
        try {
            WorldEntry entry = WorldEntry.parse(entryString);
            return addWorldEntry(entry);
        } catch (IllegalArgumentException e) {
            DebugUtils.logInternalError("Warning: Invalid world entry '" + entryString + "': " + e.getMessage());
            return this;
        }
    }

    public ItemConfigurationBuilder worldList(List<String> worlds) {
        this.worldList = new ArrayList<>(worlds);
        this.worldEntries = worlds.stream()
                .map(WorldEntry::forWorld)
                .toList();
        return this;
    }

    public ItemConfigurationBuilder worldList(String... worlds) {
        return worldList(Arrays.asList(worlds));
    }

    public ItemConfigurationBuilder addWorld(String worldName) {
        if (!this.worldList.contains(worldName)) {
            this.worldList.add(worldName);
            this.worldEntries.add(WorldEntry.forWorld(worldName));
        }
        return this;
    }

    public ItemConfigurationBuilder worldCooldowns(Map<String, Double> cooldowns) {
        this.worldCooldowns = new HashMap<>(cooldowns);
        return this;
    }

    public ItemConfigurationBuilder worldCooldown(String worldName, double cooldownSeconds) {
        this.worldCooldowns.put(worldName, cooldownSeconds);
        return this;
    }

    public ItemConfigurationBuilder worldCooldown(String worldName, int cooldownSeconds) {
        this.worldCooldowns.put(worldName, (double) cooldownSeconds);
        return this;
    }

    public ItemConfigurationBuilder whitelistWorlds(String... worlds) {
        return worldType(WorldFilterType.WHITELIST)
                .worldList(worlds);
    }

    public ItemConfigurationBuilder blacklistWorlds(String... worlds) {
        return worldType(WorldFilterType.BLACKLIST)
                .worldList(worlds);
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

    public ItemConfigurationBuilder hitCount(int hitCount) {
        return actionConfigValue("hit-count", hitCount);
    }

    public ItemConfigurationBuilder hitPeriod(int hitPeriod) {
        return actionConfigValue("hit-period", hitPeriod);
    }

    public ItemConfigurationBuilder multipleHitConfig(int hitCount, int hitPeriod) {
        return hitCount(hitCount).hitPeriod(hitPeriod);
    }

    public ItemConfigurationBuilder loadFromConfig(ConfigurationSection config) {
        if (config == null) {
            return this;
        }

        ItemData parsedItemData = ConfigurationParser.parseFromConfig(config);
        this.itemData = parsedItemData;

        parseInteractiveItemSpecificConfig(config);

        detectAndSetPlaceholders();

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

    private void parseInteractiveItemSpecificConfig(ConfigurationSection config) {
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
            parseAndSetCooldown(config);
        }

        if (config.contains("effects-on-use")) {
            effectsOnUse(config.getConfigurationSection("effects-on-use"));
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

        parseRegionConfig(config);
        parseWorldConfig(config);
        parseTriggerAndIdConfig(config);
        parseActionConfig(config);
    }

    private void parseAndSetCooldown(ConfigurationSection config) {
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

    private void parseRegionConfig(ConfigurationSection config) {
        if (config.contains("region.type")) {
            regionType(config.getString("region.type"));
        }

        if (config.contains("region.checker")) {
            regionChecker(config.getString("region.checker"));
        }

        if (config.contains("region.list")) {
            parseRegionList(config);
        }

        if (config.contains("region.cooldowns")) {
            parseRegionCooldowns(config);
        }
    }

    private void parseRegionList(ConfigurationSection config) {
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

    private void parseRegionCooldowns(ConfigurationSection config) {
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

    private void parseWorldConfig(ConfigurationSection config) {
        if (config.contains("world.type")) {
            worldType(config.getString("world.type"));
        }

        if (config.contains("world.list")) {
            parseWorldList(config);
        }

        if (config.contains("world.cooldowns")) {
            parseWorldCooldowns(config);
        }
    }

    private void parseWorldList(ConfigurationSection config) {
        if (config.isList("world.list")) {
            List<String> worldStrings = config.getStringList("world.list");
            worldEntriesFromStrings(worldStrings);
        } else {
            String worldString = config.getString("world.list");
            if (worldString != null && !worldString.trim().isEmpty()) {
                String[] worlds = worldString.split(",");
                List<String> worldList = new ArrayList<>();
                for (String world : worlds) {
                    String trimmed = world.trim();
                    if (!trimmed.isEmpty()) {
                        worldList.add(trimmed);
                    }
                }
                worldEntriesFromStrings(worldList);
            }
        }
    }

    private void parseWorldCooldowns(ConfigurationSection config) {
        ConfigurationSection cooldownSection = config.getConfigurationSection("world.cooldowns");
        if (cooldownSection != null) {
            Map<String, Double> cooldowns = new HashMap<>();
            for (String worldName : cooldownSection.getKeys(false)) {
                Object cooldownValue = cooldownSection.get(worldName);
                if (cooldownValue instanceof Number) {
                    cooldowns.put(worldName, ((Number) cooldownValue).doubleValue());
                } else if (cooldownValue instanceof String) {
                    try {
                        cooldowns.put(worldName, Double.parseDouble((String) cooldownValue));
                    } catch (NumberFormatException e) {
                        cooldowns.put(worldName, (double) cooldownSection.getInt(worldName));
                    }
                }
            }
            worldCooldowns(cooldowns);
        }
    }

    private void parseTriggerAndIdConfig(ConfigurationSection config) {
        if (config.contains("trigger-type")) {
            triggerType(config.getString("trigger-type"));
        }

        if (config.contains("force-id")) {
            forceId(config.getString("force-id"));
        }
    }

    private void parseActionConfig(ConfigurationSection config) {
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
                        parseRadiusConfig(actionConfigSection);
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

                    if (actionConfigSection.contains("hit-count")) {
                        hitCount(actionConfigSection.getInt("hit-count"));
                    }

                    if (actionConfigSection.contains("hit-period")) {
                        hitPeriod(actionConfigSection.getInt("hit-period"));
                    }
                }
            }
        }
    }

    private void parseRadiusConfig(ConfigurationSection actionConfigSection) {
        Object radiusValue = actionConfigSection.get("radius");
        if (radiusValue instanceof Number) {
            radius(((Number) radiusValue).doubleValue());
        } else if (radiusValue instanceof String) {
            try {
                radius(Double.parseDouble((String) radiusValue));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void detectAndSetPlaceholders() {
        if (ConfigurationParser.detectPlaceholdersInItemData(itemData)) {
            usePlaceholders(true);
        }
    }

    private long parseDateString(String dateString) throws DateTimeParseException {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new DateTimeParseException("Empty date string", dateString, 0);
        }

        dateString = dateString.trim();

        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new DateTimeParseException("Unable to parse date: " + dateString, dateString, 0);
    }

    public ItemConfiguration build() {
        return new ItemConfiguration(this);
    }

    public static ItemConfigurationBuilder fromConfig(ConfigurationSection config) {
        return new ItemConfigurationBuilder().loadFromConfig(config);
    }
}
