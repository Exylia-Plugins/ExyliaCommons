package net.exylia.commons.v2.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.model.ItemData;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NavigationData {

    private ItemData previousButton;
    private int previousButtonSlot;

    private ItemData nextButton;
    private int nextButtonSlot;

    private ItemData infoItem;
    private int infoItemSlot;

    public boolean hasPreviousButton() {
        return previousButton != null && previousButtonSlot >= 0;
    }

    public boolean hasNextButton() {
        return nextButton != null && nextButtonSlot >= 0;
    }

    public boolean hasInfoItem() {
        return infoItem != null && infoItemSlot >= 0;
    }

    public NavigationData copy() {
        return NavigationData.builder()
                .previousButton(previousButton != null ? previousButton.copy() : null)
                .previousButtonSlot(previousButtonSlot)
                .nextButton(nextButton != null ? nextButton.copy() : null)
                .nextButtonSlot(nextButtonSlot)
                .infoItem(infoItem != null ? infoItem.copy() : null)
                .infoItemSlot(infoItemSlot)
                .build();
    }
}
