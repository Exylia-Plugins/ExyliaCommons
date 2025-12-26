package net.exylia.commons.v2.channel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessage {

    private String channelId;
    private String senderName;
    private UUID senderId;
    private String message;
    private String serverName;
    private long timestamp;
}
