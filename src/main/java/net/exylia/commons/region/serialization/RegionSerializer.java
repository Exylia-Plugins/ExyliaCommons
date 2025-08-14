package net.exylia.commons.region.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.model.RegionFlag;
import net.exylia.commons.region.model.RegionFlagType;
import net.exylia.commons.region.model.RegionPriority;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.database.serialization.SerializationUtils;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Location;

import java.util.*;

/**
 * Serializador especializado para Region que guarda solo datos persistentes
 * CORREGIDO: Ahora maneja correctamente la metadata y otros datos
 */
public class RegionSerializer {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Location.class, (JsonSerializer<Location>) (src, typeOfSrc, context) ->
                    context.serialize(SerializationUtils.serializeLocation(src)))
            .registerTypeAdapter(Location.class, (JsonDeserializer<Location>) (json, typeOfT, context) ->
                    SerializationUtils.deserializeLocation(json.getAsString()))
            .registerTypeAdapter(Selection.class, new SelectionTypeAdapter())
            .registerTypeAdapter(UUID.class, (JsonSerializer<UUID>) (src, typeOfSrc, context) ->
                    context.serialize(src.toString()))
            .registerTypeAdapter(UUID.class, (JsonDeserializer<UUID>) (json, typeOfT, context) ->
                    UUID.fromString(json.getAsString()))
            .setPrettyPrinting()
            .serializeNulls() // IMPORTANTE: Incluir valores null para mantener estructura completa
            .create();

    /**
     * Datos persistentes de una región (solo lo que se guarda en DB)
     * CORREGIDO: Manejo mejorado de metadata
     */
    public static class RegionData {
        public String id;
        public Selection selection;
        public long createdAt;
        public String displayName;
        public String description;
        public RegionPriority priority;
        public Map<String, Object> metadata;
        public Map<String, String> flagStates;
        public Set<String> owners;
        public Set<String> members;

        public RegionData() {
            this.metadata = new HashMap<>();
            this.flagStates = new HashMap<>();
            this.owners = new HashSet<>();
            this.members = new HashSet<>();
        }

        public RegionData(Region region) {
            this.id = region.getId();
            this.selection = region.getSelection();
            this.createdAt = region.getCreatedAt();
            this.displayName = region.getDisplayName();
            this.description = region.getDescription();
            this.priority = region.getPriority();

            // CORREGIDO: Clonar metadata de forma más profunda y segura
            this.metadata = new HashMap<>();
            if (region.getMetadata() != null) {
                for (Map.Entry<String, Object> entry : region.getMetadata().entrySet()) {
                    Object value = entry.getValue();

                    // Manejar tipos especiales que necesitan serialización especial
                    if (value instanceof Set) {
                        // Para sets como allowed-blocks, convertir a lista para JSON
                        this.metadata.put(entry.getKey(), new ArrayList<>((Set<?>) value));
                    } else if (value instanceof Map) {
                        // Para mapas, clonar
                        this.metadata.put(entry.getKey(), new HashMap<>((Map<?, ?>) value));
                    } else {
                        // Para tipos primitivos y otros, copiar directamente
                        this.metadata.put(entry.getKey(), value);
                    }
                }
            }

            // Convertir flags a String keys
            this.flagStates = new HashMap<>();
            if (region.getConfiguredFlags() != null) {
                for (Map.Entry<RegionFlag, RegionFlagType> entry : region.getConfiguredFlags().entrySet()) {
                    this.flagStates.put(entry.getKey().name(), entry.getValue().name());
                }
            }

            // Convertir UUIDs a Strings
            this.owners = new HashSet<>();
            if (region.getOwners() != null) {
                for (UUID uuid : region.getOwners()) {
                    this.owners.add(uuid.toString());
                }
            }

            this.members = new HashSet<>();
            if (region.getMembers() != null) {
                for (UUID uuid : region.getMembers()) {
                    this.members.add(uuid.toString());
                }
            }
        }
    }

    /**
     * Serializa una Region a JSON (solo datos persistentes)
     * CORREGIDO: Validación mejorada
     */
    public static String serialize(Region region) {
        if (region == null) return null;

        try {
            RegionData data = new RegionData(region);
            String result = GSON.toJson(data);

            // Validar que el resultado no esté vacío
            if (result == null || result.trim().isEmpty() || "{}".equals(result.trim())) {
                throw new RuntimeException("Serialization resulted in empty or null JSON");
            }

            return result;
        } catch (Exception e) {
            // Log detallado para debugging
            DebugUtils.logError("Error serializing Region: " + region.getId() + " - " + e.getMessage());
            DebugUtils.logError("Region metadata: " + region.getMetadata());
            e.printStackTrace();
            throw new RuntimeException("Error serializando Region: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializa una Region desde JSON
     * CORREGIDO: Manejo mejorado de metadata
     */
    public static Region deserialize(String json) {
        if (json == null || json.trim().isEmpty()) return null;

        try {
            RegionData data = GSON.fromJson(json, RegionData.class);
            return createRegionFromData(data);
        } catch (Exception e) {
            DebugUtils.logError("Error deserializing Region JSON: " + json.substring(0, Math.min(100, json.length())) + "...");
            throw new RuntimeException("Error deserializando Region: " + e.getMessage(), e);
        }
    }

    /**
     * Crea una instancia completa de Region desde los datos persistentes
     * CORREGIDO: Restauración mejorada de metadata
     */
    private static Region createRegionFromData(RegionData data) {
        // Crear la región base
        Region region = new Region(data.id, data.selection);

        // Establecer timestamp original
        if (data.createdAt > 0) {
            // El constructor ya establece createdAt, pero podríamos usar reflection si necesitáramos el valor original
            // Por ahora, mantenemos el comportamiento actual
        }

        // Restaurar datos persistentes
        if (data.displayName != null) {
            region.setDisplayName(data.displayName);
        }
        if (data.description != null) {
            region.setDescription(data.description);
        }
        if (data.priority != null) {
            region.setPriority(data.priority);
        }

        // CORREGIDO: Restaurar metadata de forma más cuidadosa
        if (data.metadata != null && !data.metadata.isEmpty()) {
            Map<String, Object> restoredMetadata = new HashMap<>();

            for (Map.Entry<String, Object> entry : data.metadata.entrySet()) {
                Object value = entry.getValue();

                // Manejar reconversión de tipos especiales
                if (value instanceof List && entry.getKey().equals("allowed-blocks")) {
                    // Convertir lista de vuelta a Set para allowed-blocks
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) value;
                    restoredMetadata.put(entry.getKey(), new HashSet<>(list));
                } else {
                    // Para otros tipos, usar tal como vienen
                    restoredMetadata.put(entry.getKey(), value);
                }
            }

            region.setMetadata(restoredMetadata);
        }

        // Restaurar flags desde strings
        if (data.flagStates != null && !data.flagStates.isEmpty()) {
            Map<RegionFlag, RegionFlagType> flags = new EnumMap<>(RegionFlag.class);
            for (Map.Entry<String, String> entry : data.flagStates.entrySet()) {
                try {
                    RegionFlag flag = RegionFlag.valueOf(entry.getKey());
                    RegionFlagType type = RegionFlagType.valueOf(entry.getValue());
                    flags.put(flag, type);
                } catch (IllegalArgumentException e) {
                    // Ignorar flags inválidas (backward compatibility)
                    DebugUtils.logError("Ignoring invalid flag during deserialization: " + entry.getKey() + "=" + entry.getValue());
                }
            }
            region.setFlags(flags);
        }

        // Restaurar owners desde strings
        if (data.owners != null && !data.owners.isEmpty()) {
            Set<UUID> ownerUUIDs = new HashSet<>();
            for (String uuidString : data.owners) {
                try {
                    ownerUUIDs.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    DebugUtils.logError("Ignoring invalid owner UUID during deserialization: " + uuidString);
                }
            }
            region.setOwners(ownerUUIDs);
        }

        // Restaurar members desde strings
        if (data.members != null && !data.members.isEmpty()) {
            Set<UUID> memberUUIDs = new HashSet<>();
            for (String uuidString : data.members) {
                try {
                    memberUUIDs.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    DebugUtils.logError("Ignoring invalid member UUID during deserialization: " + uuidString);
                }
            }
            region.setMembers(memberUUIDs);
        }

        // Los datos transientes (playersInside, callbacks) se inicializan vacíos
        // y se llenarán cuando sea necesario durante el runtime

        return region;
    }

    /**
     * Verifica si un JSON es una Region válida
     * CORREGIDO: Validación más completa
     */
    public static boolean isValidRegionJson(String json) {
        if (json == null || json.trim().isEmpty()) return false;

        try {
            RegionData data = GSON.fromJson(json, RegionData.class);
            return data != null &&
                    data.id != null && !data.id.isEmpty() &&
                    data.selection != null &&
                    data.selection.isComplete(); // Verificar que la selección esté completa
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene el tamaño aproximado en bytes del JSON serializado
     */
    public static int getSerializedSize(Region region) {
        String json = serialize(region);
        return json != null ? json.getBytes().length : 0;
    }

    /**
     * TypeAdapter para Selection (necesario para serialización custom)
     */
    private static class SelectionTypeAdapter implements JsonSerializer<Selection>, JsonDeserializer<Selection> {

        @Override
        public com.google.gson.JsonElement serialize(Selection src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("pos1", SerializationUtils.serializeLocation(src.getPos1()));
            obj.addProperty("pos2", SerializationUtils.serializeLocation(src.getPos2()));
            return obj;
        }

        @Override
        public Selection deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) {
            com.google.gson.JsonObject obj = json.getAsJsonObject();
            Location pos1 = SerializationUtils.deserializeLocation(obj.get("pos1").getAsString());
            Location pos2 = SerializationUtils.deserializeLocation(obj.get("pos2").getAsString());
            return new Selection(pos1, pos2);
        }
    }
}