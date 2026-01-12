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
public class FillerData {

    private String name;

    private ItemData itemData;

    @Builder.Default
    private List<Integer> slots = new ArrayList<>();

    public FillerData copy() {
        return FillerData.builder()
                .name(name)
                .itemData(itemData != null ? itemData.copy() : null)
                .slots(new ArrayList<>(slots))
                .build();
    }
}
