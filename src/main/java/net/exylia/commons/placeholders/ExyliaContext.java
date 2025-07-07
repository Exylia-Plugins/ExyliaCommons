// ==================== SISTEMA DE CONTEXTO GLOBAL ====================

package net.exylia.commons.placeholders;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ExyliaContext {

    // Almacenamiento de datos por tipo
    private final Map<Class<?>, Object> typedData = new ConcurrentHashMap<>();

    // Almacenamiento de datos por clave string
    private final Map<String, Object> keyedData = new ConcurrentHashMap<>();

    // Datos dinámicos (se calculan en tiempo de ejecución)
    private final Map<String, Supplier<Object>> dynamicData = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTORES Y FACTORY METHODS ====================

    public ExyliaContext() {}

    /**
     * Crea un contexto vacío
     */
    public static ExyliaContext create() {
        return new ExyliaContext();
    }

    /**
     * Crea un contexto con objetos iniciales
     */
    public static ExyliaContext of(Object... objects) {
        ExyliaContext exyliaContext = new ExyliaContext();
        exyliaContext.addAll(objects);
        return exyliaContext;
    }

    /**
     * Crea un contexto con un mapa de datos
     */
    public static ExyliaContext of(Map<String, Object> data) {
        ExyliaContext exyliaContext = new ExyliaContext();
        exyliaContext.putAll(data);
        return exyliaContext;
    }

    // ==================== AÑADIR DATOS ====================

    /**
     * Añade un objeto detectando automáticamente su tipo
     */
    public ExyliaContext add(Object object) {
        if (object != null) {
            typedData.put(object.getClass(), object);
        }
        return this;
    }

    /**
     * Añade múltiples objetos
     */
    public ExyliaContext addAll(Object... objects) {
        for (Object obj : objects) {
            add(obj);
        }
        return this;
    }

    /**
     * Añade múltiples objetos desde colección
     */
    public ExyliaContext addAll(Collection<Object> objects) {
        for (Object obj : objects) {
            add(obj);
        }
        return this;
    }

    /**
     * Añade un objeto con tipo específico
     */
    public <T> ExyliaContext add(Class<T> type, T object) {
        if (object != null) {
            typedData.put(type, object);
        }
        return this;
    }

    /**
     * Añade datos con clave string
     */
    public ExyliaContext put(String key, Object value) {
        if (key != null && value != null) {
            keyedData.put(key, value);
        }
        return this;
    }

    /**
     * Añade múltiples datos desde mapa
     */
    public ExyliaContext putAll(Map<String, Object> data) {
        if (data != null) {
            keyedData.putAll(data);
        }
        return this;
    }

    /**
     * Añade datos dinámicos que se calculan cuando se necesitan
     */
    public ExyliaContext putDynamic(String key, Supplier<Object> supplier) {
        if (key != null && supplier != null) {
            dynamicData.put(key, supplier);
        }
        return this;
    }

    // ==================== OBTENER DATOS ====================

    /**
     * Obtiene un objeto por su tipo
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object value = typedData.get(type);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Obtiene datos por clave string
     */
    public Object get(String key) {
        // Primero buscar en datos estáticos
        Object value = keyedData.get(key);
        if (value != null) {
            return value;
        }

        // Luego buscar en datos dinámicos
        Supplier<Object> supplier = dynamicData.get(key);
        if (supplier != null) {
            return supplier.get();
        }

        return null;
    }

    /**
     * Obtiene datos por clave con tipo específico
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Busca un objeto en todos los datos disponibles
     */
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> type) {
        // Buscar primero en datos tipados
        T result = get(type);
        if (result != null) {
            return result;
        }

        // Buscar en datos con clave
        for (Object value : keyedData.values()) {
            if (type.isInstance(value)) {
                return (T) value;
            }
        }

        // Buscar en datos dinámicos
        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (type.isInstance(value)) {
                    return (T) value;
                }
            } catch (Exception ignored) {
                // Ignorar errores en datos dinámicos
            }
        }

        return null;
    }

    /**
     * Busca todos los objetos de un tipo específico
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> type) {
        List<T> results = new ArrayList<>();

        // Buscar en datos tipados
        T typed = get(type);
        if (typed != null) {
            results.add(typed);
        }

        // Buscar en datos con clave
        for (Object value : keyedData.values()) {
            if (type.isInstance(value)) {
                results.add((T) value);
            }
        }

        // Buscar en datos dinámicos
        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (type.isInstance(value)) {
                    results.add((T) value);
                }
            } catch (Exception ignored) {
                // Ignorar errores en datos dinámicos
            }
        }

        return results;
    }

    // ==================== VERIFICACIÓN DE EXISTENCIA ====================

    /**
     * Verifica si existe un tipo específico
     */
    public boolean has(Class<?> type) {
        return typedData.containsKey(type);
    }

    /**
     * Verifica si existe una clave específica
     */
    public boolean has(String key) {
        return keyedData.containsKey(key) || dynamicData.containsKey(key);
    }

    /**
     * Verifica si el contexto contiene algún objeto del tipo especificado
     */
    public boolean contains(Class<?> type) {
        return find(type) != null;
    }

    // ==================== OPERACIONES DE CONTEXTO ====================

    /**
     * Fusiona otro contexto en este
     */
    public ExyliaContext merge(ExyliaContext other) {
        if (other != null) {
            this.typedData.putAll(other.typedData);
            this.keyedData.putAll(other.keyedData);
            this.dynamicData.putAll(other.dynamicData);
        }
        return this;
    }

    /**
     * Crea una copia del contexto
     */
    public ExyliaContext copy() {
        ExyliaContext copy = new ExyliaContext();
        copy.typedData.putAll(this.typedData);
        copy.keyedData.putAll(this.keyedData);
        copy.dynamicData.putAll(this.dynamicData);
        return copy;
    }

    /**
     * Crea un contexto hijo que hereda de este pero puede ser modificado independientemente
     */
    public ExyliaContext createChild() {
        return copy();
    }

    /**
     * Limpia todos los datos
     */
    public ExyliaContext clear() {
        typedData.clear();
        keyedData.clear();
        dynamicData.clear();
        return this;
    }

    // ==================== INTEGRACIÓN CON SISTEMA DE PLACEHOLDERS ====================

    /**
     * Procesa placeholders usando este contexto
     */
    public String processPlaceholders(String text, Player player) {
        return PlaceholderSystemManager.getInstance().process(text, player, this.getAllObjects());
    }

    /**
     * Procesa placeholders sin jugador
     */
    public String processPlaceholders(String text) {
        return processPlaceholders(text, null);
    }

    /**
     * Obtiene todos los objetos como array para el sistema de placeholders
     */
    public Object[] getAllObjects() {
        List<Object> allObjects = new ArrayList<>();

        allObjects.add(this);

        // Añadir objetos tipados
        allObjects.addAll(typedData.values());

        // Añadir datos con clave
        allObjects.addAll(keyedData.values());

        // Evaluar y añadir datos dinámicos
        for (Supplier<Object> supplier : dynamicData.values()) {
            try {
                Object value = supplier.get();
                if (value != null) {
                    allObjects.add(value);
                }
            } catch (Exception ignored) {
                // Ignorar errores en datos dinámicos
            }
        }

        return allObjects.toArray();
    }

    // ==================== MÉTODOS ÚTILES ====================

    /**
     * Obtiene el tamaño total del contexto
     */
    public int size() {
        return typedData.size() + keyedData.size() + dynamicData.size();
    }

    /**
     * Verifica si el contexto está vacío
     */
    public boolean isEmpty() {
        return typedData.isEmpty() && keyedData.isEmpty() && dynamicData.isEmpty();
    }

    /**
     * Obtiene todas las claves de datos estáticos
     */
    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>(keyedData.keySet());
        keys.addAll(dynamicData.keySet());
        return keys;
    }

    /**
     * Obtiene todos los tipos disponibles
     */
    public Set<Class<?>> getTypes() {
        return new HashSet<>(typedData.keySet());
    }

    // ==================== MÉTODOS DE CONVENIENCIA ====================

    /**
     * Añade datos específicos comunes del sistema
     */
    public ExyliaContext withPlayer(Player player) {
        return add(Player.class, player);
    }

    /**
     * Añade datos de tiempo actual
     */
    public ExyliaContext withCurrentTime() {
        return putDynamic("current_time", System::currentTimeMillis)
                .putDynamic("current_date", () -> new Date().toString());
    }

    /**
     * Añade contador dinámico
     */
    public ExyliaContext withCounter(String key) {
        final int[] counter = {0};
        return putDynamic(key, () -> ++counter[0]);
    }

    // ==================== DEBUG Y INFORMACIÓN ====================

    /**
     * Información de debug del contexto
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Context{");

        if (!typedData.isEmpty()) {
            sb.append("typed=[");
            typedData.forEach((type, value) ->
                    sb.append(type.getSimpleName()).append(":").append(value.getClass().getSimpleName()).append(", "));
            sb.append("], ");
        }

        if (!keyedData.isEmpty()) {
            sb.append("keyed=[");
            keyedData.forEach((key, value) ->
                    sb.append(key).append(":").append(value.getClass().getSimpleName()).append(", "));
            sb.append("], ");
        }

        if (!dynamicData.isEmpty()) {
            sb.append("dynamic=[");
            dynamicData.keySet().forEach(key -> sb.append(key).append(", "));
            sb.append("], ");
        }

        sb.append("total=").append(size()).append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ExyliaContext that = (ExyliaContext) obj;
        return Objects.equals(typedData, that.typedData) &&
                Objects.equals(keyedData, that.keyedData);
        // No comparamos dynamicData porque son funciones
    }

    @Override
    public int hashCode() {
        return Objects.hash(typedData, keyedData);
    }
}