package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.config.Messages;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.visual.cache.CacheManager;
import net.exylia.commons.v2.visual.color.MessageCenterer;
import net.exylia.commons.v2.visual.config.MessageConfig;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.processor.EffectProcessor;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MessageRenderer implements VisualRenderer<MessageConfig> {
    private static final MessageRenderer INSTANCE = new MessageRenderer();

    private MessageRenderer() {
    }

    public static MessageRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, MessageConfig config, PlaceholderContext context) {
        List<Component> components = processMessages(player, config, context);

        if (config.isBroadcast()) {
            sendToAll(components, config);
        } else if (config.getFilter() != null) {
            sendToFiltered(components, config);
        } else if (config.getRecipients() != null) {
            sendToRecipients(components, config);
        } else if (player != null && player.isOnline()) {
            sendToPlayer(player, components);
        }
    }

    private List<Component> processMessages(Player player, MessageConfig config, PlaceholderContext context) {
        List<Component> components = new ArrayList<>();
        List<String> messages = config.getMessages();

        for (String message : messages) {
            String prefix = Messages.getPrefix();
            message = message.replace("%prefix%", prefix != null ? prefix : "");

            EffectProcessor.ParsedMessage parsed = EffectProcessor.parse(message, player, context);

            if (!parsed.getSounds().isEmpty()) {
                for (SoundConfig sound : parsed.getSounds()) {
                    VisualManager.getInstance().playSound(player, sound, context);
                }
            }
            if (!parsed.getParticles().isEmpty()) {
                for (ParticleConfig particle : parsed.getParticles()) {
                    VisualManager.getInstance().spawnParticle(player, particle, context);
                }
            }
            if (!parsed.getFireworks().isEmpty()) {
                for (FireworkConfig firework : parsed.getFireworks()) {
                    VisualManager.getInstance().launchFirework(player, firework, context);
                }
            }
            if (!parsed.getEffects().isEmpty()) {
                for (EffectConfig effect : parsed.getEffects()) {
                    VisualManager.getInstance().applyEffect(player, effect, context);
                }
            }

            String processed = CacheManager.getInstance().processPlaceholders(parsed.getCleanMessage(), player, context);

            if (config.isCentered() || parsed.isCentered()) {
                processed = MessageCenterer.center(processed);
            }

            Component component = ColorAPI.parse(processed);
            components.add(component);
        }

        return components;
    }

    private void sendToPlayer(Player player, List<Component> components) {
        for (Component component : components) {
            player.sendMessage(component);
        }
    }

    private void sendToRecipients(List<Component> components, MessageConfig config) {
        Collection<Player> recipients = config.getRecipients();
        Collection<Player> excluded = config.getExcluded();

        for (Player player : recipients) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            if (player.isOnline()) {
                sendToPlayer(player, components);
            }
        }
    }

    private void sendToAll(List<Component> components, MessageConfig config) {
        Collection<Player> excluded = config.getExcluded();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            sendToPlayer(player, components);
        }
    }

    private void sendToFiltered(List<Component> components, MessageConfig config) {
        Collection<Player> excluded = config.getExcluded();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (excluded != null && excluded.contains(player)) {
                continue;
            }
            if (config.getFilter().test(player)) {
                sendToPlayer(player, components);
            }
        }
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
