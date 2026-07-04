package net.exylia.commons.v2.sequence.preview;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import org.bukkit.entity.Player;

class PacketEventsChunkHandler implements ChunkHandler {

    @Override
    public void clear(Player player, int radius) {
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(player, new WrapperPlayServerUnloadChunk(cx + dx, cz + dz));
            }
        }
    }

    @Override
    public void restore(Player player, int radius) {
        ChunkResendSupport.restore(player, radius);
    }
}
