package net.exylia.commons.v2.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.animation.AnimationSettings;
import net.exylia.commons.v2.ui.refresh.RefreshMode;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MenuData {

    @Builder.Default
    private String title = "Menu";

    @Builder.Default
    private MenuType type = MenuType.SIMPLE;

    @Builder.Default
    private int size = 54;

    @Builder.Default
    private RefreshMode refreshMode = RefreshMode.DISABLED;

    @Builder.Default
    private long refreshInterval = 20L;

    @Builder.Default
    private long clickRefreshDelay = 1L;

    private ItemData globalFiller;

    private ItemData borderFiller;

    private ItemData paginationFiller;

    @Builder.Default
    private Map<String, ItemData> items = new LinkedHashMap<>();

    @Builder.Default
    private List<Integer> paginationSlots = new ArrayList<>();

    @Builder.Default
    private List<ItemData> paginationItems = new ArrayList<>();

    private ItemData paginationItemTemplate;

    private NavigationData paginationNavigation;

    private transient Supplier<List<ItemData>> paginationItemsSupplier;

    @Builder.Default
    private List<SectionData> sections = new ArrayList<>();

    @Builder.Default
    private boolean snapshotEnabled = false;

    private String snapshotId;

    @Builder.Default
    private boolean restoreOnClose = true;

    @Builder.Default
    private boolean playerInventoryEnabled = false;

    @Builder.Default
    private List<Integer> allowedPlayerSlots = new ArrayList<>();

    @Builder.Default
    private List<Integer> editableSlots = new ArrayList<>();

    private transient Map<Integer, Consumer<ItemStack>> captureSlots;

    @Builder.Default
    private PlaceholderContext context = PlaceholderContext.create();

    @Builder.Default
    private List<String> openSounds = new ArrayList<>();

    @Builder.Default
    private List<String> closeSounds = new ArrayList<>();

    @Builder.Default
    private List<String> clickSounds = new ArrayList<>();

    @Builder.Default
    private List<FillerData> customFillers = new ArrayList<>();

    @Builder.Default
    private AnimationSettings animationSettings = AnimationSettings.builder().build();

    public void addCaptureSlot(int slot, Consumer<ItemStack> handler) {
        if (captureSlots == null) captureSlots = new HashMap<>();
        captureSlots.put(slot, handler);
    }

    public boolean hasCaptureSlot(int slot) {
        return captureSlots != null && captureSlots.containsKey(slot);
    }

    public Consumer<ItemStack> getCaptureSlotHandler(int slot) {
        return captureSlots != null ? captureSlots.get(slot) : null;
    }

    public boolean hasGlobalFiller() {
        return globalFiller != null;
    }

    public boolean hasCustomFillers() {
        return customFillers != null && !customFillers.isEmpty();
    }

    public boolean hasOpenSounds() {
        return openSounds != null && !openSounds.isEmpty();
    }

    public boolean hasCloseSounds() {
        return closeSounds != null && !closeSounds.isEmpty();
    }

    public boolean hasClickSounds() {
        return clickSounds != null && !clickSounds.isEmpty();
    }

    public boolean hasBorderFiller() {
        return borderFiller != null;
    }

    public boolean hasPaginationFiller() {
        return paginationFiller != null;
    }

    public boolean hasPagination() {
        return !paginationSlots.isEmpty() && (!paginationItems.isEmpty() || paginationItemsSupplier != null);
    }

    public boolean hasPaginationItemTemplate() {
        return paginationItemTemplate != null;
    }

    public <T> MenuData withPaginationData(List<T> dataList, Function<T, PlaceholderContext> contextMapper) {
        if (paginationItemTemplate == null) {
            return this;
        }

        List<ItemData> generatedItems = new ArrayList<>();
        for (T data : dataList) {
            PlaceholderContext ctx = contextMapper.apply(data);
            ItemData item = paginationItemTemplate.copy().toBuilder()
                    .context(ctx)
                    .build();
            generatedItems.add(item);
        }

        this.paginationItems = generatedItems;
        return this;
    }

    public <T> MenuData withPaginationSupplier(Supplier<List<T>> dataSupplier, Function<T, PlaceholderContext> contextMapper) {
        if (paginationItemTemplate == null) {
            return this;
        }

        ItemData template = paginationItemTemplate;
        this.paginationItemsSupplier = () -> {
            List<T> data = dataSupplier.get();
            List<ItemData> items = new ArrayList<>(data.size());
            for (T element : data) {
                PlaceholderContext ctx = contextMapper.apply(element);
                items.add(template.copy().toBuilder().context(ctx).build());
            }
            return items;
        };

        this.paginationItems = this.paginationItemsSupplier.get();
        return this;
    }

    public boolean hasSections() {
        return !sections.isEmpty();
    }

    public int getRows() {
        return size / 9;
    }

    public MenuData copy() {
        Map<String, ItemData> copiedItems = new LinkedHashMap<>();
        items.forEach((key, item) -> copiedItems.put(key, item != null ? item.copy() : null));

        List<ItemData> copiedPaginationItems = new ArrayList<>();
        for (ItemData item : paginationItems) {
            copiedPaginationItems.add(item != null ? item.copy() : null);
        }

        List<SectionData> copiedSections = new ArrayList<>();
        for (SectionData section : sections) {
            copiedSections.add(section != null ? section.copy() : null);
        }

        List<FillerData> copiedCustomFillers = new ArrayList<>();
        for (FillerData filler : customFillers) {
            copiedCustomFillers.add(filler != null ? filler.copy() : null);
        }

        MenuData copy = MenuData.builder()
                .title(title)
                .type(type)
                .size(size)
                .refreshMode(refreshMode)
                .refreshInterval(refreshInterval)
                .clickRefreshDelay(clickRefreshDelay)
                .globalFiller(globalFiller != null ? globalFiller.copy() : null)
                .borderFiller(borderFiller != null ? borderFiller.copy() : null)
                .paginationFiller(paginationFiller != null ? paginationFiller.copy() : null)
                .items(copiedItems)
                .paginationSlots(new ArrayList<>(paginationSlots))
                .paginationItems(copiedPaginationItems)
                .paginationItemTemplate(paginationItemTemplate != null ? paginationItemTemplate.copy() : null)
                .paginationNavigation(paginationNavigation != null ? paginationNavigation.copy() : null)
                .paginationItemsSupplier(paginationItemsSupplier)
                .sections(copiedSections)
                .snapshotEnabled(snapshotEnabled)
                .snapshotId(snapshotId)
                .restoreOnClose(restoreOnClose)
                .playerInventoryEnabled(playerInventoryEnabled)
                .allowedPlayerSlots(new ArrayList<>(allowedPlayerSlots))
                .editableSlots(new ArrayList<>(editableSlots))
                .context(context.copy())
                .openSounds(new ArrayList<>(openSounds))
                .closeSounds(new ArrayList<>(closeSounds))
                .clickSounds(new ArrayList<>(clickSounds))
                .customFillers(copiedCustomFillers)
                .animationSettings(animationSettings != null ? animationSettings.copy() : null)
                .build();
        if (captureSlots != null) {
            copy.captureSlots = new HashMap<>(captureSlots);
        }
        return copy;
    }
}
