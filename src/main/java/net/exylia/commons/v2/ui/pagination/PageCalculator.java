package net.exylia.commons.v2.ui.pagination;

import java.util.ArrayList;
import java.util.List;

public class PageCalculator {

    public static int getTotalPages(int totalItems, int itemsPerPage) {
        if (itemsPerPage <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
    }

    public static int clampPage(int page, int totalPages) {
        return Math.max(1, Math.min(page, totalPages));
    }

    public static <T> List<T> getPageItems(List<T> allItems, int page, int itemsPerPage) {
        if (allItems == null || allItems.isEmpty()) {
            return new ArrayList<>();
        }

        int totalPages = getTotalPages(allItems.size(), itemsPerPage);
        page = clampPage(page, totalPages);

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        if (startIndex >= allItems.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allItems.subList(startIndex, endIndex));
    }

    public static int getStartIndex(int page, int itemsPerPage) {
        return (page - 1) * itemsPerPage;
    }

    public static int getEndIndex(int page, int itemsPerPage, int totalItems) {
        int startIndex = getStartIndex(page, itemsPerPage);
        return Math.min(startIndex + itemsPerPage, totalItems);
    }
}
