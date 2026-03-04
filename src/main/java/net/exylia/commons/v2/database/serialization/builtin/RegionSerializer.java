package net.exylia.commons.v2.database.serialization.builtin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.database.serialization.Deserializer;
import net.exylia.commons.v2.database.serialization.Serializer;
import net.exylia.commons.v2.region.model.RegionFlag;
import net.exylia.commons.v2.region.model.RegionFlagState;
import net.exylia.commons.v2.region.model.RegionPriority;
import net.exylia.commons.v2.region.model.Region;
import org.bukkit.Material;

import java.util.*;

public class RegionSerializer implements Serializer<Region> {

    public static final RegionSerializer INSTANCE = new RegionSerializer();
    private static final Gson GSON = new Gson();
    private static final SelectionSerializer SELECTION_SERIALIZER = SelectionSerializer.INSTANCE;

    @Override
    public String serialize(Region value) {
        if (value == null || !value.isValid()) {
            return null;
        }

        JsonObject json = new JsonObject();

        json.addProperty("id", value.getId());
        json.addProperty("displayName", value.getDisplayName());
        json.addProperty("description", value.getDescription());
        json.addProperty("priority", value.getPriority().name());
        json.addProperty("createdAt", value.getCreatedAt());
        json.addProperty("temporaryBlocksSeconds", value.getTemporaryBlocksSeconds());

        String selectionJson = SELECTION_SERIALIZER.serialize(value.getSelection());
        if (selectionJson != null) {
            json.add("selection", GSON.fromJson(selectionJson, JsonObject.class));
        }

        JsonObject flagsJson = new JsonObject();
        for (Map.Entry<RegionFlag, RegionFlagState> entry : value.getConfiguredFlags().entrySet()) {
            flagsJson.addProperty(entry.getKey().name(), entry.getValue().name());
        }
        json.add("flags", flagsJson);

        JsonArray ownersArray = new JsonArray();
        for (UUID owner : value.getOwners()) {
            ownersArray.add(owner.toString());
        }
        json.add("owners", ownersArray);

        JsonArray membersArray = new JsonArray();
        for (UUID member : value.getMembers()) {
            membersArray.add(member.toString());
        }
        json.add("members", membersArray);

        JsonArray allowedBlocksArray = new JsonArray();
        for (Material material : value.getAllowedBlocks()) {
            allowedBlocksArray.add(material.name());
        }
        json.add("allowedBlocks", allowedBlocksArray);

        JsonObject metadataJson = new JsonObject();
        for (Map.Entry<String, Object> entry : value.getMetadataCopy().entrySet()) {
            Object metaValue = entry.getValue();
            if (metaValue instanceof String) {
                metadataJson.addProperty(entry.getKey(), (String) metaValue);
            } else if (metaValue instanceof Number) {
                metadataJson.addProperty(entry.getKey(), (Number) metaValue);
            } else if (metaValue instanceof Boolean) {
                metadataJson.addProperty(entry.getKey(), (Boolean) metaValue);
            } else {
                metadataJson.addProperty(entry.getKey(), metaValue.toString());
            }
        }
        json.add("metadata", metadataJson);

        return GSON.toJson(json);
    }
}

class RegionDeserializer implements Deserializer<Region> {

    private static final Gson GSON = new Gson();
    private static final SelectionDeserializer SELECTION_DESERIALIZER = new SelectionDeserializer();

    @Override
    public Region deserialize(String value, Class<Region> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            JsonObject json = GSON.fromJson(value, JsonObject.class);

            String id = json.get("id").getAsString();

            JsonObject selectionJson = json.getAsJsonObject("selection");
            String selectionString = GSON.toJson(selectionJson);
            Selection selection = SELECTION_DESERIALIZER.deserialize(selectionString, Selection.class);

            if (selection == null || !selection.isComplete()) {
                return null;
            }

            Region region = new Region(id, selection);

            if (json.has("displayName")) {
                region.setDisplayName(json.get("displayName").getAsString());
            }

            if (json.has("description")) {
                region.setDescription(json.get("description").getAsString());
            }

            if (json.has("priority")) {
                try {
                    region.setPriority(RegionPriority.valueOf(json.get("priority").getAsString()));
                } catch (IllegalArgumentException e) {
                    region.setPriority(RegionPriority.NORMAL);
                }
            }

            if (json.has("temporaryBlocksSeconds")) {
                region.setTemporaryBlocksSeconds(json.get("temporaryBlocksSeconds").getAsInt());
            }

            if (json.has("flags")) {
                JsonObject flagsJson = json.getAsJsonObject("flags");
                for (Map.Entry<String, JsonElement> entry : flagsJson.entrySet()) {
                    try {
                        RegionFlag flag = RegionFlag.valueOf(entry.getKey());
                        RegionFlagState state = RegionFlagState.valueOf(entry.getValue().getAsString());
                        region.setFlag(flag, state);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (json.has("owners")) {
                JsonArray ownersArray = json.getAsJsonArray("owners");
                for (JsonElement element : ownersArray) {
                    try {
                        region.addOwner(UUID.fromString(element.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (json.has("members")) {
                JsonArray membersArray = json.getAsJsonArray("members");
                for (JsonElement element : membersArray) {
                    try {
                        UUID memberId = UUID.fromString(element.getAsString());
                        if (!region.isOwner(memberId)) {
                            region.addMember(memberId);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (json.has("allowedBlocks")) {
                JsonArray allowedBlocksArray = json.getAsJsonArray("allowedBlocks");
                Set<Material> allowedBlocks = new HashSet<>();
                for (JsonElement element : allowedBlocksArray) {
                    try {
                        Material material = Material.valueOf(element.getAsString());
                        allowedBlocks.add(material);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                region.setAllowedBlocks(allowedBlocks);
            }

            if (json.has("metadata")) {
                JsonObject metadataJson = json.getAsJsonObject("metadata");
                for (Map.Entry<String, JsonElement> entry : metadataJson.entrySet()) {
                    JsonElement element = entry.getValue();
                    if (element.isJsonPrimitive()) {
                        if (element.getAsJsonPrimitive().isBoolean()) {
                            region.setMetadata(entry.getKey(), element.getAsBoolean());
                        } else if (element.getAsJsonPrimitive().isNumber()) {
                            region.setMetadata(entry.getKey(), element.getAsNumber());
                        } else {
                            region.setMetadata(entry.getKey(), element.getAsString());
                        }
                    }
                }
            }

            return region;

        } catch (Exception e) {
            return null;
        }
    }
}
