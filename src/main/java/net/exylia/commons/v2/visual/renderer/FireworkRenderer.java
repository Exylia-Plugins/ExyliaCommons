package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkRenderer implements VisualRenderer<FireworkConfig> {
    private static final FireworkRenderer INSTANCE = new FireworkRenderer();

    private FireworkRenderer() {
    }

    public static FireworkRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, FireworkConfig config, PlaceholderContext context) {
        Location location = config.getLocation() != null ? config.getLocation() : player.getLocation();

        if (location.getWorld() == null) {
            return;
        }

        FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                .with(config.getType())
                .withColor(config.getColors())
                .flicker(config.isFlicker())
                .trail(config.isTrail());

        if (config.getFadeColors() != null && !config.getFadeColors().isEmpty()) {
            effectBuilder.withFade(config.getFadeColors());
        }

        FireworkEffect effect = effectBuilder.build();

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(effect);
        meta.setPower(config.getPower());
        firework.setFireworkMeta(meta);
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
