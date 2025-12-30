package net.exylia.commons.v2.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.model.ItemData;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SectionData {

    private String name;

    @Builder.Default
    private List<Integer> slots = new ArrayList<>();

    @Builder.Default
    private List<ItemData> items = new ArrayList<>();

    private NavigationData navigation;

    private ItemData fillerItem;

    private ItemData selectedItemTemplate;

    public int getItemsPerPage() {
        return slots.size();
    }

    public int getTotalPages(int totalItems) {
        if (slots.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) totalItems / slots.size()));
    }

    public boolean hasNavigation() {
        return navigation != null;
    }

    public boolean hasFillerItem() {
        return fillerItem != null;
    }

    public boolean hasSelectedTemplate() {
        return selectedItemTemplate != null;
    }

    public SectionData copy() {
        List<ItemData> copiedItems = new ArrayList<>();
        for (ItemData item : items) {
            copiedItems.add(item != null ? item.copy() : null);
        }

        return SectionData.builder()
                .name(name)
                .slots(new ArrayList<>(slots))
                .items(copiedItems)
                .navigation(navigation != null ? navigation.copy() : null)
                .fillerItem(fillerItem != null ? fillerItem.copy() : null)
                .selectedItemTemplate(selectedItemTemplate != null ? selectedItemTemplate.copy() : null)
                .build();
    }
}
