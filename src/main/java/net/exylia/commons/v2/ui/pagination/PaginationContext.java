package net.exylia.commons.v2.ui.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaginationContext {

    @Builder.Default
    private int currentPage = 1;

    @Builder.Default
    private Map<String, Integer> sectionPages = new ConcurrentHashMap<>();

    @Builder.Default
    private Map<String, Integer> selectedIndices = new ConcurrentHashMap<>();

    public int getSectionPage(String sectionName) {
        return sectionPages.getOrDefault(sectionName, 1);
    }

    public void setSectionPage(String sectionName, int page) {
        sectionPages.put(sectionName, page);
    }

    public Integer getSelectedIndex(String sectionName) {
        return selectedIndices.get(sectionName);
    }

    public void setSelectedIndex(String sectionName, Integer index) {
        if (index == null) {
            selectedIndices.remove(sectionName);
        } else {
            selectedIndices.put(sectionName, index);
        }
    }

    public void clear() {
        sectionPages.clear();
        selectedIndices.clear();
        currentPage = 1;
    }
}
