package net.exylia.commons.v2.ui.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.RefreshMode;
import net.exylia.commons.v2.ui.sound.SoundConfig;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class MenuConfig {
    private final String id;
    private final String title;
    @Builder.Default
    private final int rows = 3;
    @Builder.Default
    private final MenuType type = MenuType.SIMPLE;

    @Builder.Default
    private final Map<Integer, MenuItemConfig> items = new HashMap<>();
    private final MenuItemConfig globalFiller;
    private final MenuItemConfig borderFiller;

    @Builder.Default
    private final boolean dynamicUpdates = false;
    @Builder.Default
    private final long updateInterval = 20L;

    private final SoundConfig openSound;
    private final SoundConfig closeSound;
    private final SoundConfig clickSound;

    @Builder.Default
    private final RefreshMode refreshMode = RefreshMode.SMART;

    private final PaginationConfig paginationConfig;
    private final EditableConfig editableConfig;
    private final ConfirmationConfig confirmationConfig;
    private final SelectionConfig selectionConfig;

    @Getter
    @Builder
    public static class PaginationConfig {
        private final int[] itemSlots;
        private final MenuItemConfig previousButton;
        private final MenuItemConfig nextButton;
        private final int previousButtonSlot;
        private final int nextButtonSlot;
        private final MenuItemConfig itemSlotFiller;
    }

    @Getter
    @Builder
    public static class EditableConfig {
        private final int[] editableSlots;
        private final MenuItemConfig editableSlotFiller;
    }

    @Getter
    @Builder
    public static class ConfirmationConfig {
        private final String message;
        private final MenuItemConfig confirmButton;
        private final MenuItemConfig cancelButton;
        private final MenuItemConfig infoItem;
        @Builder.Default
        private final int confirmSlot = 11;
        @Builder.Default
        private final int cancelSlot = 15;
        @Builder.Default
        private final int infoSlot = 13;
    }

    @Getter
    @Builder
    public static class SelectionConfig {
        private final String optionMaterial;
        @Builder.Default
        private final int startSlot = 10;
    }
}
