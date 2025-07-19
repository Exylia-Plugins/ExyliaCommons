package net.exylia.commons.database.annotations;

public enum SerializationType {
    AUTO,       // Detecta automáticamente el mejor tipo
    JSON,       // Para mapas, listas, objetos simples
    BASE64,     // Para ItemStack, objetos complejos de Bukkit
    STRING,     // Para Location, Component (formato custom)
    YAML        // Para configuraciones legibles
}
