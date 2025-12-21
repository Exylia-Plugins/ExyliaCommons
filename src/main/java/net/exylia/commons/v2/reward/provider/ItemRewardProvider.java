package net.exylia.commons.v2.reward.provider;

import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.reward.config.ItemRewardConfig;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ItemRewardProvider implements RewardProvider {

    @Override
    public CompletableFuture<RewardResult> provide(Reward reward, RewardContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ItemRewardConfig config = (ItemRewardConfig) reward.getData();

                ItemStack item = createItemStack(config, context);

                Schedulers.sync(() -> {
                    context.getPlayer().getInventory().addItem(item);
                });

                return RewardResult.success(reward);
            } catch (Exception e) {
                return RewardResult.builder()
                        .success(false)
                        .reward(reward)
                        .error(e)
                        .message("Failed to give item: " + e.getMessage())
                        .build();
            }
        });
    }

    private ItemStack createItemStack(ItemRewardConfig config, RewardContext context) {
        String processedMaterial = Placeholders.process(
                config.getMaterial(),
                context.getPlayer(),
                context.getPlaceholderContext()
        );

        Material material = Material.valueOf(processedMaterial.toUpperCase());
        ItemStack item = new ItemStack(material);
        item.setAmount(config.getAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.getName() != null) {
                String processedName = Placeholders.process(
                        config.getName(),
                        context.getPlayer(),
                        context.getPlaceholderContext()
                );
                meta.displayName(ColorAPI.parse(processedName));
            }

            if (config.getLore() != null && !config.getLore().isEmpty()) {
                List<String> processedLore = config.getLore().stream()
                        .map(line -> Placeholders.process(
                                line,
                                context.getPlayer(),
                                context.getPlaceholderContext()
                        ))
                        .collect(Collectors.toList());

                meta.lore(ColorAPI.parse(processedLore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}
