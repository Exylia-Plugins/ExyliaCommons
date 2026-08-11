package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A "pick one of these" request, rendered as a grid of buttons in a Minecraft dialog (or an
 * inventory when the client cannot use dialogs).
 *
 * <p>Supports optional paging and a search box, so it stays usable for large registries such as
 * the ~107 particles or ~1539 sounds, where a single flat list would be unusable.
 */
@Getter
@Setter
public class SingleOptionRequest extends InputRequest<String> {

    /** Buttons per page when paging is enabled. Dialogs cap out well before this many rows. */
    public static final int DEFAULT_PAGE_SIZE = 45;

    private final List<OptionEntry> options = new ArrayList<>();
    private Consumer<String> onResponse;
    private int columns = 1;

    /** When true a search button is shown, letting the player filter by name. */
    private boolean searchable = false;

    /** Buttons per page. {@code <= 0} disables paging (renders everything at once). */
    private int pageSize = 0;

    /** Current page, zero-based. Mutated as the player navigates; not part of the result. */
    private int page = 0;

    /** Active search filter, or null when unfiltered. */
    private String query;

    public SingleOptionRequest(Player player, String prompt) {
        super(player, prompt);
    }

    public void addOption(String key, String label) {
        options.add(new OptionEntry(key, label));
    }

    public List<OptionEntry> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public boolean isPaged() {
        return pageSize > 0;
    }

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    /**
     * @return the options matching the active query, or every option when unfiltered. Matching is
     * case-insensitive and checks both key and label, so a player can type either the friendly
     * name ("happy villager") or the raw id ("HAPPY_VILLAGER").
     */
    public List<OptionEntry> getFilteredOptions() {
        if (!hasQuery()) return getOptions();

        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<OptionEntry> matches = new ArrayList<>();
        for (OptionEntry entry : options) {
            if (matches(entry, needle)) matches.add(entry);
        }
        return Collections.unmodifiableList(matches);
    }

    private static boolean matches(OptionEntry entry, String needle) {
        if (entry.key() != null && entry.key().toLowerCase(Locale.ROOT).contains(needle)) return true;
        if (entry.label() == null) return false;
        // Underscores are stripped so "happy villager" matches the HAPPY_VILLAGER id too.
        String label = entry.label().toLowerCase(Locale.ROOT).replace('_', ' ');
        return label.contains(needle) || label.contains(needle.replace('_', ' '));
    }

    /** @return total pages for the current filter, always at least 1. */
    public int getTotalPages() {
        if (!isPaged()) return 1;
        int size = getFilteredOptions().size();
        if (size == 0) return 1;
        return (size + pageSize - 1) / pageSize;
    }

    /** Clamps {@link #page} into range, e.g. after a query shrinks the result set. */
    public void clampPage() {
        int total = getTotalPages();
        if (page >= total) page = total - 1;
        if (page < 0) page = 0;
    }

    /** @return the options to render right now, after filtering and paging. */
    public List<OptionEntry> getVisibleOptions() {
        List<OptionEntry> filtered = getFilteredOptions();
        if (!isPaged()) return filtered;

        clampPage();
        int from = page * pageSize;
        if (from >= filtered.size()) return Collections.emptyList();
        return filtered.subList(from, Math.min(from + pageSize, filtered.size()));
    }

    /**
     * Resolves an index within the currently visible page back to its option, so handlers can
     * keep sending compact per-button indices instead of full keys.
     *
     * @return the matching entry, or null when the index is stale/out of range
     */
    public OptionEntry resolveVisible(int visibleIndex) {
        List<OptionEntry> visible = getVisibleOptions();
        if (visibleIndex < 0 || visibleIndex >= visible.size()) return null;
        return visible.get(visibleIndex);
    }

    @Override
    public void accept(String value) {
        if (onResponse != null) {
            onResponse.accept(value);
        }
    }
}
