package net.exylia.commons.item.handlers;

import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.FireworkUtils;
import net.exylia.commons.utils.ParticleUtils;
import net.exylia.commons.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Manejador de efectos visuales y sonoros de los items
 */
public class ItemEffectsHandler {

    /**
     * Ejecuta todos los efectos configurados para un item
     * @param player Jugador que activó el item
     * @param location Ubicación donde ejecutar los efectos
     * @param config Configuración del item
     */
    public static void executeEffects(Player player, Location location, ItemConfiguration config) {
        if (config.hasSound()) {
            executeSound(player, config.getSoundOnUse());
        }

        if (config.hasParticles()) {
            executeParticles(location, config.getParticlesOnUse());
        }

        if (config.hasFirework()) {
            executeFirework(location, config);
        }
    }

    /**
     * Ejecuta el sonido configurado
     * @param player Jugador que escuchará el sonido
     * @param soundConfig Configuración del sonido
     */
    private static void executeSound(Player player, String soundConfig) {
        try {
            SoundUtils.playSound(player, soundConfig);
        } catch (Exception e) {
            // Log error pero no interrumpir el flujo
            DebugUtils.logInternalError("Error playing sound: " + soundConfig + " - " + e.getMessage());
        }
    }

    /**
     * Ejecuta las partículas configuradas
     * @param location Ubicación donde generar las partículas
     * @param particleConfig Configuración de las partículas
     */
    private static void executeParticles(Location location, String particleConfig) {
        try {
            ParticleUtils.spawnParticles(location, particleConfig);
        } catch (Exception e) {
            // Log error pero no interrumpir el flujo
            DebugUtils.logInternalError("Error spawning particles: " + particleConfig + " - " + e.getMessage());
        }
    }

    /**
     * Ejecuta los fuegos artificiales configurados
     * @param location Ubicación donde lanzar los fuegos artificiales
     * @param config Configuración del item
     */
    private static void executeFirework(Location location, ItemConfiguration config) {
        try {
            Location fireworkLocation = location.clone().add(0, 1, 0);

            if (config.isLaunchFireworkOnUse()) {
                FireworkUtils.launchRandomFirework(fireworkLocation);
            } else if (config.getFireworkOnUse() != null && !config.getFireworkOnUse().trim().isEmpty()) {
                FireworkUtils.launchFirework(fireworkLocation, config.getFireworkOnUse());
            }
        } catch (Exception e) {
            // Log error pero no interrumpir el flujo
            DebugUtils.logInternalError("Error launching firework - " + e.getMessage());
        }
    }
}