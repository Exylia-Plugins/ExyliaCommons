package net.exylia.commons.v2.reward.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.reward.config.ItemRewardConfig;
import net.exylia.commons.v2.reward.core.RewardManager;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;
import net.exylia.commons.v2.reward.model.RewardType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RewardBuilder {

    private RewardType type;
    private Object data;
    private double chance = 100.0;
    private String condition;
    private String permission;
    private String message;
    private int priority = 0;

    public RewardBuilder command(String command) {
        this.type = RewardType.COMMAND;
        this.data = command;
        return this;
    }

    public RewardBuilder message(String message) {
        this.type = RewardType.MESSAGE;
        this.data = message;
        return this;
    }

    public RewardBuilder item(String material, int amount) {
        this.type = RewardType.ITEM;
        this.data = ItemRewardConfig.builder()
                .material(material)
                .amount(amount)
                .build();
        return this;
    }

    public RewardBuilder item(ItemRewardConfig itemConfig) {
        this.type = RewardType.ITEM;
        this.data = itemConfig;
        return this;
    }

    public RewardBuilder chance(double chance) {
        this.chance = chance;
        return this;
    }

    public RewardBuilder condition(String condition) {
        this.condition = condition;
        return this;
    }

    public RewardBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public RewardBuilder successMessage(String message) {
        this.message = message;
        return this;
    }

    public RewardBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public Reward build() {
        if (type == null || data == null) {
            throw new IllegalStateException("Reward type and data must be set");
        }

        return Reward.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .data(data)
                .chance(chance)
                .condition(condition)
                .permission(permission)
                .message(message)
                .priority(priority)
                .build();
    }

    public CompletableFuture<RewardResult> give(Player player) {
        return give(player, PlaceholderContext.create());
    }

    public CompletableFuture<RewardResult> give(Player player, PlaceholderContext context) {
        Reward reward = build();

        RewardContext rewardContext = RewardContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();

        return RewardManager.getInstance().giveSingle(reward, rewardContext);
    }
}
