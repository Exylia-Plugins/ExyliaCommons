package net.exylia.commons.v2.ui.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import org.bukkit.entity.Player;

public class InventoryPacketListener extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow wrapper = new WrapperPlayServerOpenWindow(event);
            Player player = event.getPlayer();
            if (player != null) {
                ContainerIdTracker.setContainerId(player.getUniqueId(), wrapper.getContainerId());
            }
        } else if (packetType == PacketType.Play.Server.CLOSE_WINDOW) {
            WrapperPlayServerCloseWindow wrapper = new WrapperPlayServerCloseWindow(event);
            Player player = event.getPlayer();
            if (player != null && wrapper.getWindowId() != 0) {
                ContainerIdTracker.remove(player.getUniqueId());
            }
        }
    }
}
