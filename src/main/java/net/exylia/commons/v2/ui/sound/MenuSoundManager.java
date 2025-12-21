package net.exylia.commons.v2.ui.sound;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MenuSoundManager {
    private final Map<String, SoundConfig> soundPresets = new ConcurrentHashMap<>();

    public void loadPresetsFromConfig(FileConfiguration config) {
        soundPresets.clear();

        ConfigurationSection soundsSection = config.getConfigurationSection("sounds.presets");
        if (soundsSection == null) {
            return;
        }

        for (String key : soundsSection.getKeys(false)) {
            ConfigurationSection soundConfig = soundsSection.getConfigurationSection(key);
            if (soundConfig != null) {
                SoundConfig preset = parseSoundConfig(soundConfig);
                if (preset != null) {
                    soundPresets.put(key, preset);
                }
            }
        }
    }

    public SoundConfig parseSoundConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String soundKey = section.getString("sound");
        if (soundKey == null) {
            return null;
        }

        return SoundConfig.builder()
            .soundKey(soundKey)
            .volume((float) section.getDouble("volume", 1.0))
            .pitch((float) section.getDouble("pitch", 1.0))
            .delay(section.getLong("delay", 0L))
            .async(section.getBoolean("async", false))
            .build();
    }

    public Optional<SoundConfig> getPreset(String name) {
        return Optional.ofNullable(soundPresets.get(name));
    }

    public void registerPreset(String name, SoundConfig sound) {
        soundPresets.put(name, sound);
    }

    public void unregisterPreset(String name) {
        soundPresets.remove(name);
    }

    public void clear() {
        soundPresets.clear();
    }

    public int getPresetCount() {
        return soundPresets.size();
    }
}
