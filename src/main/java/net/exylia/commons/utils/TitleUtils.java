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

public class TitleUtils {

    private static Plugin plugin;
    private static final Map<UUID, Map<String, TitleData>> playerTitles = new ConcurrentHashMap<>();

    public static void init(Plugin mainPlugin) {
        plugin = mainPlugin;
    }

    /**
     * Crea un título desde configuración
     * @param player Jugador destinatario
     * @param section Sección de configuración del título
     * @param replacements Reemplazos para el texto (opcional)
     * @return ID único del título creado
     */
    public static String createTitle(Player player, ConfigurationSection section, String... replacements) {
        if (section == null || !section.getBoolean("enabled", true)) return null;

        String id = generateTitleId();
        String title = section.getString("title", "");
        String subtitle = section.getString("subtitle", "");

        // Aplicar reemplazos
        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                title = title.replace(replacements[i], replacements[i + 1]);
                subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
            }
        }

        // Aplicar colores
        title = ColorUtils.applyColorPresets(title);
        subtitle = ColorUtils.applyColorPresets(subtitle);

        String type = section.getString("type", "single").toLowerCase();
        int fadeIn = section.getInt("fadeIn", 20);
        int stay = section.getInt("stay", 60);
        int fadeOut = section.getInt("fadeOut", 20);

        switch (type) {
            case "sequence":
                return createTitleSequence(player, section, id, replacements);

            case "countdown":
                double countdownTime = section.getDouble("countdownTime", 10.0);
                String countdownTemplate = section.getString("countdownTemplate", "%time%");
                return createCountdownTitle(player, id, title, subtitle, countdownTemplate,
                        countdownTime, fadeIn, stay, fadeOut);

            case "animated":
                return createAnimatedTitle(player, section, id, title, subtitle, fadeIn, stay, fadeOut);

            case "repeated":
                int repetitions = section.getInt("repetitions", 3);
                long delayBetween = section.getLong("delayBetween", 80);
                return createRepeatedTitle(player, id, title, subtitle, fadeIn, stay, fadeOut,
                        repetitions, delayBetween);

            case "single":
            default:
                return createSingleTitle(player, id, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Crea un título simple (se muestra una vez)
     * @param player Jugador destinatario
     * @param id ID único
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return ID del título creado
     */
    public static String createSingleTitle(Player player, String id, String title, String subtitle,
                                           int fadeIn, int stay, int fadeOut) {
        if (id == null) id = generateTitleId();

        MessageUtils.sendTitleAsync(player, title, subtitle, fadeIn, stay, fadeOut);

        // Almacenar datos para tracking
        storeTitleData(player, id, new TitleData(title, subtitle, null, TitleType.SINGLE, fadeIn, stay, fadeOut));

        // Auto-remover después del tiempo total de visualización
        long totalTime = fadeIn + stay + fadeOut + 10; // +10 ticks de margen
        String finalId = id;
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeTitleData(player, finalId), totalTime);

        return id;
    }

    /**
     * Crea un título repetido
     * @param player Jugador destinatario
     * @param id ID único
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @param repetitions Número de repeticiones
     * @param delayBetween Retraso entre repeticiones (ticks)
     * @return ID del título creado
     */
    public static String createRepeatedTitle(Player player, String id, String title, String subtitle,
                                             int fadeIn, int stay, int fadeOut, int repetitions, long delayBetween) {
        if (id == null) id = generateTitleId();

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private int currentRep = 0;

            @Override
            public void run() {
                if (!player.isOnline() || currentRep >= repetitions) {
                    cancelTitle(player, finalId);
                    return;
                }

                MessageUtils.sendTitleAsync(player, title, subtitle, fadeIn, stay, fadeOut);
                currentRep++;
            }
        }.runTaskTimer(plugin, 0L, delayBetween);

        storeTitleData(player, id, new TitleData(title, subtitle, task, TitleType.REPEATED, fadeIn, stay, fadeOut));
        return id;
    }

    /**
     * Crea un título con countdown
     * @param player Jugador destinatario
     * @param id ID único
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param countdownTemplate Plantilla para el countdown (ej: "%time%")
     * @param timeInSeconds Tiempo en segundos
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return ID del título creado
     */
    public static String createCountdownTitle(Player player, String id, String title, String subtitle,
                                              String countdownTemplate, double timeInSeconds,
                                              int fadeIn, int stay, int fadeOut) {
        if (id == null) id = generateTitleId();

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private double timeLeft = timeInSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || timeLeft <= 0) {
                    // Mostrar título final si llega a 0
                    if (timeLeft <= 0 && player.isOnline()) {
                        String finalTitle = title.replace("%time%", "0");
                        String finalSubtitle = subtitle.replace("%time%", "¡Tiempo agotado!");
                        MessageUtils.sendTitleAsync(player, finalTitle, finalSubtitle, fadeIn, stay, fadeOut);
                    }
                    cancelTitle(player, finalId);
                    return;
                }

                String formattedTime = formatTime(timeLeft);
                String currentTitle = title.replace("%time%", formattedTime);
                String currentSubtitle = subtitle.replace("%time%", formattedTime);

                MessageUtils.sendTitleAsync(player, currentTitle, currentSubtitle, fadeIn, stay, fadeOut);
                timeLeft -= 1.0; // Reducir 1 segundo por tick
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo

        storeTitleData(player, id, new TitleData(title, subtitle, task, TitleType.COUNTDOWN, fadeIn, stay, fadeOut));
        return id;
    }

    /**
     * Crea un título animado
     * @param player Jugador destinatario
     * @param section Sección de configuración
     * @param id ID único
     * @param baseTitle Título base
     * @param baseSubtitle Subtítulo base
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return ID del título creado
     */
    public static String createAnimatedTitle(Player player, ConfigurationSection section, String id,
                                             String baseTitle, String baseSubtitle, int fadeIn, int stay, int fadeOut) {
        if (id == null) id = generateTitleId();

        String animationType = section.getString("animation.type", "typewriter").toLowerCase();
        long updateInterval = section.getLong("animation.updateInterval", 3);
        double duration = section.getDouble("duration", 10.0);
        boolean loop = section.getBoolean("animation.loop", false);

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private int animationStep = 0;
            private long ticksElapsed = 0;
            private final long maxTicks = (long) (duration * 20);

            @Override
            public void run() {
                if (!player.isOnline() || (!loop && ticksElapsed >= maxTicks)) {
                    cancelTitle(player, finalId);
                    return;
                }

                String[] animatedText = applyTitleAnimation(baseTitle, baseSubtitle, animationType, animationStep);
                MessageUtils.sendTitleAsync(player, animatedText[0], animatedText[1], fadeIn, stay, fadeOut);

                animationStep++;
                ticksElapsed += updateInterval;

                if (loop && ticksElapsed >= maxTicks) {
                    animationStep = 0;
                    ticksElapsed = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        storeTitleData(player, id, new TitleData(baseTitle, baseSubtitle, task, TitleType.ANIMATED, fadeIn, stay, fadeOut));
        return id;
    }

    /**
     * Crea una secuencia de títulos
     * @param player Jugador destinatario
     * @param section Sección de configuración
     * @param id ID único
     * @param replacements Reemplazos globales
     * @return ID de la secuencia creada
     */
    public static String createTitleSequence(Player player, ConfigurationSection section, String id, String... replacements) {
        if (id == null) id = generateTitleId();

        ConfigurationSection sequenceSection = section.getConfigurationSection("sequence");
        if (sequenceSection == null) return null;

        List<TitleSequenceStep> steps = new ArrayList<>();

        for (String key : sequenceSection.getKeys(false)) {
            ConfigurationSection stepSection = sequenceSection.getConfigurationSection(key);
            if (stepSection == null) continue;

            String title = stepSection.getString("title", "");
            String subtitle = stepSection.getString("subtitle", "");
            int fadeIn = stepSection.getInt("fadeIn", 20);
            int stay = stepSection.getInt("stay", 60);
            int fadeOut = stepSection.getInt("fadeOut", 20);
            long delay = stepSection.getLong("delay", 0);

            // Aplicar reemplazos
            if (replacements != null) {
                for (int i = 0; i < replacements.length - 1; i += 2) {
                    title = title.replace(replacements[i], replacements[i + 1]);
                    subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
                }
            }

            steps.add(new TitleSequenceStep(title, subtitle, fadeIn, stay, fadeOut, delay));
        }

        if (steps.isEmpty()) return null;

        String finalId = id;
        BukkitTask task = new BukkitRunnable() {
            private int currentStep = 0;
            private long nextExecutionTime = 0;

            @Override
            public void run() {
                if (!player.isOnline() || currentStep >= steps.size()) {
                    cancelTitle(player, finalId);
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime < nextExecutionTime) return;

                TitleSequenceStep step = steps.get(currentStep);
                MessageUtils.sendTitleAsync(player, step.title(), step.subtitle(),
                        step.fadeIn(), step.stay(), step.fadeOut());

                nextExecutionTime = currentTime + (step.delay() * 50); // Convertir ticks a ms
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        storeTitleData(player, id, new TitleData("", "", task, TitleType.SEQUENCE, 0, 0, 0));
        return id;
    }

    /**
     * Crea un título de progreso
     * @param player Jugador destinatario
     * @param id ID único
     * @param titleTemplate Plantilla del título con %progress%, %bar%, etc.
     * @param subtitleTemplate Plantilla del subtítulo
     * @param current Valor actual
     * @param max Valor máximo
     * @param barLength Longitud de la barra
     * @param filledChar Carácter para parte llena
     * @param emptyChar Carácter para parte vacía
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return ID del título creado
     */
    public static String createProgressTitle(Player player, String id, String titleTemplate, String subtitleTemplate,
                                             double current, double max, int barLength, char filledChar, char emptyChar,
                                             int fadeIn, int stay, int fadeOut) {
        if (id == null) id = generateTitleId();

        double percentage = Math.max(0, Math.min(100, (current / max) * 100));
        int filled = (int) ((percentage / 100) * barLength);
        int empty = barLength - filled;

        String bar = "&a" + String.valueOf(filledChar).repeat(filled) +
                "&7" + String.valueOf(emptyChar).repeat(empty);

        String title = titleTemplate
                .replace("%progress%", String.format("%.1f%%", percentage))
                .replace("%bar%", bar)
                .replace("%current%", String.format("%.0f", current))
                .replace("%max%", String.format("%.0f", max));

        String subtitle = subtitleTemplate
                .replace("%progress%", String.format("%.1f%%", percentage))
                .replace("%bar%", bar)
                .replace("%current%", String.format("%.0f", current))
                .replace("%max%", String.format("%.0f", max));

        return createSingleTitle(player, id, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Actualiza el texto de un título existente (solo para tipos que lo permiten)
     * @param player Jugador
     * @param id ID del título
     * @param newTitle Nuevo título
     * @param newSubtitle Nuevo subtítulo
     * @param replacements Reemplazos opcionales
     */
    public static void updateTitleText(Player player, String id, String newTitle, String newSubtitle, String... replacements) {
        TitleData data = getTitleData(player, id);
        if (data == null || data.type() == TitleType.SINGLE) return;

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                newTitle = newTitle.replace(replacements[i], replacements[i + 1]);
                newSubtitle = newSubtitle.replace(replacements[i], replacements[i + 1]);
            }
        }

        // Actualizar los datos almacenados
        storeTitleData(player, id, new TitleData(newTitle, newSubtitle, data.task(), data.type(),
                data.fadeIn(), data.stay(), data.fadeOut()));
    }

    /**
     * Cancela y elimina un título específico
     * @param player Jugador
     * @param id ID del título
     * @return true si se canceló exitosamente
     */
    public static boolean cancelTitle(Player player, String id) {
        TitleData data = getTitleData(player, id);
        if (data == null) return false;

        // Cancelar la tarea si existe
        if (data.task() != null && !data.task().isCancelled()) {
            data.task().cancel();
        }

        // Limpiar título enviando uno vacío
        MessageUtils.sendTitleAsync(player, "", "", 0, 1, 0);

        // Eliminar los datos
        removeTitleData(player, id);
        return true;
    }

    /**
     * Cancela todos los títulos de un jugador
     * @param player Jugador
     * @return Número de títulos cancelados
     */
    public static int cancelAllTitles(Player player) {
        Map<String, TitleData> playerData = playerTitles.get(player.getUniqueId());
        if (playerData == null || playerData.isEmpty()) return 0;

        int count = 0;
        for (String id : new HashSet<>(playerData.keySet())) {
            if (cancelTitle(player, id)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Pausa un título (solo afecta a los que tienen tasks)
     * @param player Jugador
     * @param id ID del título
     * @return true si se pausó exitosamente
     */
    public static boolean pauseTitle(Player player, String id) {
        TitleData data = getTitleData(player, id);
        if (data == null || data.task() == null) return false;

        if (!data.task().isCancelled()) {
            data.task().cancel();
            return true;
        }
        return false;
    }

    /**
     * Obtiene todos los títulos activos de un jugador
     * @param player Jugador
     * @return Set con los IDs de los títulos activos
     */
    public static Set<String> getActiveTitles(Player player) {
        Map<String, TitleData> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? new HashSet<>(playerData.keySet()) : new HashSet<>();
    }

    /**
     * Verifica si un jugador tiene un título específico activo
     * @param player Jugador
     * @param id ID del título
     * @return true si el título está activo
     */
    public static boolean hasTitle(Player player, String id) {
        return getTitleData(player, id) != null;
    }

    /**
     * Limpia todos los títulos al descargar el plugin
     */
    public static void cleanup() {
        for (UUID playerId : playerTitles.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                cancelAllTitles(player);
            }
        }
        playerTitles.clear();
    }

    // ============== MÉTODOS PRIVADOS ==============

    private static String generateTitleId() {
        return "title_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private static void storeTitleData(Player player, String id, TitleData data) {
        playerTitles.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(id, data);
    }

    private static TitleData getTitleData(Player player, String id) {
        Map<String, TitleData> playerData = playerTitles.get(player.getUniqueId());
        return playerData != null ? playerData.get(id) : null;
    }

    private static void removeTitleData(Player player, String id) {
        Map<String, TitleData> playerData = playerTitles.get(player.getUniqueId());
        if (playerData != null) {
            playerData.remove(id);
            if (playerData.isEmpty()) {
                playerTitles.remove(player.getUniqueId());
            }
        }
    }

    private static String[] applyTitleAnimation(String baseTitle, String baseSubtitle, String animationType, int step) {
        String animatedTitle = baseTitle;
        String animatedSubtitle = baseSubtitle;

        switch (animationType.toLowerCase()) {
            case "typewriter":
                if (baseTitle.length() > 0) {
                    int titleLength = Math.min(step, baseTitle.length());
                    animatedTitle = baseTitle.substring(0, titleLength);
                }
                if (baseSubtitle.length() > 0) {
                    int subtitleStart = Math.max(0, step - baseTitle.length());
                    int subtitleLength = Math.min(subtitleStart, baseSubtitle.length());
                    animatedSubtitle = baseSubtitle.substring(0, subtitleLength);
                }
                break;

            case "fade":
                String[] fadeColors = {"&8", "&7", "&f", "&7", "&8"};
                String color = fadeColors[step % fadeColors.length];
                animatedTitle = color + baseTitle;
                animatedSubtitle = color + baseSubtitle;
                break;

            case "rainbow":
                String[] rainbowColors = {"&c", "&6", "&e", "&a", "&b", "&9", "&d"};
                String rainbowColor = rainbowColors[step % rainbowColors.length];
                animatedTitle = rainbowColor + baseTitle;
                animatedSubtitle = rainbowColor + baseSubtitle;
                break;

            case "shake":
                String[] shakeSpaces = {"", " ", "  ", " ", ""};
                String spaces = shakeSpaces[step % shakeSpaces.length];
                animatedTitle = spaces + baseTitle;
                animatedSubtitle = spaces + baseSubtitle;
                break;

            case "bounce":
                String bounceEffect = (step % 2 == 0) ? "&l" : "";
                animatedTitle = bounceEffect + baseTitle;
                animatedSubtitle = bounceEffect + baseSubtitle;
                break;

            default:
                break;
        }

        return new String[]{animatedTitle, animatedSubtitle};
    }

    private static String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format("%.0f", seconds);
        } else if (seconds < 3600) {
            int minutes = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%d:%02d", minutes, secs);
        } else {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            return String.format("%d:%02d:00", hours, minutes);
        }
    }

    // ============== CLASES INTERNAS ==============

    private record TitleData(String title, String subtitle, BukkitTask task, TitleType type,
                             int fadeIn, int stay, int fadeOut) {}

    private record TitleSequenceStep(String title, String subtitle, int fadeIn, int stay, int fadeOut, long delay) {}

    private enum TitleType {
        SINGLE,      // Título simple (una vez)
        REPEATED,    // Título repetido
        COUNTDOWN,   // Título con countdown
        ANIMATED,    // Título animado
        SEQUENCE,    // Secuencia de títulos
        PROGRESS     // Título de progreso
    }
}