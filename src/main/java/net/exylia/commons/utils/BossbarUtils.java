package net.exylia.commons.utils;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossbarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, BossbarData>> playerBossbars = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    /**
     * Crea una bossbar desde configuración con tiempo específico
     * @param player Jugador destinatario
     * @param section Sección de configuración de la bossbar
     * @param timeInSeconds Tiempo en segundos
     * @param replacements Reemplazos para el texto (opcional)
     * @return ID único de la bossbar creada
     */
    public static String createTimedBossbar(Player player, ConfigurationSection section, double timeInSeconds, String... replacements) {
        if (section == null || !section.getBoolean("enabled", true)) return null;

        String id = generateBossbarId();
        String text = section.getString("text", "");

        // Aplicar reemplazos
        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                text = text.replace(replacements[i], replacements[i + 1]);
            }
        }

        BossBar.Color color = parseColor(section.getString("color", "BLUE"));
        BossBar.Overlay overlay = parseOverlay(section.getString("overlay", "PROGRESS"));
        boolean decreasing = section.getBoolean("decreasing", true);
        boolean autoHide = section.getBoolean("autoHide", true);

        return createTimedBossbar(player, id, text, color, overlay, timeInSeconds, decreasing, autoHide);
    }

    /**
     * Crea una bossbar con tiempo específico
     * @param player Jugador destinatario
     * @param id ID único para la bossbar
     * @param text Texto de la bossbar
     * @param color Color de la bossbar
     * @param overlay Estilo de la bossbar
     * @param timeInSeconds Tiempo en segundos
     * @param decreasing Si la barra debe disminuir (true) o aumentar (false)
     * @param autoHide Si la barra debe ocultarse automáticamente al terminar
     * @return ID de la bossbar creada
     */
    public static String createTimedBossbar(Player player, String id, String text, BossBar.Color color,
                                            BossBar.Overlay overlay, double timeInSeconds, boolean decreasing, boolean autoHide) {
        if (id == null) id = generateBossbarId();

        BossBar bossBar = MessageUtils.createBossBar(text, color, overlay);
        bossBar.progress(decreasing ? 1.0f : 0.0f);

        MessageUtils.showPlayerBossBar(player, bossBar);

        long totalTicks = (long) (timeInSeconds * 20);
        long updateInterval = Math.max(1, totalTicks / 100); // Actualizar hasta 100 veces para suavidad

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private long ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelBossbar(player, finalId);
                    return;
                }

                ticksElapsed += updateInterval;
                double progress = Math.min(1.0, (double) ticksElapsed / totalTicks);

                if (decreasing) {
                    bossBar.progress((float) (1.0 - progress));
                } else {
                    bossBar.progress((float) progress);
                }

                if (progress >= 1.0) {
                    if (autoHide) {
                        cancelBossbar(player, finalId);
                    } else {
                        this.cancel();
                        removeBossbarData(player, finalId);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeBossbarData(player, id, new BossbarData(bossBar, task, BossbarType.TIMED));
        return id;
    }

    /**
     * Crea una bossbar estática (sin tiempo)
     * @param player Jugador destinatario
     * @param section Sección de configuración
     * @param replacements Reemplazos para el texto
     * @return ID de la bossbar creada
     */
    public static String createStaticBossbar(Player player, ConfigurationSection section, String... replacements) {
        if (section == null || !section.getBoolean("enabled", true)) return null;

        String id = generateBossbarId();
        String text = section.getString("text", "");

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                text = text.replace(replacements[i], replacements[i + 1]);
            }
        }

        BossBar.Color color = parseColor(section.getString("color", "BLUE"));
        BossBar.Overlay overlay = parseOverlay(section.getString("overlay", "PROGRESS"));
        float progress = (float) section.getDouble("progress", 1.0);

        return createStaticBossbar(player, id, text, color, overlay, progress);
    }

    /**
     * Crea una bossbar estática
     * @param player Jugador destinatario
     * @param id ID único
     * @param text Texto
     * @param color Color
     * @param overlay Estilo
     * @param progress Progreso (0.0 - 1.0)
     * @return ID de la bossbar
     */
    public static String createStaticBossbar(Player player, String id, String text, BossBar.Color color,
                                             BossBar.Overlay overlay, float progress) {
        if (id == null) id = generateBossbarId();

        BossBar bossBar = MessageUtils.createBossBar(text, color, overlay);
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));

        MessageUtils.showPlayerBossBar(player, bossBar);
        storeBossbarData(player, id, new BossbarData(bossBar, null, BossbarType.STATIC));
        return id;
    }

    /**
     * Crea una bossbar animada (que se actualiza continuamente)
     * @param player Jugador destinatario
     * @param section Sección de configuración
     * @param replacements Reemplazos para el texto
     * @return ID de la bossbar creada
     */
    public static String createAnimatedBossbar(Player player, ConfigurationSection section, String... replacements) {
        if (section == null || !section.getBoolean("enabled", true)) return null;

        String id = generateBossbarId();
        String text = section.getString("text", "");

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                text = text.replace(replacements[i], replacements[i + 1]);
            }
        }

        BossBar.Color color = parseColor(section.getString("color", "BLUE"));
        BossBar.Overlay overlay = parseOverlay(section.getString("overlay", "PROGRESS"));
        long updateInterval = section.getLong("updateInterval", 20); // ticks
        double speed = section.getDouble("animationSpeed", 0.02); // cambio por tick
        boolean oscillate = section.getBoolean("oscillate", true); // ida y vuelta

        return createAnimatedBossbar(player, id, text, color, overlay, updateInterval, speed, oscillate);
    }

    /**
     * Crea una bossbar animada
     * @param player Jugador destinatario
     * @param id ID único
     * @param text Texto
     * @param color Color
     * @param overlay Estilo
     * @param updateInterval Intervalo de actualización en ticks
     * @param speed Velocidad de la animación
     * @param oscillate Si debe oscilar (ida y vuelta)
     * @return ID de la bossbar
     */
    public static String createAnimatedBossbar(Player player, String id, String text, BossBar.Color color,
                                               BossBar.Overlay overlay, long updateInterval, double speed, boolean oscillate) {
        if (id == null) id = generateBossbarId();

        BossBar bossBar = MessageUtils.createBossBar(text, color, overlay);
        bossBar.progress(0.0f);

        MessageUtils.showPlayerBossBar(player, bossBar);

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private double progress = 0.0;
            private boolean increasing = true;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelBossbar(player, finalId);
                    return;
                }

                if (oscillate) {
                    if (increasing) {
                        progress += speed;
                        if (progress >= 1.0) {
                            progress = 1.0;
                            increasing = false;
                        }
                    } else {
                        progress -= speed;
                        if (progress <= 0.0) {
                            progress = 0.0;
                            increasing = true;
                        }
                    }
                } else {
                    progress += speed;
                    if (progress > 1.0) {
                        progress = 0.0;
                    }
                }

                bossBar.progress((float) progress);
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeBossbarData(player, id, new BossbarData(bossBar, task, BossbarType.ANIMATED));
        return id;
    }

    /**
     * Actualiza el texto de una bossbar existente
     * @param player Jugador
     * @param id ID de la bossbar
     * @param newText Nuevo texto
     * @param replacements Reemplazos opcionales
     */
    public static void updateBossbarText(Player player, String id, String newText, String... replacements) {
        BossbarData data = getBossbarData(player, id);
        if (data == null) return;

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                newText = newText.replace(replacements[i], replacements[i + 1]);
            }
        }

        data.bossBar().name(ColorUtils.parse(newText));
    }

    /**
     * Actualiza el progreso de una bossbar estática
     * @param player Jugador
     * @param id ID de la bossbar
     * @param progress Nuevo progreso (0.0 - 1.0)
     */
    public static void updateBossbarProgress(Player player, String id, float progress) {
        BossbarData data = getBossbarData(player, id);
        if (data == null) return;

        data.bossBar().progress(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    /**
     * Actualiza el color de una bossbar
     * @param player Jugador
     * @param id ID de la bossbar
     * @param color Nuevo color
     */
    public static void updateBossbarColor(Player player, String id, BossBar.Color color) {
        BossbarData data = getBossbarData(player, id);
        if (data == null) return;

        data.bossBar().color(color);
    }

    /**
     * Cancela y elimina una bossbar específica
     * @param player Jugador
     * @param id ID de la bossbar
     * @return true si se canceló exitosamente
     */
    public static boolean cancelBossbar(Player player, String id) {
        BossbarData data = getBossbarData(player, id);
        if (data == null) return false;

        // Ocultar la bossbar
        MessageUtils.hidePlayerBossBar(player, data.bossBar());

        // Cancelar la tarea si existe
        if (data.task() != null && !data.task().isCancelled()) {
            data.task().cancel();
        }

        // Eliminar los datos
        removeBossbarData(player, id);
        return true;
    }

    /**
     * Cancela todas las bossbars de un jugador
     * @param player Jugador
     * @return Número de bossbars canceladas
     */
    public static int cancelAllBossbars(Player player) {
        Map<String, BossbarData> playerData = playerBossbars.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        for (String id : new HashSet<>(playerData.keySet())) {
            if (cancelBossbar(player, id)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Pausa una bossbar (solo afecta a las temporizadas y animadas)
     * @param player Jugador
     * @param id ID de la bossbar
     * @return true si se pausó exitosamente
     */
    public static boolean pauseBossbar(Player player, String id) {
        BossbarData data = getBossbarData(player, id);
        if (data == null || data.task() == null) return false;

        if (!data.task().isCancelled()) {
            data.task().cancel();
            return true;
        }
        return false;
    }

    /**
     * Obtiene todas las bossbars activas de un jugador
     * @param player Jugador
     * @return Set con los IDs de las bossbars activas
     */
    public static Set<String> getActiveBossbars(Player player) {
        Map<String, BossbarData> playerData = playerBossbars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Verifica si un jugador tiene una bossbar específica activa
     * @param player Jugador
     * @param id ID de la bossbar
     * @return true si la bossbar está activa
     */
    public static boolean hasBossbar(Player player, String id) {
        return getBossbarData(player, id) != null;
    }

    /**
     * Obtiene el progreso actual de una bossbar
     * @param player Jugador
     * @param id ID de la bossbar
     * @return Progreso actual (0.0 - 1.0) o -1 si no existe
     */
    public static float getBossbarProgress(Player player, String id) {
        BossbarData data = getBossbarData(player, id);
        return data != null ? data.bossBar().progress() : -1;
    }

    /**
     * Limpia todas las bossbars al descargar el plugin
     */
    public static void cleanup() {
        for (UUID playerId : playerBossbars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllBossbars(player);
            }
        }
        playerBossbars.clear();
    }

    // ============== MÉTODOS PRIVADOS ==============

    private static String generateBossbarId() {
        return "bossbar_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private static void storeBossbarData(Player player, String id, BossbarData data) {
        playerBossbars.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, data);
    }

    private static BossbarData getBossbarData(Player player, String id) {
        Map<String, BossbarData> playerData = playerBossbars.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeBossbarData(Player player, String id) {
        Map<String, BossbarData> playerData = playerBossbars.get(player.getUniqueId());
        if (playerData != null) {
            playerData.remove(id);
            if (playerData.isEmpty()) {
                playerBossbars.remove(player.getUniqueId());
            }
        }
    }

    private static BossBar.Color parseColor(String colorStr) {
        try {
            return BossBar.Color.valueOf(colorStr.toUpperCase());
        } catch (Exception e) {
            return BossBar.Color.BLUE;
        }
    }

    private static BossBar.Overlay parseOverlay(String overlayStr) {
        try {
            return BossBar.Overlay.valueOf(overlayStr.toUpperCase());
        } catch (Exception e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    // ============== CLASES INTERNAS ==============

    private record BossbarData(BossBar bossBar, BukkitTask task, BossbarType type) {}

    private enum BossbarType {
        STATIC,     // Bossbar fija sin animación
        TIMED,      // Bossbar con tiempo específico
        ANIMATED    // Bossbar con animación continua
    }
}