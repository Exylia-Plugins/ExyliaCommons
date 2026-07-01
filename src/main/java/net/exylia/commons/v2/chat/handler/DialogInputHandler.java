package net.exylia.commons.v2.chat.handler;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.BooleanInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.InputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.common.client.WrapperCommonClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.common.server.WrapperCommonServerClearDialog;
import com.github.retrooper.packetevents.wrapper.common.server.WrapperCommonServerShowDialog;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerClearDialog;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerShowDialog;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.chat.request.BooleanInputRequest;
import net.exylia.commons.v2.chat.request.ConfirmationRequest;
import net.exylia.commons.v2.chat.request.MultiNumberInputRequest;
import net.exylia.commons.v2.chat.request.NumberFieldDef;
import net.exylia.commons.v2.chat.request.NumberInputRequest;
import net.exylia.commons.v2.chat.request.OptionEntry;
import net.exylia.commons.v2.chat.request.SingleOptionRequest;
import net.exylia.commons.v2.chat.request.TextInputRequest;
import net.exylia.commons.v2.chat.session.InputSession;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DialogInputHandler {

    private static final String NAMESPACE = "exyliacommons";

    private final Map<UUID, InputSession> pendingSessions = new ConcurrentHashMap<>();
    private PacketListenerCommon packetListener;

    public void registerEventListener() {
        packetListener = new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                WrapperCommonClientCustomClickAction<?> packet;
                if (event.getPacketType() == PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
                    packet = new WrapperPlayClientCustomClickAction(event);
                } else if (event.getPacketType() == PacketType.Configuration.Client.CUSTOM_CLICK_ACTION) {
                    packet = new WrapperConfigClientCustomClickAction(event);
                } else {
                    return;
                }
                handleCustomClickAction(packet);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    public void unregisterEventListener() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
    }

    public boolean show(InputSession session) {
        if (session.getRequest() instanceof SingleOptionRequest) {
            return showOptionButtonDialog(session);
        }
        if (session.getRequest() instanceof MultiNumberInputRequest) {
            return showMultiNumberDialog(session);
        }
        try {
            Player player = session.getPlayer();
            String sessionKey = session.getSessionId().toString().replace("-", "");
            String inputKey = "input_" + sessionKey;

            InputControl inputControl = buildInputControl(session);
            if (inputControl == null) return false;

            ActionButton submitButton = new ActionButton(
                new CommonButtonData(ColorAPI.parse(ChatInputDefaults.ChatInput.Dialog.SUBMIT_LABEL), null, 200),
                new DynamicCustomAction(new ResourceLocation(NAMESPACE, "submit-" + sessionKey), null)
            );
            ActionButton cancelButton = new ActionButton(
                new CommonButtonData(ColorAPI.parse(ChatInputDefaults.ChatInput.Dialog.CANCEL_LABEL), null, 200),
                new DynamicCustomAction(new ResourceLocation(NAMESPACE, "cancel-" + sessionKey), null)
            );

            CommonDialogData data = new CommonDialogData(
                ColorAPI.parse(session.getRequest().getPrompt()),
                null,
                true,
                false,
                DialogAction.CLOSE,
                Collections.emptyList(),
                List.of(new Input(inputKey, inputControl))
            );

            Dialog dialog = new MultiActionDialog(data, List.of(submitButton), cancelButton, 1);

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) return false;

            WrapperCommonServerShowDialog<?> wrapper = user.getConnectionState() == ConnectionState.CONFIGURATION
                ? new WrapperConfigServerShowDialog(dialog)
                : new WrapperPlayServerShowDialog(dialog);
            user.sendPacket(wrapper);

            pendingSessions.put(player.getUniqueId(), session);
            DebugAPI.logLibDebug("[Dialog] Shown for " + player.getName() + " sessionKey=" + sessionKey);
            return true;
        } catch (Exception e) {
            DebugAPI.logLibWarn("[Dialog] show() failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean showOptionButtonDialog(InputSession session) {
        try {
            Player player = session.getPlayer();
            String sessionKey = session.getSessionId().toString().replace("-", "");
            SingleOptionRequest optionReq = (SingleOptionRequest) session.getRequest();

            List<ActionButton> optionButtons = new ArrayList<>();
            List<OptionEntry> options = optionReq.getOptions();
            for (int i = 0; i < options.size(); i++) {
                optionButtons.add(new ActionButton(
                    new CommonButtonData(ColorAPI.parse(options.get(i).label()), null, 200),
                    new DynamicCustomAction(new ResourceLocation(NAMESPACE, "opt-" + i + "-" + sessionKey), null)
                ));
            }

            ActionButton cancelButton = new ActionButton(
                new CommonButtonData(ColorAPI.parse(ChatInputDefaults.ChatInput.Dialog.CANCEL_LABEL), null, 200),
                new DynamicCustomAction(new ResourceLocation(NAMESPACE, "cancel-" + sessionKey), null)
            );

            CommonDialogData data = new CommonDialogData(
                ColorAPI.parse(session.getRequest().getPrompt()),
                null, true, false, DialogAction.CLOSE,
                Collections.emptyList(),
                Collections.emptyList()
            );

            Dialog dialog = new MultiActionDialog(data, optionButtons, cancelButton, optionReq.getColumns());

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) return false;

            WrapperCommonServerShowDialog<?> wrapper = user.getConnectionState() == ConnectionState.CONFIGURATION
                ? new WrapperConfigServerShowDialog(dialog)
                : new WrapperPlayServerShowDialog(dialog);
            user.sendPacket(wrapper);

            pendingSessions.put(player.getUniqueId(), session);
            DebugAPI.logLibDebug("[Dialog] Option buttons shown for " + player.getName() + " sessionKey=" + sessionKey);
            return true;
        } catch (Exception e) {
            DebugAPI.logLibWarn("[Dialog] showOptionButtonDialog() failed: " + e.getMessage());
            return false;
        }
    }

    private boolean showMultiNumberDialog(InputSession session) {
        try {
            Player player = session.getPlayer();
            String sessionKey = session.getSessionId().toString().replace("-", "");
            MultiNumberInputRequest multiReq = (MultiNumberInputRequest) session.getRequest();

            List<Input> inputs = new ArrayList<>();
            for (NumberFieldDef field : multiReq.getFields()) {
                String fieldKey = "field_" + field.key() + "_" + sessionKey;
                int maxLen = field.decimals() ? 20 : 19;
                String defaultValue = field.decimals()
                    ? String.valueOf(field.min())
                    : String.valueOf((long) field.min());
                inputs.add(new Input(fieldKey, new TextInputControl(
                    200,
                    Component.text(field.label()),
                    true,
                    defaultValue,
                    maxLen,
                    null
                )));
            }

            ActionButton submitButton = new ActionButton(
                new CommonButtonData(ColorAPI.parse(ChatInputDefaults.ChatInput.Dialog.SUBMIT_LABEL), null, 200),
                new DynamicCustomAction(new ResourceLocation(NAMESPACE, "submit-" + sessionKey), null)
            );
            ActionButton cancelButton = new ActionButton(
                new CommonButtonData(ColorAPI.parse(ChatInputDefaults.ChatInput.Dialog.CANCEL_LABEL), null, 200),
                new DynamicCustomAction(new ResourceLocation(NAMESPACE, "cancel-" + sessionKey), null)
            );

            CommonDialogData data = new CommonDialogData(
                ColorAPI.parse(session.getRequest().getPrompt()),
                null, true, false, DialogAction.CLOSE,
                Collections.emptyList(),
                inputs
            );

            Dialog dialog = new MultiActionDialog(data, List.of(submitButton), cancelButton, 1);

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) return false;

            WrapperCommonServerShowDialog<?> wrapper = user.getConnectionState() == ConnectionState.CONFIGURATION
                ? new WrapperConfigServerShowDialog(dialog)
                : new WrapperPlayServerShowDialog(dialog);
            user.sendPacket(wrapper);

            pendingSessions.put(player.getUniqueId(), session);
            DebugAPI.logLibDebug("[Dialog] Multi-number shown for " + player.getName() + " sessionKey=" + sessionKey);
            return true;
        } catch (Exception e) {
            DebugAPI.logLibWarn("[Dialog] showMultiNumberDialog() failed: " + e.getMessage());
            return false;
        }
    }

    public void close(InputSession session) {
        pendingSessions.remove(session.getPlayer().getUniqueId());
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(session.getPlayer());
            if (user == null) return;
            WrapperCommonServerClearDialog<?> wrapper = user.getConnectionState() == ConnectionState.CONFIGURATION
                ? new WrapperConfigServerClearDialog()
                : new WrapperPlayServerClearDialog();
            user.sendPacket(wrapper);
        } catch (Exception ignored) {}
    }

    private InputControl buildInputControl(InputSession session) {
        if (session.getRequest() instanceof TextInputRequest textReq) {
            int maxLen = textReq.getMaxLength() > 0 ? textReq.getMaxLength() : 32767;
            return new TextInputControl(200, Component.empty(), false, "", maxLen, null);
        } else if (session.getRequest() instanceof NumberInputRequest numReq) {
            int maxLen = numReq.isDecimals() ? 20 : 19;
            return new TextInputControl(200, Component.empty(), false, "", maxLen, null);
        } else if (session.getRequest() instanceof BooleanInputRequest || session.getRequest() instanceof ConfirmationRequest) {
            return new BooleanInputControl(ColorAPI.parse(session.getRequest().getPrompt()), false, "true", "false");
        }
        return null;
    }

    private void handleCustomClickAction(WrapperCommonClientCustomClickAction<?> packet) {
        try {
            ResourceLocation id = packet.getId();
            if (!NAMESPACE.equals(id.getNamespace())) return;

            String key = id.getKey();
            if (key.startsWith("submit-")) {
                String sessionKey = key.substring(7);
                UUID playerUuid = findPlayerBySessionKey(sessionKey);
                if (playerUuid == null) return;
                InputSession session = pendingSessions.remove(playerUuid);
                if (session == null) return;
                NBT nbt = packet.getPayload();
                NBTCompound compound = nbt instanceof NBTCompound c ? c : new NBTCompound();
                Object value = extractResponse(session, compound, sessionKey);
                if (value != null) {
                    ChatInputManager.getInstance().handleCompleteFromHandler(playerUuid, value);
                }
            } else if (key.startsWith("cancel-")) {
                String sessionKey = key.substring(7);
                UUID playerUuid = findPlayerBySessionKey(sessionKey);
                if (playerUuid == null) return;
                InputSession session = pendingSessions.remove(playerUuid);
                if (session == null) return;
                ChatInputManager.getInstance().handleCancelFromHandler(session.getPlayer());
            } else if (key.startsWith("opt-")) {
                String withoutPrefix = key.substring(4);
                int separatorIdx = withoutPrefix.lastIndexOf('-');
                if (separatorIdx < 0) return;
                String sessionKey = withoutPrefix.substring(separatorIdx + 1);
                String indexStr = withoutPrefix.substring(0, separatorIdx);
                UUID playerUuid = findPlayerBySessionKey(sessionKey);
                if (playerUuid == null) return;
                InputSession session = pendingSessions.remove(playerUuid);
                if (session == null) return;
                if (!(session.getRequest() instanceof SingleOptionRequest optReq)) return;
                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    return;
                }
                if (index < 0 || index >= optReq.getOptions().size()) return;
                ChatInputManager.getInstance().handleCompleteFromHandler(playerUuid, optReq.getOptions().get(index).key());
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("[Dialog] handleCustomClickAction failed: " + e.getMessage());
        }
    }

    private UUID findPlayerBySessionKey(String sessionKey) {
        for (Map.Entry<UUID, InputSession> entry : pendingSessions.entrySet()) {
            if (entry.getValue().getSessionId().toString().replace("-", "").equals(sessionKey)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Object extractResponse(InputSession session, NBTCompound compound, String sessionKey) {
        String inputKey = "input_" + sessionKey;
        if (session.getRequest() instanceof TextInputRequest textReq) {
            String value = compound.getStringTagValueOrNull(inputKey);
            if (value != null && textReq.getTransformer() != null) {
                value = textReq.getTransformer().apply(value);
            }
            if (value != null && textReq.getValidator() != null && !textReq.getValidator().test(value)) {
                return null;
            }
            return value;
        } else if (session.getRequest() instanceof NumberInputRequest numReq) {
            String raw = compound.getStringTagValueOrNull(inputKey);
            if (raw == null || raw.isBlank()) return null;
            try {
                if (numReq.isDecimals()) {
                    double d = Double.parseDouble(raw);
                    if (d < numReq.getMin() || d > numReq.getMax()) return null;
                    return d;
                } else {
                    long l = Long.parseLong(raw);
                    if (l < numReq.getMin() || l > numReq.getMax()) return null;
                    return l;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (session.getRequest() instanceof MultiNumberInputRequest multiReq) {
            Map<String, Number> result = new LinkedHashMap<>();
            for (NumberFieldDef field : multiReq.getFields()) {
                String fieldKey = "field_" + field.key() + "_" + sessionKey;
                String raw = compound.getStringTagValueOrNull(fieldKey);
                if (raw == null || raw.isBlank()) return null;
                try {
                    if (field.decimals()) {
                        double d = Double.parseDouble(raw);
                        if (d < field.min() || d > field.max()) return null;
                        result.put(field.key(), d);
                    } else {
                        long l = Long.parseLong(raw);
                        if (l < (long) field.min() || l > (long) field.max()) return null;
                        result.put(field.key(), l);
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return result.isEmpty() ? null : result;
        } else if (session.getRequest() instanceof BooleanInputRequest || session.getRequest() instanceof ConfirmationRequest) {
            NBTByte nbt = compound.getTagOfTypeOrNull(inputKey, NBTByte.class);
            return nbt == null ? null : nbt.getAsBool();
        }
        return null;
    }
}
