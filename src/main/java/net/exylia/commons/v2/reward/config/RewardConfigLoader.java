package net.exylia.commons.v2.reward.config;

import net.exylia.commons.v2.reward.model.RewardType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class RewardConfigLoader {

    public List<RewardConfig> load(ConfigurationSection section) {
        return loadFromKey(section, "rewards");
    }

    public List<RewardConfig> loadFromKey(ConfigurationSection section, String key) {
        List<RewardConfig> rewards = new ArrayList<>();

        if (section == null) {
            return rewards;
        }

        if (section.isList(key)) {
            List<?> rawList = section.getList(key);
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof ConfigurationSection rewardSection) {
                        rewards.add(loadSingle(rewardSection));
                    } else if (obj instanceof String rawReward) {
                        rewards.add(parseInlineReward(rawReward));
                    }
                }
            }
        } else if (section.isConfigurationSection(key)) {
            ConfigurationSection rewardsSection = section.getConfigurationSection(key);
            if (rewardsSection != null) {
                rewards.addAll(loadFromSection(rewardsSection));
            }
        }

        return rewards;
    }

    private List<RewardConfig> loadFromSection(ConfigurationSection section) {
        List<RewardConfig> rewards = new ArrayList<>();

        if (section.contains("commands")) {
            rewards.addAll(loadCommands(section));
        }

        if (section.contains("items")) {
            rewards.addAll(loadItems(section));
        }

        if (section.contains("messages")) {
            rewards.addAll(loadMessages(section));
        }

        return rewards;
    }

    private List<RewardConfig> loadCommands(ConfigurationSection section) {
        List<RewardConfig> rewards = new ArrayList<>();

        if (section.isList("commands")) {
            List<?> commands = section.getList("commands");
            if (commands != null) {
                for (Object obj : commands) {
                    if (obj instanceof String command) {
                        rewards.add(RewardConfig.builder()
                                .type(RewardType.COMMAND)
                                .rawData(command)
                                .build());
                    } else if (obj instanceof ConfigurationSection cmdSection) {
                        rewards.add(loadCommandConfig(cmdSection));
                    }
                }
            }
        }

        return rewards;
    }

    private RewardConfig loadCommandConfig(ConfigurationSection section) {
        return RewardConfig.builder()
                .type(RewardType.COMMAND)
                .rawData(section.getString("command"))
                .chance(section.getDouble("chance", 100.0))
                .condition(section.getString("condition"))
                .message(section.getString("message"))
                .priority(section.getInt("priority", 0))
                .build();
    }

    private List<RewardConfig> loadItems(ConfigurationSection section) {
        List<RewardConfig> rewards = new ArrayList<>();

        if (section.isList("items")) {
            List<?> items = section.getList("items");
            if (items != null) {
                for (Object obj : items) {
                    if (obj instanceof ConfigurationSection itemSection) {
                        rewards.add(loadItemConfig(itemSection));
                    }
                }
            }
        }

        return rewards;
    }

    private RewardConfig loadItemConfig(ConfigurationSection section) {
        ItemRewardConfig itemConfig = ItemRewardConfig.builder()
                .material(section.getString("material", "STONE"))
                .amount(section.getInt("amount", 1))
                .name(section.getString("name"))
                .lore(section.getStringList("lore"))
                .build();

        return RewardConfig.builder()
                .type(RewardType.ITEM)
                .rawData(itemConfig)
                .chance(section.getDouble("chance", 100.0))
                .condition(section.getString("condition"))
                .message(section.getString("message"))
                .priority(section.getInt("priority", 0))
                .build();
    }

    private List<RewardConfig> loadMessages(ConfigurationSection section) {
        List<RewardConfig> rewards = new ArrayList<>();

        if (section.isList("messages")) {
            List<String> messages = section.getStringList("messages");
            for (String message : messages) {
                rewards.add(RewardConfig.builder()
                        .type(RewardType.MESSAGE)
                        .rawData(message)
                        .build());
            }
        }

        return rewards;
    }

    private RewardConfig loadSingle(ConfigurationSection section) {
        String typeStr = section.getString("type", "COMMAND");
        RewardType type = RewardType.valueOf(typeStr.toUpperCase());

        return switch (type) {
            case COMMAND -> loadCommandConfig(section);
            case ITEM -> loadItemConfig(section);
            case MESSAGE -> RewardConfig.builder()
                    .type(RewardType.MESSAGE)
                    .rawData(section.getString("message"))
                    .chance(section.getDouble("chance", 100.0))
                    .condition(section.getString("condition"))
                    .priority(section.getInt("priority", 0))
                    .build();
        };
    }

    private RewardConfig parseInlineReward(String raw) {
        if (raw.startsWith("command:")) {
            return RewardConfig.builder()
                    .type(RewardType.COMMAND)
                    .rawData(raw.substring(8).trim())
                    .build();
        } else if (raw.startsWith("message:")) {
            return RewardConfig.builder()
                    .type(RewardType.MESSAGE)
                    .rawData(raw.substring(8).trim())
                    .build();
        } else {
            return RewardConfig.builder()
                    .type(RewardType.COMMAND)
                    .rawData(raw)
                    .build();
        }
    }
}
