package net.exylia.commons.v2.region.api;

import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.v2.region.model.*;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

public class RegionBuilder {
    private final String id;
    private Selection selection;
    private String displayName;
    private String description;
    private RegionPriority priority = RegionPriority.NORMAL;
    private final Map<String, Object> metadata = new HashMap<>();
    private final Map<RegionFlag, RegionFlagState> flags = new EnumMap<>(RegionFlag.class);
    private final Set<UUID> owners = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();
    private final Set<Material> allowedBlocks = new HashSet<>();
    private final Set<Material> breakableBlocks = new HashSet<>();
    private RegionCallback onEnter;
    private RegionCallback onExit;
    private int temporaryBlocksSeconds = 30;

    public RegionBuilder(String id) {
        this.id = id;
    }

    public RegionBuilder selection(Location pos1, Location pos2) {
        this.selection = new Selection(pos1, pos2);
        return this;
    }

    public RegionBuilder selection(Selection selection) {
        this.selection = selection;
        return this;
    }

    public RegionBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public RegionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public RegionBuilder priority(RegionPriority priority) {
        this.priority = priority;
        return this;
    }

    public RegionBuilder flag(RegionFlag flag, boolean value) {
        this.flags.put(flag, RegionFlagState.fromBoolean(value));
        return this;
    }

    public RegionBuilder flag(RegionFlag flag, RegionFlagState state) {
        this.flags.put(flag, state);
        return this;
    }

    public RegionBuilder owners(UUID... ownerIds) {
        this.owners.addAll(Arrays.asList(ownerIds));
        return this;
    }

    public RegionBuilder members(UUID... memberIds) {
        this.members.addAll(Arrays.asList(memberIds));
        return this;
    }

    public RegionBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public RegionBuilder allowedBlocks(Material... materials) {
        this.allowedBlocks.addAll(Arrays.asList(materials));
        return this;
    }

    public RegionBuilder allowedBlocks(Set<Material> materials) {
        this.allowedBlocks.addAll(materials);
        return this;
    }

    public RegionBuilder breakableBlocks(Material... materials) {
        this.breakableBlocks.addAll(Arrays.asList(materials));
        return this;
    }

    public RegionBuilder breakableBlocks(Set<Material> materials) {
        this.breakableBlocks.addAll(materials);
        return this;
    }

    public RegionBuilder onEnter(RegionCallback callback) {
        this.onEnter = callback;
        return this;
    }

    public RegionBuilder onExit(RegionCallback callback) {
        this.onExit = callback;
        return this;
    }

    public RegionBuilder temporaryBlocksSeconds(int seconds) {
        this.temporaryBlocksSeconds = seconds;
        return this;
    }

    public RegionBuilder safeZone() {
        flag(RegionFlag.PVP, false);
        flag(RegionFlag.BUILD, false);
        flag(RegionFlag.BREAK, false);
        flag(RegionFlag.INTERACT, false);
        flag(RegionFlag.ITEM_DROP, false);
        flag(RegionFlag.ITEM_PICKUP, false);
        return this;
    }

    public RegionBuilder membersOnly() {
        flag(RegionFlag.REGION_MEMBERS_ONLY, true);
        return this;
    }

    public RegionBuilder playerBuildOnly() {
        flag(RegionFlag.PLAYER_BUILD_ONLY, true);
        return this;
    }

    public RegionBuilder temporaryBlocks(int seconds, boolean reGive) {
        flag(RegionFlag.TEMPORARY_BLOCKS, true);
        if (reGive) {
            flag(RegionFlag.RE_GIVE_BLOCKS, true);
        }
        temporaryBlocksSeconds(seconds);
        return this;
    }

    public Region build() {
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Region ID cannot be null or empty");
        }

        if (selection == null || !selection.isComplete()) {
            throw new IllegalStateException("Region selection must be complete");
        }

        Region region = new Region(id, selection);

        if (displayName != null) {
            region.setDisplayName(displayName);
        }

        if (description != null) {
            region.setDescription(description);
        }

        region.setPriority(priority);

        for (Map.Entry<RegionFlag, RegionFlagState> entry : flags.entrySet()) {
            region.setFlag(entry.getKey(), entry.getValue());
        }

        for (UUID owner : owners) {
            region.addOwner(owner);
        }

        for (UUID member : members) {
            region.addMember(member);
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            region.setMetadata(entry.getKey(), entry.getValue());
        }

        if (!allowedBlocks.isEmpty()) {
            region.setAllowedBlocks(allowedBlocks);
        }

        if (!breakableBlocks.isEmpty()) {
            region.setBreakableBlocks(breakableBlocks);
        }

        if (onEnter != null) {
            region.setOnEnter(onEnter);
        }

        if (onExit != null) {
            region.setOnExit(onExit);
        }

        region.setTemporaryBlocksSeconds(temporaryBlocksSeconds);

        return region;
    }
}
