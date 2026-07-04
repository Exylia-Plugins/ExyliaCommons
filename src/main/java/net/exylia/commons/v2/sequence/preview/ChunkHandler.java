package net.exylia.commons.v2.sequence.preview;

import org.bukkit.entity.Player;

interface ChunkHandler {

    void clear(Player player, int radius);

    void restore(Player player, int radius);

    ChunkHandler NOOP = new ChunkHandler() {
        public void clear(Player player, int radius) {}
        public void restore(Player player, int radius) {}
    };
}
