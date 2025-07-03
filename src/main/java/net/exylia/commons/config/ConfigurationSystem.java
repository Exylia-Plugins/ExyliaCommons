package net.exylia.commons.config;

import net.exylia.commons.placeholders.PlaceholderRegistry;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Sistema de configuración avanzado y elegante para plugins Exylia
 *
 * Características principales:
 * - Carga automática de configuraciones basada en anotaciones
 * - Sistema de cache inteligente con invalidación automática
 * - Soporte completo para placeholders con contexto
 * - Validación automática de configuraciones
 * - Sistema de fallbacks y valores por defecto
 * - Hot-reload con notificaciones a listeners
 * - API fluida y minimalista
 * - Soporte para configuraciones anidadas y complejas
 */
public class ConfigurationSystem {

    private final JavaPlugin plugin;
    private final Map<String, ConfigFileData> configFiles = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> configInstances = new ConcurrentHashMap<>();
    private final Set<ConfigReloadListener> reloadListeners = new HashSet<>();
    private final ConfigCache cache = new ConfigCache();

    private String globalPrefix = "";
    private boolean debugMode = false;

    public ConfigurationSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        ColorUtils.initializePresets(plugin);
    }

    // ========== API PRINCIPAL - CONFIGURACIÓN AUTOMÁTICA ==========

    /**
     * Inicializa automáticamente todas las configuraciones basándose en clases anotadas
     * @param configClasses Clases de configuración con anotaciones @ConfigFile
     */
    @SafeVarargs
    public final ConfigurationSystem initialize(Class<? extends ConfigBase>... configClasses) {
        logInfo("Inicializando sistema de configuración avanzado...");

        // Cargar archivos automáticamente basándose en anotaciones
        for (Class<? extends ConfigBase> configClass : configClasses) {
            loadConfigClass(configClass);
        }

        // Configurar el prefix global desde messages si existe
        setupGlobalPrefix();

        logSuccess("Sistema de configuración inicializado con " + configFiles.size() + " archivos");
        return this;
    }

    /**
     * Carga una clase de configuración específica
     */
    private void loadConfigClass(Class<? extends ConfigBase> configClass) {
        try {
            ConfigFile annotation = configClass.getAnnotation(ConfigFile.class);
            if (annotation == null) {
                throw new IllegalArgumentException("La clase " + configClass.getSimpleName() +
                        " debe tener la anotación @ConfigFile");
            }

            String fileName = annotation.value();
            boolean required = annotation.required();
            String[] dependencies = annotation.dependencies();

            // Cargar dependencias primero
            for (String dependency : dependencies) {
                if (!configFiles.containsKey(dependency)) {
                    loadConfigFile(dependency, false);
                }
            }

            // Cargar el archivo
            ConfigFileData fileData = loadConfigFile(fileName, required);

            // Crear instancia de la clase de configuración
            ConfigBase instance = configClass.getDeclaredConstructor().newInstance();
            instance.initialize(this, fileData);
            configInstances.put(configClass, instance);

            logDebug(debugMode, "Clase de configuración cargada: " + configClass.getSimpleName());

        } catch (Exception e) {
            logError("Error cargando clase de configuración " + configClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ========== API SIMPLIFICADA PARA OBTENER CONFIGURACIONES ==========

    /**
     * Obtiene una instancia de configuración por su clase
     * @param configClass La clase de configuración
     * @return La instancia configurada
     */
    @SuppressWarnings("unchecked")
    public <T extends ConfigBase> T getConfig(Class<T> configClass) {
        T instance = (T) configInstances.get(configClass);
        if (instance == null) {
            throw new IllegalStateException("Configuración no inicializada: " + configClass.getSimpleName());
        }
        return instance;
    }

    /**
     * Acceso directo a un archivo de configuración por nombre
     * @param fileName Nombre del archivo sin extensión
     * @return El FileConfiguration correspondiente
     */
    public FileConfiguration getFile(String fileName) {
        ConfigFileData data = configFiles.get(fileName);
        return data != null ? data.configuration : null;
    }

    // ========== API DE MENSAJES AVANZADA ==========

    /**
     * API fluida para obtener mensajes con máxima flexibilidad
     */
    public MessageBuilder message(String path) {
        return new MessageBuilder(path);
    }

    /**
     * Obtiene un mensaje simple sin procesamiento adicional
     */
    public Component getMessage(String path) {
        return message(path).build();
    }

    /**
     * Obtiene un mensaje con reemplazos simples
     */
    public Component getMessage(String path, Object... replacements) {
        return message(path).replace(replacements).build();
    }

    /**
     * Builder pattern para construcción fluida de mensajes
     */
    public class MessageBuilder {
        private final String path;
        private final List<Object> contexts = new ArrayList<>();
        private Player player;
        private final Map<String, Object> replacements = new HashMap<>();
        private boolean usePrefix = true;
        private String customPrefix;
        private boolean asString = false;

        public MessageBuilder(String path) {
            this.path = path;
        }

        // ========== MÉTODOS PARA CONTEXTOS MÚLTIPLES ==========

        /**
         * Añade un contexto único al builder
         */
        public MessageBuilder withContext(Object context) {
            if (context != null) {
                this.contexts.add(context);
            }
            return this;
        }

        /**
         * Añade múltiples contextos de una vez
         */
        public MessageBuilder withContexts(Object... contexts) {
            for (Object context : contexts) {
                if (context != null) {
                    this.contexts.add(context);
                }
            }
            return this;
        }

        /**
         * Añade múltiples contextos desde una colección
         */
        public MessageBuilder withContexts(Collection<Object> contexts) {
            for (Object context : contexts) {
                if (context != null) {
                    this.contexts.add(context);
                }
            }
            return this;
        }

        /**
         * Limpia todos los contextos actuales
         */
        public MessageBuilder clearContexts() {
            this.contexts.clear();
            return this;
        }

        /**
         * Obtiene una copia inmutable de los contextos actuales
         */
        public List<Object> getContexts() {
            return new ArrayList<>(contexts);
        }

        /**
         * Verifica si tiene contextos
         */
        public boolean hasContexts() {
            return !contexts.isEmpty();
        }

        // ========== MÉTODOS EXISTENTES MEJORADOS ==========

        public MessageBuilder forPlayer(Player player) {
            this.player = player;
            return this;
        }

        public MessageBuilder replace(String placeholder, Object value) {
            replacements.put(placeholder, value);
            return this;
        }

        public MessageBuilder replace(Object... args) {
            for (int i = 0; i < args.length - 1; i += 2) {
                replacements.put(args[i].toString(), args[i + 1]);
            }
            return this;
        }

        public MessageBuilder replace(Map<String, Object> replacements) {
            this.replacements.putAll(replacements);
            return this;
        }

        public MessageBuilder noPrefix() {
            this.usePrefix = false;
            return this;
        }

        public MessageBuilder withPrefix(String prefix) {
            this.customPrefix = prefix;
            return this;
        }

        public MessageBuilder asString() {
            this.asString = true;
            return this;
        }

        // ========== MÉTODOS DE CONSTRUCCIÓN ==========

        public Component build() {
            return buildInternal(false);
        }

        public String buildString() {
            return buildInternal(true).toString();
        }

        private String generateCacheKey() {
            // Regla simple: Si hay contextos, jugador, o reemplazos dinámicos, no cachear
            if (!contexts.isEmpty() || player != null || !replacements.isEmpty()) {
                return null; // No usar caché
            }

            // Verificar si el mensaje original contiene placeholders
            String messageText = getMessageFile().getString(path, "");
            if (containsPlaceholders(messageText)) {
                return null; // No usar caché
            }

            // Solo cachear mensajes completamente estáticos
            return String.format("%s:%s:%s",
                    path,
                    usePrefix,
                    customPrefix != null ? customPrefix : "default"
            );
        }

        /**
         * Verifica si un texto contiene placeholders
         */
        private boolean containsPlaceholders(String text) {
            if (text == null || text.isEmpty()) {
                return false;
            }

            // Buscar patrón %cualquier_cosa%
            int firstPercent = text.indexOf('%');
            if (firstPercent == -1) {
                return false;
            }

            int secondPercent = text.indexOf('%', firstPercent + 1);
            return secondPercent != -1;
        }

        private Component buildInternal(boolean forceString) {
            // Usar cache solo para mensajes completamente estáticos
            String cacheKey = generateCacheKey();
            if (cacheKey != null) {
                Component cached = cache.getMessage(cacheKey);
                if (cached != null) {
                    return cached;
                }
            }

            // Obtener mensaje base
            String message = getMessageFile().getString(path, "{error}" + path + " not found");

            // Aplicar prefix
            if (usePrefix) {
                if (customPrefix != null) {
                    message = customPrefix + message;
                } else {
                    message = message.replace("%prefix%", globalPrefix);
                }
            }

            // Procesar placeholders con múltiples contextos
            if (!contexts.isEmpty() || player != null) {
                message = PlaceholderRegistry.processMultipleContexts(message, contexts, player);
            }

            // Aplicar reemplazos manuales
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                if (!(entry.getValue() instanceof Component)) {
                    message = message.replace(entry.getKey(), entry.getValue().toString());
                }
            }

            // Convertir a Component
            Component component = ColorUtils.parse(message);

            // Aplicar reemplazos de Component
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                if (entry.getValue() instanceof Component) {
                    component = component.replaceText(TextReplacementConfig.builder()
                            .match(entry.getKey())
                            .replacement((Component) entry.getValue())
                            .build());
                }
            }

            // Guardar en cache SOLO si es completamente estático
            if (cacheKey != null) {
                cache.putMessage(cacheKey, component);
            }

            return component;
        }

        /**
         * Genera un hash único para todos los contextos
         */
        private String generateContextsHash() {
            if (contexts.isEmpty()) {
                return "null";
            }

            int hash = 1;
            for (Object context : contexts) {
                hash = 31 * hash + (context != null ? context.hashCode() : 0);
            }
            return String.valueOf(hash);
        }

        // ========== MÉTODOS DE UTILIDAD ==========

        /**
         * Clona el builder actual con todos sus contextos y configuraciones
         */
        public MessageBuilder clone() {
            MessageBuilder cloned = new MessageBuilder(this.path);
            cloned.contexts.addAll(this.contexts);
            cloned.player = this.player;
            cloned.replacements.putAll(this.replacements);
            cloned.usePrefix = this.usePrefix;
            cloned.customPrefix = this.customPrefix;
            cloned.asString = this.asString;
            return cloned;
        }

        /**
         * Crea un nuevo builder basado en este pero con un path diferente
         */
        public MessageBuilder withPath(String newPath) {
            MessageBuilder newBuilder = new MessageBuilder(newPath);
            newBuilder.contexts.addAll(this.contexts);
            newBuilder.player = this.player;
            newBuilder.replacements.putAll(this.replacements);
            newBuilder.usePrefix = this.usePrefix;
            newBuilder.customPrefix = this.customPrefix;
            newBuilder.asString = this.asString;
            return newBuilder;
        }

        /**
         * Información de debug sobre el builder
         */
        public String getDebugInfo() {
            return String.format("MessageBuilder{path='%s', contextos=%d, jugador=%s, reemplazos=%d, prefix=%s}",
                    path,
                    contexts.size(),
                    player != null ? player.getName() : "null",
                    replacements.size(),
                    usePrefix ? (customPrefix != null ? customPrefix : "global") : "none"
            );
        }

        @Override
        public String toString() {
            return getDebugInfo();
        }
    }

    // ========== SISTEMA DE CACHE INTELIGENTE ==========

    private static class ConfigCache {
        private final Map<String, Component> messageCache = new ConcurrentHashMap<>();
        private final Map<String, Object> valueCache = new ConcurrentHashMap<>();
        private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
        private static final long CACHE_TTL = 300000; // 5 minutos

        public Component getMessage(String key) {
            if (isExpired(key)) {
                invalidate(key);
                return null;
            }
            return messageCache.get(key);
        }

        public void putMessage(String key, Component value) {
            messageCache.put(key, value);
            cacheTimestamps.put(key, System.currentTimeMillis());
        }

        public Object getValue(String key) {
            if (isExpired(key)) {
                invalidate(key);
                return null;
            }
            return valueCache.get(key);
        }

        public void putValue(String key, Object value) {
            valueCache.put(key, value);
            cacheTimestamps.put(key, System.currentTimeMillis());
        }

        private boolean isExpired(String key) {
            Long timestamp = cacheTimestamps.get(key);
            return timestamp == null || (System.currentTimeMillis() - timestamp) > CACHE_TTL;
        }

        public void invalidate(String key) {
            messageCache.remove(key);
            valueCache.remove(key);
            cacheTimestamps.remove(key);
        }

        public void invalidateAll() {
            messageCache.clear();
            valueCache.clear();
            cacheTimestamps.clear();
        }
    }

    // ========== SISTEMA DE RELOAD AVANZADO ==========

    /**
     * Recarga todas las configuraciones de forma asíncrona
     */
    public CompletableFuture<Boolean> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logInfo("Iniciando reload asíncrono del sistema de configuración...");

                // Invalidar cache
                cache.invalidateAll();

                // Recargar archivos
                boolean allSuccess = true;
                for (String fileName : configFiles.keySet()) {
                    if (!reloadFile(fileName)) {
                        allSuccess = false;
                    }
                }

                // Recargar instancias de configuración
                for (Object instance : configInstances.values()) {
                    if (instance instanceof ConfigBase) {
                        ((ConfigBase) instance).onReload();
                    }
                }

                // Reconfigurar sistemas dependientes
                setupGlobalPrefix();
                ColorUtils.reloadPresets();

                logSuccess("Reload asíncrono " + (allSuccess ? "exitoso" : "con advertencias"));

                // Notificar a listeners en el hilo principal
                Bukkit.getScheduler().runTask(plugin, this::notifyReloadListeners);

                return allSuccess;

            } catch (Exception e) {
                logError("Error durante reload asíncrono: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Recarga un archivo específico de forma asíncrona
     */
    public CompletableFuture<Boolean> reloadFileAsync(String fileName) {
        return CompletableFuture.supplyAsync(() -> reloadFile(fileName));
    }

    /**
     * Método síncrono que delega al asíncrono para compatibilidad
     */
    public boolean reloadAll() {
        try {
            return reloadAllAsync().get();
        } catch (Exception e) {
            logError("Error en reload síncrono de configuración: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reload con callback
     */
    public void reloadAllAsync(Consumer<Boolean> callback) {
        reloadAllAsync().thenAccept(callback);
    }

    /**
     * Reload con timeout
     */
    public CompletableFuture<Boolean> reloadAllAsync(long timeoutSeconds) {
        return reloadAllAsync()
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logError("Reload de configuración cancelado por timeout (" + timeoutSeconds + "s)");
                    } else {
                        logError("Error en reload de configuración con timeout: " + throwable.getMessage());
                    }
                    return false;
                });
    }

    /**
     * Recarga un archivo específico
     */
    public boolean reloadFile(String fileName) {
        try {
            ConfigFileData oldData = configFiles.get(fileName);
            if (oldData == null) {
                logError("Archivo no encontrado para reload: " + fileName);
                return false;
            }

            File file = new File(plugin.getDataFolder(), fileName + ".yml");
            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(file);

            // Validar configuración si tiene validador
            if (oldData.validator != null && !oldData.validator.apply(newConfig)) {
                logError("Validación fallida para " + fileName + " durante reload");
                return false;
            }

            // Actualizar configuración
            oldData.configuration = newConfig;
            oldData.lastModified = file.lastModified();

            logDebug(debugMode, "Archivo recargado: " + fileName);
            return true;

        } catch (Exception e) {
            logError("Error recargando " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    // ========== LISTENERS Y HOOKS ==========

    public interface ConfigReloadListener {
        void onConfigReload(String fileName);
        default void onAllConfigsReload() {}
    }

    public void addReloadListener(ConfigReloadListener listener) {
        reloadListeners.add(listener);
    }

    public void removeReloadListener(ConfigReloadListener listener) {
        reloadListeners.remove(listener);
    }

    private void notifyReloadListeners() {
        reloadListeners.forEach(listener -> {
            try {
                listener.onAllConfigsReload();
            } catch (Exception e) {
                logError("Error en listener de reload: " + e.getMessage());
            }
        });
    }

    // ========== MÉTODOS INTERNOS ==========

    private ConfigFileData loadConfigFile(String fileName, boolean required) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".yml");

            if (!file.exists()) {
                if (required) {
                    plugin.saveResource(fileName + ".yml", false);
                } else {
                    logDebug(debugMode, "Archivo opcional no encontrado: " + fileName);
                    return null;
                }
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigFileData data = new ConfigFileData(config, file.lastModified());

            configFiles.put(fileName, data);
            logDebug(debugMode, "Archivo cargado: " + fileName);

            return data;

        } catch (Exception e) {
            if (required) {
                throw new RuntimeException("Error cargando archivo requerido " + fileName, e);
            } else {
                logError("Error cargando archivo opcional " + fileName + ": " + e.getMessage());
                return null;
            }
        }
    }

    private void setupGlobalPrefix() {
        FileConfiguration messages = getFile("messages");
        if (messages != null) {
            globalPrefix = messages.getString("prefix", "");
            debugMode = messages.getBoolean("debug", false);
        }
    }

    private FileConfiguration getMessageFile() {
        FileConfiguration messages = getFile("messages");
        if (messages == null) {
            throw new IllegalStateException("Archivo messages.yml no está cargado");
        }
        return messages;
    }

    // ========== CLASES DE DATOS ==========

    public static class ConfigFileData {
        FileConfiguration configuration;
        long lastModified;
        Function<FileConfiguration, Boolean> validator;

        ConfigFileData(FileConfiguration configuration, long lastModified) {
            this.configuration = configuration;
            this.lastModified = lastModified;
        }
    }

    // ========== MÉTODOS DE UTILIDAD ==========

    /**
     * Verifica si un archivo de configuración ha sido modificado externamente
     */
    public boolean isFileModified(String fileName) {
        ConfigFileData data = configFiles.get(fileName);
        if (data == null) return false;

        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        return file.lastModified() != data.lastModified;
    }

    /**
     * Obtiene estadísticas del sistema de configuración
     */
    public ConfigStats getStats() {
        return new ConfigStats(
                configFiles.size(),
                configInstances.size(),
                cache.messageCache.size(),
                cache.valueCache.size()
        );
    }

    public static class ConfigStats {
        public final int loadedFiles;
        public final int configInstances;
        public final int cachedMessages;
        public final int cachedValues;

        ConfigStats(int loadedFiles, int configInstances, int cachedMessages, int cachedValues) {
            this.loadedFiles = loadedFiles;
            this.configInstances = configInstances;
            this.cachedMessages = cachedMessages;
            this.cachedValues = cachedValues;
        }
    }

    /**
     * Cleanup del sistema
     */
    public void shutdown() {
        cache.invalidateAll();
        configFiles.clear();
        configInstances.clear();
        reloadListeners.clear();
        logInfo("Sistema de configuración finalizado");
    }
}