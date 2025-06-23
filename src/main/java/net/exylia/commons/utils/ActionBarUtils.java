package net.exylia.commons.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, ActionBarData>> playerActionBars = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    /**
     * Crea un action bar desde configuración
     * @param player Jugador destinatario
     * @param section Sección de configuración del action bar
     * @param replacements Reemplazos para el texto (opcional)
     * @return ID único del action bar creado
     */
    public static String createActionBar(Player player, ConfigurationSection section, String... replacements) {
        if (section == null || !section.getBoolean("enabled", true)) return null;

        String id = generateActionBarId();
        String text = section.getString("text", "");

        // Aplicar reemplazos
        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                text = text.replace(replacements[i], replacements[i + 1]);
            }
        }

        String type = section.getString("type", "single").toLowerCase();

        switch (type) {
            case "permanent":
            case "unlimited":
                long updateInterval = section.getLong("updateInterval", 20);
                return createPermanentActionBar(player, id, text, updateInterval);

            case "timed":
                double duration = section.getDouble("duration", 5.0);
                long interval = section.getLong("updateInterval", 20);
                return createTimedActionBar(player, id, text, duration, interval);

            case "animated":
                return createAnimatedActionBar(player, section, id, text);

            case "single":
            default:
                return createSingleActionBar(player, id, text);
        }
    }

    /**
     * Crea un action bar simple (se muestra una vez)
     * @param player Jugador destinatario
     * @param id ID único
     * @param text Texto del action bar
     * @return ID del action bar creado
     */
    public static String createSingleActionBar(Player player, String id, String text) {
        if (id == null) id = generateActionBarId();

        MessageUtils.sendActionBarAsync(player, text);

        // Almacenar datos para tracking (sin task)
        storeActionBarData(player, id, new ActionBarData(text, null, ActionBarType.SINGLE));

        // Auto-remover después de 3 segundos (tiempo típico de visualización)
        String finalId = id;
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeActionBarData(player, finalId), 60L);

        return id;
    }

    /**
     * Crea un action bar permanente (se repite indefinidamente)
     * @param player Jugador destinatario
     * @param id ID único
     * @param text Texto del action bar
     * @param updateInterval Intervalo de actualización en ticks
     * @return ID del action bar creado
     */
    public static String createPermanentActionBar(Player player, String id, String text, long updateInterval) {
        if (id == null) id = generateActionBarId();

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActionBar(player, finalId);
                    return;
                }
                MessageUtils.sendActionBarAsync(player, text);
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeActionBarData(player, id, new ActionBarData(text, task, ActionBarType.PERMANENT));
        return id;
    }

    /**
     * Crea un action bar temporal (se repite durante un tiempo específico)
     * @param player Jugador destinatario
     * @param id ID único
     * @param text Texto del action bar
     * @param durationInSeconds Duración en segundos
     * @param updateInterval Intervalo de actualización en ticks
     * @return ID del action bar creado
     */
    public static String createTimedActionBar(Player player, String id, String text, double durationInSeconds, long updateInterval) {
        if (id == null) id = generateActionBarId();

        long totalTicks = (long) (durationInSeconds * 20);
        long iterations = totalTicks / updateInterval;

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private long currentIteration = 0;

            @Override
            public void run() {
                if (!player.isOnline() || currentIteration >= iterations) {
                    cancelActionBar(player, finalId);
                    return;
                }

                MessageUtils.sendActionBarAsync(player, text);
                currentIteration++;
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeActionBarData(player, id, new ActionBarData(text, task, ActionBarType.TIMED));
        return id;
    }

    /**
     * Crea un action bar animado
     * @param player Jugador destinatario
     * @param section Sección de configuración
     * @param id ID único
     * @param baseText Texto base
     * @return ID del action bar creado
     */
    public static String createAnimatedActionBar(Player player, ConfigurationSection section, String id, String baseText) {
        if (id == null) id = generateActionBarId();

        String animationType = section.getString("animation.type", "dots").toLowerCase();
        long updateInterval = section.getLong("animation.updateInterval", 10);
        double duration = section.getDouble("duration", -1); // -1 = infinito

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private int animationStep = 0;
            private long ticksElapsed = 0;
            private final long maxTicks = duration > 0 ? (long) (duration * 20) : -1;

            @Override
            public void run() {
                if (!player.isOnline() || (maxTicks > 0 && ticksElapsed >= maxTicks)) {
                    cancelActionBar(player, finalId);
                    return;
                }

                String animatedText = applyAnimation(baseText, animationType, animationStep);
                MessageUtils.sendActionBarAsync(player, animatedText);

                animationStep++;
                ticksElapsed += updateInterval;
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeActionBarData(player, id, new ActionBarData(baseText, task, ActionBarType.ANIMATED));
        return id;
    }

    /**
     * Crea un action bar con countdown
     * @param player Jugador destinatario
     * @param id ID único
     * @param textTemplate Plantilla de texto con %time% para el tiempo
     * @param timeInSeconds Tiempo inicial en segundos
     * @param updateInterval Intervalo de actualización en ticks
     * @return ID del action bar creado
     */
    public static String createCountdownActionBar(Player player, String id, String textTemplate, double timeInSeconds, long updateInterval) {
        if (id == null) id = generateActionBarId();

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private double timeLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || timeLeft <= 0) {
                    // Mostrar mensaje final si queda en 0
                    if (timeLeft <= 0 && player.isOnline()) {
                        String finalText = textTemplate.replace("%time%", "0");
                        MessageUtils.sendActionBarAsync(player, finalText);
                    }
                    cancelActionBar(player, finalId);
                    return;
                }

                String currentText = textTemplate.replace("%time%", formatTime(timeLeft));
                MessageUtils.sendActionBarAsync(player, currentText);

                timeLeft -= (double) updateInterval / 20.0;
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeActionBarData(player, id, new ActionBarData(textTemplate, task, ActionBarType.COUNTDOWN));
        return id;
    }

    /**
     * Actualiza el texto de un action bar existente
     * @param player Jugador
     * @param id ID del action bar
     * @param newText Nuevo texto
     * @param replacements Reemplazos opcionales
     */
    public static void updateActionBarText(Player player, String id, String newText, String... replacements) {
        ActionBarData data = getActionBarData(player, id);
        if (data == null) return;

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                newText = newText.replace(replacements[i], replacements[i + 1]);
            }
        }

        // Actualizar el texto almacenado
        storeActionBarData(player, id, new ActionBarData(newText, data.task(), data.type()));
    }

    /**
     * Cancela y elimina un action bar específico
     * @param player Jugador
     * @param id ID del action bar
     * @return true si se canceló exitosamente
     */
    public static boolean cancelActionBar(Player player, String id) {
        ActionBarData data = getActionBarData(player, id);
        if (data == null) return false;

        // Cancelar la tarea si existe
        if (data.task() != null && !data.task().isCancelled()) {
            data.task().cancel();
        }

        // Limpiar action bar enviando uno vacío
        MessageUtils.sendActionBarAsync(player, "");

        // Eliminar los datos
        removeActionBarData(player, id);
        return true;
    }

    /**
     * Cancela todos los action bars de un jugador
     * @param player Jugador
     * @return Número de action bars cancelados
     */
    public static int cancelAllActionBars(Player player) {
        Map<String, ActionBarData> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        for (String id : new HashSet<>(playerData.keySet())) {
            if (cancelActionBar(player, id)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Pausa un action bar (solo afecta a los que tienen tasks)
     * @param player Jugador
     * @param id ID del action bar
     * @return true si se pausó exitosamente
     */
    public static boolean pauseActionBar(Player player, String id) {
        ActionBarData data = getActionBarData(player, id);
        if (data == null || data.task() == null) return false;

        if (!data.task().isCancelled()) {
            data.task().cancel();
            return true;
        }
        return false;
    }

    /**
     * Obtiene todos los action bars activos de un jugador
     * @param player Jugador
     * @return Set con los IDs de los action bars activos
     */
    public static Set<String> getActiveActionBars(Player player) {
        Map<String, ActionBarData> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Verifica si un jugador tiene un action bar específico activo
     * @param player Jugador
     * @param id ID del action bar
     * @return true si el action bar está activo
     */
    public static boolean hasActionBar(Player player, String id) {
        return getActionBarData(player, id) != null;
    }

    /**
     * Envía un action bar de progreso
     * @param player Jugador
     * @param id ID único
     * @param textTemplate Plantilla con %progress% y %bar%
     * @param current Valor actual
     * @param max Valor máximo
     * @param barLength Longitud de la barra
     * @param filledChar Carácter para parte llena
     * @param emptyChar Carácter para parte vacía
     * @return ID del action bar creado
     */
    public static String createProgressActionBar(Player player, String id, String textTemplate, double current, double max,
                                                 int barLength, char filledChar, char emptyChar) {
        if (id == null) id = generateActionBarId();

        double percentage = Math.max(0, Math.min(100, (current / max) * 100));
        int filled = (int) ((percentage / 100) * barLength);
        int empty = barLength - filled;

        String bar = "&a" + String.valueOf(filledChar).repeat(filled) +
                "&7" + String.valueOf(emptyChar).repeat(empty);

        String text = textTemplate
                .replace("%progress%", String.format("%.1f%%", percentage))
                .replace("%bar%", bar)
                .replace("%current%", String.format("%.0f", current))
                .replace("%max%", String.format("%.0f", max));

        return createSingleActionBar(player, id, text);
    }

    /**
     * Limpia todos los action bars al descargar el plugin
     */
    public static void cleanup() {
        for (UUID playerId : playerActionBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllActionBars(player);
            }
        }
        playerActionBars.clear();
    }

    // ============== MÉTODOS PRIVADOS ==============

    private static String generateActionBarId() {
        return "actionbar_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private static void storeActionBarData(Player player, String id, ActionBarData data) {
        playerActionBars.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, data);
    }

    private static ActionBarData getActionBarData(Player player, String id) {
        Map<String, ActionBarData> playerData = playerActionBars.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeActionBarData(Player player, String id) {
        Map<String, ActionBarData> playerData = playerActionBars.get(player.getUniqueId());
        if (playerData != null) {
            playerData.remove(id);
            if (playerData.isEmpty()) {
                playerActionBars.remove(player.getUniqueId());
            }
        }
    }

    private static String applyAnimation(String baseText, String animationType, int step) {
        return switch (animationType.toLowerCase()) {
            case "dots" -> {
                String[] dots = {"", ".", "..", "..."};
                yield baseText + dots[step % dots.length];
            }
            case "loading" -> {
                String[] loading = {"[   ]", "[▌  ]", "[██ ]", "[███]", "[███]", "[███]"};
                yield baseText.replace("%loading%", loading[step % loading.length]);
            }
            case "spinner" -> {
                String[] spinner = {"|", "/", "-", "\\"};
                yield baseText.replace("%spinner%", spinner[step % spinner.length]);
            }
            case "wave" -> {
                String[] wave = {"~", "~~", "~~~", "~~~~", "~~~~~", "~~~~", "~~~", "~~"};
                yield baseText.replace("%wave%", wave[step % wave.length]);
            }
            case "blink" -> (step % 2 == 0) ? baseText : "";
            default -> baseText;
        };
    }

    private static String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format("%.1fs", seconds);
        } else if (seconds < 3600) {
            int minutes = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%dm %ds", minutes, secs);
        } else {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            return String.format("%dh %dm", hours, minutes);
        }
    }

    // ============== CLASES INTERNAS ==============

    private record ActionBarData(String text, BukkitTask task, ActionBarType type) {}

    private enum ActionBarType {
        SINGLE,      // Action bar simple (una vez)
        PERMANENT,   // Action bar permanente
        TIMED,       // Action bar temporal
        ANIMATED,    // Action bar animado
        COUNTDOWN    // Action bar con countdown
    }
}