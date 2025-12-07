package net.exylia.commons.v2.hologram.core;

import net.exylia.commons.v2.hologram.exception.HologramException;
import net.exylia.commons.v2.hologram.model.Hologram;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class HologramRegistry {
    private final ConcurrentHashMap<String, Hologram> holograms = new ConcurrentHashMap<>();

    public void register(Hologram hologram) {
        if (holograms.containsKey(hologram.getId())) {
            throw new HologramException.HologramAlreadyExistsException(hologram.getId());
        }
        holograms.put(hologram.getId(), hologram);
    }

    public void unregister(String id) {
        holograms.remove(id);
    }

    public Optional<Hologram> get(String id) {
        return Optional.ofNullable(holograms.get(id));
    }

    public Collection<Hologram> getAll() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public boolean contains(String id) {
        return holograms.containsKey(id);
    }

    public void clear() {
        holograms.clear();
    }

    public int size() {
        return holograms.size();
    }
}
