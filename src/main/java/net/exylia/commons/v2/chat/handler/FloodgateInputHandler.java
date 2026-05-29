package net.exylia.commons.v2.chat.handler;

import net.exylia.commons.v2.chat.config.ChatInputDefaults;
import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.chat.request.BooleanInputRequest;
import net.exylia.commons.v2.chat.request.ConfirmationRequest;
import net.exylia.commons.v2.chat.request.NumberInputRequest;
import net.exylia.commons.v2.chat.request.OptionEntry;
import net.exylia.commons.v2.chat.request.SingleOptionRequest;
import net.exylia.commons.v2.chat.request.TextInputRequest;
import net.exylia.commons.v2.chat.request.InputRequest;
import net.exylia.commons.v2.chat.session.InputSession;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class FloodgateInputHandler {

    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern LEGACY_COLOR = Pattern.compile("&[0-9a-fA-FklmnorKLMNOR]");

    public void show(InputSession session) {
        Player player = session.getPlayer();
        InputRequest<?> request = session.getRequest();
        DebugAPI.logLibDebug("[Bedrock] Showing form to " + player.getName() + " type=" + request.getClass().getSimpleName());

        if (request instanceof TextInputRequest textReq) {
            showTextForm(player, textReq);
        } else if (request instanceof NumberInputRequest numReq) {
            showNumberForm(player, numReq);
        } else if (request instanceof BooleanInputRequest boolReq) {
            showBooleanForm(player, boolReq);
        } else if (request instanceof ConfirmationRequest confirmReq) {
            showConfirmationForm(player, confirmReq);
        } else if (request instanceof SingleOptionRequest optionReq) {
            showOptionForm(player, optionReq);
        } else {
            DebugAPI.logLibDebug("[Bedrock] Unknown request type for " + player.getName() + " — no form shown");
        }
    }

    public void close(InputSession session) {
        DebugAPI.logLibDebug("[Bedrock] Session closed for " + session.getPlayer().getName());
    }

    private void showTextForm(Player player, TextInputRequest request) {
        UUID uuid = player.getUniqueId();
        String title = strip(request.getPrompt());
        CustomForm form = CustomForm.builder()
            .title(title)
            .input("", "")
            .validResultHandler(response -> {
                String input = response.asInput(0);
                if (request.getTransformer() != null) {
                    input = request.getTransformer().apply(input);
                }
                if (request.getMaxLength() > 0 && input.length() > request.getMaxLength()) {
                    DebugAPI.logLibDebug("[Bedrock] Text input rejected for " + player.getName() + " — exceeded maxLength=" + request.getMaxLength());
                    TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                    showTextForm(player, request);
                    return;
                }
                if (request.getValidator() != null && !request.getValidator().test(input)) {
                    DebugAPI.logLibDebug("[Bedrock] Text input rejected for " + player.getName() + " — validator failed");
                    TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                    showTextForm(player, request);
                    return;
                }
                DebugAPI.logLibDebug("[Bedrock] Text input accepted for " + player.getName() + " value=\"" + input + "\"");
                String finalInput = input;
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCompleteFromHandler(uuid, finalInput);
                });
            })
            .closedOrInvalidResultHandler(() -> {
                DebugAPI.logLibDebug("[Bedrock] Text form closed/cancelled by " + player.getName());
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCancelFromHandler(player);
                });
            })
            .build();
        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private void showNumberForm(Player player, NumberInputRequest request) {
        UUID uuid = player.getUniqueId();
        String title = strip(request.getPrompt());
        boolean hasRange = request.getMin() != -Double.MAX_VALUE && request.getMax() != Double.MAX_VALUE;

        if (!request.isDecimals() && hasRange) {
            float min = (float) request.getMin();
            float max = (float) request.getMax();
            DebugAPI.logLibDebug("[Bedrock] Number form (slider) for " + player.getName() + " range=[" + min + "," + max + "]");
            CustomForm form = CustomForm.builder()
                .title(title)
                .slider("", min, max, 1, min)
                .validResultHandler(response -> {
                    long value = Math.round((double) response.asSlider(0));
                    if (request.getValidator() != null && !request.getValidator().test(value)) {
                        DebugAPI.logLibDebug("[Bedrock] Slider input rejected for " + player.getName() + " — validator failed value=" + value);
                        TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                        showNumberForm(player, request);
                        return;
                    }
                    DebugAPI.logLibDebug("[Bedrock] Slider input accepted for " + player.getName() + " value=" + value);
                    TaskAPI.sync(() -> {
                        ChatInputManager mgr = ChatInputManager.getInstance();
                        if (mgr != null) mgr.handleCompleteFromHandler(uuid, value);
                    });
                })
                .closedOrInvalidResultHandler(() -> {
                    DebugAPI.logLibDebug("[Bedrock] Slider form closed/cancelled by " + player.getName());
                    TaskAPI.sync(() -> {
                        ChatInputManager mgr = ChatInputManager.getInstance();
                        if (mgr != null) mgr.handleCancelFromHandler(player);
                    });
                })
                .build();
            FloodgateApi.getInstance().sendForm(uuid, form);
        } else {
            DebugAPI.logLibDebug("[Bedrock] Number form (text input) for " + player.getName() + " decimals=" + request.isDecimals());
            CustomForm form = CustomForm.builder()
                .title(title)
                .input("", "")
                .validResultHandler(response -> {
                    String raw = response.asInput(0);
                    try {
                        if (request.isDecimals()) {
                            double d = Double.parseDouble(raw);
                            if (d < request.getMin() || d > request.getMax()) {
                                DebugAPI.logLibDebug("[Bedrock] Decimal input out of range for " + player.getName() + " value=" + d);
                                TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                                showNumberForm(player, request);
                                return;
                            }
                            if (request.getValidator() != null && !request.getValidator().test(d)) {
                                DebugAPI.logLibDebug("[Bedrock] Decimal input rejected for " + player.getName() + " — validator failed value=" + d);
                                TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                                showNumberForm(player, request);
                                return;
                            }
                            DebugAPI.logLibDebug("[Bedrock] Decimal input accepted for " + player.getName() + " value=" + d);
                            TaskAPI.sync(() -> {
                                ChatInputManager mgr = ChatInputManager.getInstance();
                                if (mgr != null) mgr.handleCompleteFromHandler(uuid, d);
                            });
                        } else {
                            long l = Long.parseLong(raw);
                            if (l < request.getMin() || l > request.getMax()) {
                                DebugAPI.logLibDebug("[Bedrock] Integer input out of range for " + player.getName() + " value=" + l);
                                TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                                showNumberForm(player, request);
                                return;
                            }
                            if (request.getValidator() != null && !request.getValidator().test(l)) {
                                DebugAPI.logLibDebug("[Bedrock] Integer input rejected for " + player.getName() + " — validator failed value=" + l);
                                TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                                showNumberForm(player, request);
                                return;
                            }
                            DebugAPI.logLibDebug("[Bedrock] Integer input accepted for " + player.getName() + " value=" + l);
                            TaskAPI.sync(() -> {
                                ChatInputManager mgr = ChatInputManager.getInstance();
                                if (mgr != null) mgr.handleCompleteFromHandler(uuid, l);
                            });
                        }
                    } catch (NumberFormatException e) {
                        DebugAPI.logLibDebug("[Bedrock] Number parse failed for " + player.getName() + " raw=\"" + raw + "\"");
                        TaskAPI.sync(() -> MessageAPI.send(player, ChatInputDefaults.ChatInput.ChatFallback.INVALID_MESSAGE));
                        showNumberForm(player, request);
                    }
                })
                .closedOrInvalidResultHandler(() -> {
                    DebugAPI.logLibDebug("[Bedrock] Number form closed/cancelled by " + player.getName());
                    TaskAPI.sync(() -> {
                        ChatInputManager mgr = ChatInputManager.getInstance();
                        if (mgr != null) mgr.handleCancelFromHandler(player);
                    });
                })
                .build();
            FloodgateApi.getInstance().sendForm(uuid, form);
        }
    }

    private void showBooleanForm(Player player, BooleanInputRequest request) {
        UUID uuid = player.getUniqueId();
        DebugAPI.logLibDebug("[Bedrock] Boolean form for " + player.getName());
        ModalForm form = ModalForm.builder()
            .title(strip(request.getPrompt()))
            .content("")
            .button1(strip(ChatInputDefaults.ChatInput.UIFallback.YES_LABEL))
            .button2(strip(ChatInputDefaults.ChatInput.UIFallback.NO_LABEL))
            .validResultHandler(response -> {
                boolean result = response.clickedFirst();
                DebugAPI.logLibDebug("[Bedrock] Boolean response from " + player.getName() + " = " + result);
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCompleteFromHandler(uuid, result);
                });
            })
            .closedOrInvalidResultHandler(() -> {
                DebugAPI.logLibDebug("[Bedrock] Boolean form closed/cancelled by " + player.getName());
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCancelFromHandler(player);
                });
            })
            .build();
        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private void showConfirmationForm(Player player, ConfirmationRequest request) {
        UUID uuid = player.getUniqueId();
        DebugAPI.logLibDebug("[Bedrock] Confirmation form for " + player.getName());
        ModalForm form = ModalForm.builder()
            .title(strip(request.getPrompt()))
            .content("")
            .button1(strip(ChatInputDefaults.ChatInput.UIFallback.YES_LABEL))
            .button2(strip(ChatInputDefaults.ChatInput.UIFallback.NO_LABEL))
            .validResultHandler(response -> {
                boolean result = response.clickedFirst();
                DebugAPI.logLibDebug("[Bedrock] Confirmation response from " + player.getName() + " = " + result);
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCompleteFromHandler(uuid, result);
                });
            })
            .closedOrInvalidResultHandler(() -> {
                DebugAPI.logLibDebug("[Bedrock] Confirmation form closed/cancelled by " + player.getName());
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCancelFromHandler(player);
                });
            })
            .build();
        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private void showOptionForm(Player player, SingleOptionRequest request) {
        UUID uuid = player.getUniqueId();
        List<OptionEntry> options = request.getOptions();
        DebugAPI.logLibDebug("[Bedrock] Option form for " + player.getName() + " options=" + options.size());
        SimpleForm.Builder builder = SimpleForm.builder()
            .title(strip(request.getPrompt()));
        for (OptionEntry entry : options) {
            builder.button(strip(entry.label()));
        }
        SimpleForm form = builder
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked < 0 || clicked >= options.size()) {
                    DebugAPI.logLibDebug("[Bedrock] Option response out of bounds for " + player.getName() + " clicked=" + clicked);
                    return;
                }
                String key = options.get(clicked).key();
                DebugAPI.logLibDebug("[Bedrock] Option selected by " + player.getName() + " index=" + clicked + " key=" + key);
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCompleteFromHandler(uuid, key);
                });
            })
            .closedOrInvalidResultHandler(() -> {
                DebugAPI.logLibDebug("[Bedrock] Option form closed/cancelled by " + player.getName());
                TaskAPI.sync(() -> {
                    ChatInputManager mgr = ChatInputManager.getInstance();
                    if (mgr != null) mgr.handleCancelFromHandler(player);
                });
            })
            .build();
        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    private static String strip(String text) {
        if (text == null) return "";
        String s = MINI_MESSAGE_TAG.matcher(text).replaceAll("");
        return LEGACY_COLOR.matcher(s).replaceAll("");
    }
}
