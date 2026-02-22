package net.exylia.commons.v2.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;

public class ConfigMerger {

    public static boolean merge(Config config, Collection<String> optionalPaths) {
        String resourcePath = config.getFileName().replace("\\", "/") + ".yml";
        YamlConfiguration resourceYaml = loadResourceYaml(config, resourcePath);
        if (resourceYaml == null) return false;

        Set<String> absentOptionals = resolveAbsentOptionals(resourceYaml, config.raw(), optionalPaths);

        boolean changed = mergeSection(resourceYaml, config.raw(), "", absentOptionals);
        if (changed) {
            config.save();
            config.reload();
        }
        return changed;
    }

    private static YamlConfiguration loadResourceYaml(Config config, String resourcePath) {
        ClassLoader classLoader = Configs.getResourceClassLoader();
        InputStream in = classLoader != null
                ? classLoader.getResourceAsStream(resourcePath)
                : config.getPlugin().getResource(resourcePath);
        if (in == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static Set<String> resolveAbsentOptionals(YamlConfiguration resource,
                                                      ConfigurationSection disk,
                                                      Collection<String> optionalPaths) {
        Set<String> absent = new java.util.HashSet<>();
        for (String path : optionalPaths) {
            if (resource.contains(path) && !disk.contains(path)) {
                absent.add(path);
            }
        }
        return absent;
    }

    private static boolean mergeSection(ConfigurationSection resource, ConfigurationSection disk,
                                        String currentPath, Set<String> absentOptionals) {
        boolean changed = false;

        for (String key : resource.getKeys(false)) {
            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;

            if (disk.contains(key)) {
                Object resourceValue = resource.get(key);
                if (resourceValue instanceof ConfigurationSection resourceSection
                        && disk.getConfigurationSection(key) != null) {
                    changed |= mergeSection(resourceSection, disk.getConfigurationSection(key),
                            fullPath, absentOptionals);
                }
                continue;
            }

            if (isUnderAbsentOptional(fullPath, absentOptionals)) continue;

            Object value = resource.get(key);
            if (value instanceof ConfigurationSection resourceSection) {
                ConfigurationSection newSection = disk.createSection(key);
                mergeSection(resourceSection, newSection, fullPath, absentOptionals);
                changed = true;
            } else {
                disk.set(key, value);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean isUnderAbsentOptional(String fullPath, Set<String> absentOptionals) {
        for (String optional : absentOptionals) {
            if (fullPath.equals(optional) || fullPath.startsWith(optional + ".")) {
                return true;
            }
        }
        return false;
    }
}
