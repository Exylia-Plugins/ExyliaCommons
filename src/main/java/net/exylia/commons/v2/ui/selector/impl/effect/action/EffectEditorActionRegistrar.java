package net.exylia.commons.v2.ui.selector.impl.effect.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.effect.api.EffectAPI;
import net.exylia.commons.v2.effect.model.EffectContext;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectScope;
import net.exylia.commons.v2.effect.model.EffectType;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectClipboard;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorRegistry;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectEditorSession;
import net.exylia.commons.v2.ui.selector.impl.effect.EffectListClipboard;
import net.exylia.commons.v2.ui.selector.impl.effect.editor.menu.EffectEditMenu;
import net.exylia.commons.v2.ui.selector.impl.effect.editor.menu.EffectListMenu;
import net.exylia.commons.v2.ui.selector.impl.effect.editor.menu.EffectTypeSelectMenu;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.IconPickerAPI;
import net.exylia.commons.v2.ui.selector.impl.registry.RegistryPickerAPI;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Registers the {@code commons:effect_*} actions backing the effect editor UI.
 *
 * <p>Distinct from {@link PotionEffectEditorActionRegistrar}, which owns {@code commons:potion_*}
 * for the potion-effect editor.
 */
public final class EffectEditorActionRegistrar {

    private static final String NS = "commons";

    private EffectEditorActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":effect_list").isPresent()) return;

        session(plugin, "effect_list", (player, session) -> EffectListMenu.open(player, session));

        session(plugin, "effect_add", EffectTypeSelectMenu::open);

        ActionAPI.create("effect_add_type", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    EffectEditorSession session = EffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;

                    EffectType type = EffectType.fromName(args.getString(0, ""));
                    if (type == null) return;

                    handleAddType(player, session, type);
                })
                .build();

        entry(plugin, "effect_edit", (player, session, entry) -> EffectEditMenu.open(player, entry));

        entry(plugin, "effect_delete", (player, session, entry) -> {
            session.removeEntry(entry.getId());
            EffectListMenu.open(player, session);
        });

        entry(plugin, "effect_copy", (player, session, entry) -> {
            EffectClipboard.copy(player, entry);
            player.sendMessage(ColorAPI.parse("{success}Effect copied to clipboard."));
            EffectListMenu.open(player, session);
        });

        session(plugin, "effect_paste", (player, session) -> {
            EffectEntry pasted = EffectClipboard.paste(player);
            if (pasted == null) {
                player.sendMessage(ColorAPI.parse("{error}No effect in clipboard."));
                return;
            }
            session.addEntry(pasted);
            EffectListMenu.open(player, session);
        });

        session(plugin, "effect_copy_all", (player, session) -> {
            EffectListClipboard.copy(player, session.getEntries());
            player.sendMessage(ColorAPI.parse("{success}Copied " + session.getEntries().size() + " effect(s) to clipboard."));
            EffectListMenu.open(player, session);
        });

        session(plugin, "effect_paste_all", (player, session) -> {
            List<EffectEntry> pasted = EffectListClipboard.paste(player);
            if (pasted == null || pasted.isEmpty()) {
                player.sendMessage(ColorAPI.parse("{error}No effect list in clipboard."));
                return;
            }
            pasted.forEach(session::addEntry);
            player.sendMessage(ColorAPI.parse("{success}Pasted " + pasted.size() + " effect(s)."));
            EffectListMenu.open(player, session);
        });

        session(plugin, "effect_save", (player, session) -> {
            EffectEditorRegistry.getInstance().remove(player);
            if (session.getOnSave() != null) {
                session.getOnSave().accept(player, List.copyOf(session.getEntries()));
            }
        });

        session(plugin, "effect_cancel", (player, session) -> {
            EffectEditorRegistry.getInstance().remove(player);
            if (session.getOnCancel() != null) session.getOnCancel().run();
        });

        entry(plugin, "effect_preview", (player, session, entry) ->
                EffectAPI.playSingle(entry, EffectContext.builder()
                        .player(player)
                        .skipProbability(true)
                        .skipConditions(true)
                        .skipPermissionCheck(true)
                        .skipDelay(true)
                        .build()));

        registerValueActions(plugin);
        registerMetadataActions(plugin);
    }

    // ------------------------------------------------------------ value edits

    private static void registerValueActions(JavaPlugin plugin) {
        entry(plugin, "effect_set_value", EffectEditorActionRegistrar::handleSetValue);

        entry(plugin, "effect_set_count", (player, session, entry) ->
                ChatInputAPI.integer(player, "Particle count (1 - 1000)")
                        .range(1, 1000)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setParticleCount(value.intValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_offset", (player, session, entry) ->
                ChatInputAPI.text(player, "Offset as x y z (e.g. 0.3 0.5 0.3)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            String[] parts = value.trim().split("\\s+");
                            if (parts.length == 3) {
                                entry.setOffsetX(parseDouble(parts[0], entry.getOffsetX()));
                                entry.setOffsetY(parseDouble(parts[1], entry.getOffsetY()));
                                entry.setOffsetZ(parseDouble(parts[2], entry.getOffsetZ()));
                            } else {
                                player.sendMessage(ColorAPI.parse("{error}Expected three numbers: x y z"));
                            }
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_color", (player, session, entry) ->
                ChatInputAPI.text(player, "Color as hex (#ff6b9d) or r,g,b — 'none' to clear")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setParticleColor(value.equalsIgnoreCase("none") ? null : value);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_volume", (player, session, entry) ->
                ChatInputAPI.decimal(player, "Sound volume (0.0 - 10.0)")
                        .range(0.0, 10.0)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setSoundVolume(value.floatValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_pitch", (player, session, entry) ->
                ChatInputAPI.decimal(player, "Sound pitch (0.5 - 2.0)")
                        .range(0.5, 2.0)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setSoundPitch(value.floatValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_amplifier", (player, session, entry) ->
                ChatInputAPI.integer(player, "Potion amplifier (0 = level I)")
                        .range(0, 255)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setPotionAmplifier(value.intValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_duration", (player, session, entry) ->
                ChatInputAPI.integer(player, "Potion duration in seconds")
                        .range(1, 86400)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setPotionDurationTicks(value.intValue() * 20);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_subtitle", (player, session, entry) ->
                ChatInputAPI.text(player, "Subtitle text (or 'none' to clear)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setSubtitle(value.equalsIgnoreCase("none") ? null : value);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_times", (player, session, entry) ->
                ChatInputAPI.text(player, "Title times in ticks as fadeIn stay fadeOut (e.g. 10 70 20)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            String[] parts = value.trim().split("\\s+");
                            if (parts.length == 3) {
                                entry.setTitleFadeIn((int) parseDouble(parts[0], entry.getTitleFadeIn()));
                                entry.setTitleStay((int) parseDouble(parts[1], entry.getTitleStay()));
                                entry.setTitleFadeOut((int) parseDouble(parts[2], entry.getTitleFadeOut()));
                            } else {
                                player.sendMessage(ColorAPI.parse("{error}Expected three numbers: fadeIn stay fadeOut"));
                            }
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_toggle_centered", (player, session, entry) -> {
            entry.setCentered(!entry.isCentered());
            save(player, session, entry);
        });

        entry(plugin, "effect_add_step", (player, session, entry) ->
                ChatInputAPI.text(player, "Sequence step (e.g. [CIRCLE] FLAME;radius:1.2)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            List<String> steps = new ArrayList<>(entry.getSequence());
                            steps.add(value);
                            entry.setSequence(steps);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_clear_steps", (player, session, entry) -> {
            entry.setSequence(new ArrayList<>());
            save(player, session, entry);
        });

        entry(plugin, "effect_set_extra", (player, session, entry) ->
                ChatInputAPI.decimal(player, "Particle speed / extra (0.0 - 10.0)")
                        .range(0.0, 10.0)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setParticleExtra(value.doubleValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_colors", (player, session, entry) ->
                ChatInputAPI.text(player, "Firework colors, comma separated (e.g. #ff6b9d, #ffd700)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setFireworkColors(splitColors(value));
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_fade_colors", (player, session, entry) ->
                ChatInputAPI.text(player, "Firework fade colors, comma separated ('none' to clear)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setFireworkFadeColors(value.equalsIgnoreCase("none")
                                    ? new ArrayList<>() : splitColors(value));
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_power", (player, session, entry) ->
                ChatInputAPI.integer(player, "Firework flight power (0 - 3)")
                        .range(0, 3)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setFireworkPower(value.intValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_toggle_flicker", (player, session, entry) -> {
            entry.setFireworkFlicker(!entry.isFireworkFlicker());
            save(player, session, entry);
        });

        entry(plugin, "effect_toggle_trail", (player, session, entry) -> {
            entry.setFireworkTrail(!entry.isFireworkTrail());
            save(player, session, entry);
        });

        entry(plugin, "effect_toggle_potion_particles", (player, session, entry) -> {
            entry.setPotionParticles(!entry.isPotionParticles());
            save(player, session, entry);
        });

        entry(plugin, "effect_toggle_potion_icon", (player, session, entry) -> {
            entry.setPotionIcon(!entry.isPotionIcon());
            save(player, session, entry);
        });
    }

    /**
     * Splits a comma separated color list while keeping {@code r,g,b} triplets intact:
     * numeric tokens are accumulated in groups of three, non-numeric tokens (hex) stand alone.
     * So {@code "#ff6b9d, 255,215,0"} yields {@code ["#ff6b9d", "255,215,0"]}.
     */
    private static List<String> splitColors(String raw) {
        List<String> colors = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.chars().allMatch(Character::isDigit)) {
                pending.add(trimmed);
                if (pending.size() == 3) {
                    colors.add(String.join(",", pending));
                    pending.clear();
                }
            } else {
                pending.clear();
                colors.add(trimmed);
            }
        }
        return colors;
    }

    // --------------------------------------------------------- metadata edits

    private static void registerMetadataActions(JavaPlugin plugin) {
        entry(plugin, "effect_set_name", (player, session, entry) ->
                ChatInputAPI.text(player, "Display name (or 'none' to use the value)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setName(value.equalsIgnoreCase("none") ? null : value);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_clear_name", (player, session, entry) -> {
            entry.setName(null);
            save(player, session, entry);
        });

        entry(plugin, "effect_set_chance", (player, session, entry) ->
                ChatInputAPI.decimal(player, "Chance (0.01 - 100.0)")
                        .range(0.01, 100.0)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setChance(value.doubleValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_condition", (player, session, entry) ->
                ChatInputAPI.text(player, "Condition expression (or 'none' to clear)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setCondition(value.equalsIgnoreCase("none") ? null : value);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_clear_condition", (player, session, entry) -> {
            entry.setCondition(null);
            save(player, session, entry);
        });

        entry(plugin, "effect_set_permission", (player, session, entry) ->
                ChatInputAPI.text(player, "Required permission (or 'none' to clear)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setPermission(value.equalsIgnoreCase("none") ? null : value);
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_clear_permission", (player, session, entry) -> {
            entry.setPermission(null);
            save(player, session, entry);
        });

        entry(plugin, "effect_set_priority", (player, session, entry) ->
                ChatInputAPI.integer(player, "Priority (higher = first, default 0)")
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setPriority(value.intValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_delay", (player, session, entry) ->
                ChatInputAPI.integer(player, "Delay in ticks before playing (0 = immediate)")
                        .range(0, 72000)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setDelayTicks(value.longValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_cycle_scope", (player, session, entry) -> {
            EffectScope[] scopes = EffectScope.values();
            EffectScope current = entry.getScope() == null ? EffectScope.PLAYER : entry.getScope();
            entry.setScope(scopes[(current.ordinal() + 1) % scopes.length]);
            save(player, session, entry);
        });

        entry(plugin, "effect_set_radius", (player, session, entry) ->
                ChatInputAPI.decimal(player, "Radius in blocks (1.0 - 256.0)")
                        .range(1.0, 256.0)
                        .onCancel(() -> EffectEditMenu.open(player, entry))
                        .onResponse(value -> {
                            entry.setRadius(value.doubleValue());
                            save(player, session, entry);
                        })
                        .ask());

        entry(plugin, "effect_set_icon", (player, session, entry) ->
                IconPickerAPI.open(
                        player,
                        () -> EffectEditMenu.open(player, entry),
                        snapshot -> {
                            entry.setIcon(snapshot.serialize());
                            save(player, session, entry);
                        }));

        entry(plugin, "effect_clear_icon", (player, session, entry) -> {
            entry.setIcon(null);
            save(player, session, entry);
        });
    }

    // ---------------------------------------------------------------- helpers

    /** Registers an action that needs the active session. */
    private static void session(JavaPlugin plugin, String id, BiConsumer<Player, EffectEditorSession> handler) {
        ActionAPI.create(id, plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    EffectEditorSession session = EffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    handler.accept(player, session);
                })
                .build();
    }

    /** Registers an action that needs the entry identified by the first argument. */
    private static void entry(JavaPlugin plugin, String id, EntryHandler handler) {
        ActionAPI.create(id, plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    EffectEditorSession session = EffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    EffectEntry entry = session.findById(args.getString(0, ""));
                    if (entry == null) return;
                    handler.accept(player, session, entry);
                })
                .build();
    }

    @FunctionalInterface
    private interface EntryHandler {
        void accept(Player player, EffectEditorSession session, EffectEntry entry);
    }

    private static void save(Player player, EffectEditorSession session, EffectEntry entry) {
        session.replaceEntry(entry);
        EffectEditMenu.open(player, entry);
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------ type flows

    private static void handleAddType(Player player, EffectEditorSession session, EffectType type) {
        switch (type) {
            // Registry-backed types use a browsable picker so admins never have to recall an id.
            case PARTICLE -> pick(RegistryPickerAPI.particle(player), session, player,
                    value -> EffectEntry.particle(value));

            case SOUND -> pick(RegistryPickerAPI.sound(player), session, player,
                    value -> EffectEntry.sound(value));

            case POTION -> pick(RegistryPickerAPI.potionEffect(player), session, player,
                    value -> EffectEntry.potion(value));

            case TITLE -> ask(player, session, "Title text (supports color codes)",
                    value -> EffectEntry.title(value, null));

            case ACTIONBAR -> ask(player, session, "Actionbar text (supports color codes)",
                    EffectEntry::actionbar);

            case MESSAGE -> ask(player, session, "Message text (supports color codes)",
                    EffectEntry::message);

            case SEQUENCE -> ask(player, session, "First sequence step (e.g. [CIRCLE] FLAME;radius:1.2)",
                    value -> EffectEntry.sequence(List.of(value)));

            case FIREWORK -> pick(RegistryPickerAPI.fireworkShape(player), session, player, value -> {
                EffectEntry entry = EffectEntry.firework();
                entry.setFireworkType(value);
                return entry;
            });
        }
    }

    /** Opens a registry picker, adding the built entry on pick and returning to the type list. */
    private static void pick(
            RegistryPickerAPI.Picker picker,
            EffectEditorSession session,
            Player player,
            java.util.function.Function<String, EffectEntry> factory
    ) {
        picker.onPick(value -> {
                    session.addEntry(factory.apply(value));
                    EffectListMenu.open(player, session);
                })
                .onCancel(() -> EffectTypeSelectMenu.open(player, session))
                .open();
    }

    private static void ask(
            Player player,
            EffectEditorSession session,
            String prompt,
            java.util.function.Function<String, EffectEntry> factory
    ) {
        ChatInputAPI.text(player, prompt)
                .onCancel(() -> EffectTypeSelectMenu.open(player, session))
                .onResponse(value -> {
                    session.addEntry(factory.apply(value));
                    EffectListMenu.open(player, session);
                })
                .ask();
    }

    private static void handleSetValue(Player player, EffectEditorSession session, EffectEntry entry) {
        switch (entry.getType()) {
            case PARTICLE -> repick(RegistryPickerAPI.particle(player), player, session, entry,
                    entry::setParticle);

            case SOUND -> repick(RegistryPickerAPI.sound(player), player, session, entry,
                    entry::setSound);

            case POTION -> repick(RegistryPickerAPI.potionEffect(player), player, session, entry,
                    entry::setPotion);

            case FIREWORK -> repick(RegistryPickerAPI.fireworkShape(player), player, session, entry,
                    entry::setFireworkType);

            case TITLE -> edit(player, session, entry, "Title text (supports color codes)",
                    entry::setTitle);

            case ACTIONBAR -> edit(player, session, entry, "Actionbar text (supports color codes)",
                    entry::setActionbar);

            case MESSAGE -> edit(player, session, entry, "Message text (supports color codes)",
                    entry::setMessage);

            case SEQUENCE -> edit(player, session, entry, "Replace all steps with a single step",
                    value -> entry.setSequence(new ArrayList<>(List.of(value))));
        }
    }

    /** Registry picker variant of {@link #edit}, for fields backed by a Minecraft registry. */
    private static void repick(
            RegistryPickerAPI.Picker picker,
            Player player,
            EffectEditorSession session,
            EffectEntry entry,
            java.util.function.Consumer<String> setter
    ) {
        picker.onPick(value -> {
                    setter.accept(value);
                    save(player, session, entry);
                })
                .onCancel(() -> EffectEditMenu.open(player, entry))
                .open();
    }

    private static void edit(
            Player player,
            EffectEditorSession session,
            EffectEntry entry,
            String prompt,
            java.util.function.Consumer<String> setter
    ) {
        ChatInputAPI.text(player, prompt)
                .onCancel(() -> EffectEditMenu.open(player, entry))
                .onResponse(value -> {
                    setter.accept(value);
                    save(player, session, entry);
                })
                .ask();
    }
}
