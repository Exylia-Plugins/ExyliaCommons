package net.exylia.commons.v2.yaml.util;

import net.exylia.commons.v2.yaml.exception.YamlParseException;
import net.exylia.commons.v2.yaml.exception.YamlStorageException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlFileManager {

    private final Yaml yaml;

    public YamlFileManager() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public Map<String, Object> readYaml(Path file) throws IOException {
        try {
            if (!Files.exists(file)) {
                return Collections.emptyMap();
            }

            Object loaded = yaml.load(Files.newInputStream(file));
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return Collections.emptyMap();

        } catch (Exception e) {
            throw new YamlParseException("Failed to read YAML file: " + file, e);
        }
    }

    public void writeYaml(Path file, Map<String, Object> data) throws IOException {
        try {
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }

            String yamlContent = yaml.dump(data);
            Files.writeString(file, yamlContent);

        } catch (Exception e) {
            throw new YamlStorageException("Failed to write YAML file: " + file, e);
        }
    }

    public List<Path> listYamlFiles(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new YamlStorageException("Failed to list YAML files in: " + directory, e);
        }
    }

    public void createDirectories(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public boolean validateYaml(Path file) {
        try {
            if (!Files.exists(file)) {
                return false;
            }

            Map<String, Object> data = yaml.load(Files.newInputStream(file));
            return data != null && !data.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

    public String sanitizeId(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }

        String idStr = id.toString();
        return idStr.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
