package net.exylia.commons.v2.config.schema;

import org.bukkit.configuration.ConfigurationSection;

public interface ConfigSerializer<T> {

    Object serialize(T value);

    T deserialize(ConfigurationSection section);

    Class<T> getType();
}
