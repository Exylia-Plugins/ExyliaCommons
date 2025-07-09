// ==================== CONFIGURATION SYSTEM MODERNIZADO ====================

package net.exylia.commons.config;

import lombok.Getter;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
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

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Sistema de configuración modernizado que usa el sistema unificado de placeholders
 * Elimina la dependencia del sistema de placeholders interno
 */
public class ConfigurationSystem {

    @Getter
    private final JavaPlugin plugin;
    private final PlaceholderSystemManager placeholderManager;
    private final Map<String, ConfigFileData> configFiles = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> configInstances = new ConcurrentHashMap<>();
    private final Set<ConfigReloadListener> reloadListeners = new HashSet<>();
    private final ConfigCache cache = new ConfigCache();

    private String globalPrefix = "";
    private boolean debugMode = false;

    public ConfigurationSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.placeholderManager = PlaceholderSystemManager.getInstance();
        ColorUtils.initializePresets(plugin);
    }

    // ==================== INICIALIZACIÓN ====================

    /**
     * Inicializa el sistema de configuración
     */
    @SafeVarargs
    public final ConfigurationSystem initialize(Class<? extends ConfigBase>... configClasses) {
        for (Class<? extends ConfigBase> configClass : configClasses) {
            loadConfigClass(configClass);
        }

        setupGlobalPrefix();
        logInternalSuccess("Configuration System started with " + configFiles.size() + " files");
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

            logInternalDebug(debugMode, "Clase de configuración cargada: " + configClass.getSimpleName());

        } catch (Exception e) {
            logInternalError("Error cargando clase de configuración " + configClass.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ==================== API DE MENSAJES MODERNIZADA ====================

    /**
     * Builder modernizado para mensajes que usa el sistema unificado
     */
    public MessageBuilder message(String path) {
        return new MessageBuilder(path);
    }

    /**
     * Obtiene un mensaje simple
     */
    public Component getMessage(String path) {
        return message(path).build();
    }

    /**
     * Obtiene un mensaje con reemplazos
     */
    public Component getMessage(String path, Object... replacements) {
        return message(path).replace(replacements).build();
    }

    public ConfigFileData getFileData(String fileName) {
        return configFiles.get(fileName);
    }

    /**
     * Obtiene el mapa de instancias de configuración para acceso interno
     * @return Mapa de clases a instancias de configuración
     */
    public Map<Class<?>, Object> getConfigInstances() {
        return new HashMap<>(configInstances);
    }

    /**
     * MessageBuilder modernizado que usa el sistema unificado de placeholders
     */
    public class MessageBuilder {
        private final String path;
        private ExyliaContext context = ExyliaContext.create();
        private Player player;
        private final Map<String, Object> replacements = new HashMap<>();
        private boolean usePrefix = true;
        private String customPrefix;

        public MessageBuilder(String path) {
            this.path = path;
        }

        // ==================== GESTIÓN DE CONTEXTOS ====================

        /**
         * Establece el contexto completo
         */
        public MessageBuilder withContext(ExyliaContext context) {
            this.context = context != null ? context : ExyliaContext.create();
            return this;
        }

        /**
         * Añade un objeto al contexto
         */
        public MessageBuilder addToContext(Object object) {
            this.context.add(object);
            return this;
        }

        /**
         * Añade múltiples objetos al contexto
         */
        public MessageBuilder addToContext(Object... objects) {
            this.context.addAll(objects);
            return this;
        }

        /**
         * Añade datos con clave al contexto
         */
        public MessageBuilder addToContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        /**
         * Limpia el contexto
         */
        public MessageBuilder clearContext() {
            this.context = ExyliaContext.create();
            return this;
        }

        // ==================== CONFIGURACIÓN DEL BUILDER ====================

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

        // ==================== CONSTRUCCIÓN DEL MENSAJE ====================

        public Component build() {
            return buildInternal();
        }

        public String buildString() {
            return buildInternal().toString();
        }

        private Component buildInternal() {
            // Generar clave de cache para mensajes estáticos
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

            // Procesar con ExyliaContext
            message = context.processPlaceholders(message, player);

            // Aplicar reemplazos manuales (no-Component)
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

            // Guardar en cache si es estático
            if (cacheKey != null) {
                cache.putMessage(cacheKey, component);
            }

            return component;
        }

        private String generateCacheKey() {
            // Solo cachear si no hay contextos o reemplazos dinámicos
            if (!context.isEmpty() || player != null || !replacements.isEmpty()) {
                return null;
            }

            // Verificar si el mensaje contiene placeholders
            String messageText = getMessageFile().getString(path, "");
            if (containsPlaceholders(messageText)) {
                return null;
            }

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
            return text.contains("%");
        }
    }

    // ==================== OBTENCIÓN DE CONFIGURACIONES ====================

    /**
     * Obtiene una instancia de configuración por clase
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
     * Acceso directo a archivo de configuración
     */
    public FileConfiguration getFile(String fileName) {
        ConfigFileData data = configFiles.get(fileName);
        return data != null ? data.configuration : null;
    }

    // ==================== SISTEMA DE CACHE ====================

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

    // ==================== SISTEMA DE RELOAD ====================

    /**
     * Recarga todas las configuraciones de forma asíncrona
     */
    public CompletableFuture<Boolean> reloadAllAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logInternalInfo("Iniciando reload del sistema de configuración...");

                cache.invalidateAll();

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

                setupGlobalPrefix();
                ColorUtils.reloadPresets();

                logInternalSuccess("Reload " + (allSuccess ? "exitoso" : "con advertencias"));

                Bukkit.getScheduler().runTask(plugin, this::notifyReloadListeners);
                return allSuccess;

            } catch (Exception e) {
                logInternalError("Error durante reload: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Recarga un archivo específico
     */
    public boolean reloadFile(String fileName) {
        try {
            ConfigFileData oldData = configFiles.get(fileName);
            if (oldData == null) {
                logInternalError("Archivo no encontrado para reload: " + fileName);
                return false;
            }

            File file = new File(plugin.getDataFolder(), fileName + ".yml");
            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(file);

            if (oldData.validator != null && !oldData.validator.apply(newConfig)) {
                logInternalError("Validación fallida para " + fileName + " durante reload");
                return false;
            }

            oldData.configuration = newConfig;
            oldData.lastModified = file.lastModified();

            logInternalDebug(debugMode, "Archivo recargado: " + fileName);
            return true;

        } catch (Exception e) {
            logInternalError("Error recargando " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    // ==================== MÉTODOS INTERNOS ====================

    private ConfigFileData loadConfigFile(String fileName, boolean required) {
        try {
            File file = new File(plugin.getDataFolder(), fileName + ".yml");

            if (!file.exists()) {
                if (required) {
                    plugin.saveResource(fileName + ".yml", false);
                } else {
                    logInternalDebug(debugMode, "Archivo opcional no encontrado: " + fileName);
                    return null;
                }
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigFileData data = new ConfigFileData(config, file.lastModified());

            configFiles.put(fileName, data);
            logInternalDebug(debugMode, "Archivo cargado: " + fileName);

            return data;

        } catch (Exception e) {
            if (required) {
                throw new RuntimeException("Error cargando archivo requerido " + fileName, e);
            } else {
                logInternalError("Error cargando archivo opcional " + fileName + ": " + e.getMessage());
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

    // ==================== MÉTODOS DE ESCRITURA PARA ConfigurationSystem ====================

    /**
     * Establece un valor en un archivo específico
     */
    public void setValue(String fileName, String path, Object value) {
        ConfigFileData data = configFiles.get(fileName);
        if (data == null) {
            throw new IllegalArgumentException("Archivo no encontrado: " + fileName);
        }

        data.configuration.set(path, value);
        saveFile(fileName);
    }

    /**
     * Establece múltiples valores en un archivo
     */
    public void setValues(String fileName, Map<String, Object> values) {
        ConfigFileData data = configFiles.get(fileName);
        if (data == null) {
            throw new IllegalArgumentException("Archivo no encontrado: " + fileName);
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            data.configuration.set(entry.getKey(), entry.getValue());
        }
        saveFile(fileName);
    }

    /**
     * Guarda un archivo específico
     */
    public void saveFile(String fileName) {
        try {
            ConfigFileData data = configFiles.get(fileName);
            if (data == null) {
                throw new IllegalArgumentException("Archivo no encontrado: " + fileName);
            }

            File file = new File(plugin.getDataFolder(), fileName + ".yml");
            data.configuration.save(file);
            data.lastModified = file.lastModified();

            // Limpiar cache relacionado
            cache.invalidateAll();

            logInternalDebug(debugMode, "Archivo guardado: " + fileName);
        } catch (Exception e) {
            logInternalError("Error guardando archivo " + fileName + ": " + e.getMessage());
            throw new RuntimeException("Error guardando archivo", e);
        }
    }

    /**
     * Guarda todos los archivos cargados
     */
    public void saveAllFiles() {
        for (String fileName : configFiles.keySet()) {
            try {
                saveFile(fileName);
            } catch (Exception e) {
                logInternalError("Error guardando " + fileName + " durante saveAll: " + e.getMessage());
            }
        }
    }

    // ==================== LISTENERS Y ESTADÍSTICAS ====================

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
                logInternalError("Error en listener de reload: " + e.getMessage());
            }
        });
    }

    public static class ConfigFileData {
        FileConfiguration configuration;
        long lastModified;
        java.util.function.Function<FileConfiguration, Boolean> validator;

        ConfigFileData(FileConfiguration configuration, long lastModified) {
            this.configuration = configuration;
            this.lastModified = lastModified;
        }
    }

    public void shutdown() {
        cache.invalidateAll();
        configFiles.clear();
        configInstances.clear();
        reloadListeners.clear();
        logInternalInfo("Sistema de configuración finalizado");
    }
}