package net.exylia.commons.v2.discord.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.exylia.commons.v2.discord.model.WebhookEmbed;
import net.exylia.commons.v2.discord.model.WebhookField;
import net.exylia.commons.v2.discord.model.WebhookTemplate;

public final class WebhookSerializer {
    private WebhookSerializer() {}

    public static String serialize(WebhookTemplate template) {
        JsonObject json = new JsonObject();

        if (template.getContent() != null) {
            json.addProperty("content", template.getContent());
        }
        if (template.getUsername() != null) {
            json.addProperty("username", template.getUsername());
        }
        if (template.getAvatarUrl() != null) {
            json.addProperty("avatar_url", template.getAvatarUrl());
        }
        if (template.isTts()) {
            json.addProperty("tts", true);
        }

        if (!template.getEmbeds().isEmpty()) {
            JsonArray embedsArray = new JsonArray();
            for (WebhookEmbed embed : template.getEmbeds()) {
                embedsArray.add(serializeEmbed(embed));
            }
            json.add("embeds", embedsArray);
        }

        return json.toString();
    }

    private static JsonObject serializeEmbed(WebhookEmbed embed) {
        JsonObject json = new JsonObject();

        if (embed.getTitle() != null) {
            json.addProperty("title", embed.getTitle());
        }
        if (embed.getDescription() != null) {
            json.addProperty("description", embed.getDescription());
        }
        if (embed.getUrl() != null) {
            json.addProperty("url", embed.getUrl());
        }
        if (embed.getColor() != null) {
            json.addProperty("color", embed.getColor());
        }
        if (embed.getTimestamp() != null) {
            json.addProperty("timestamp", embed.getTimestamp());
        }

        if (embed.getFooterText() != null) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", embed.getFooterText());
            if (embed.getFooterIconUrl() != null) {
                footer.addProperty("icon_url", embed.getFooterIconUrl());
            }
            json.add("footer", footer);
        }

        if (embed.getImageUrl() != null) {
            JsonObject image = new JsonObject();
            image.addProperty("url", embed.getImageUrl());
            json.add("image", image);
        }

        if (embed.getThumbnailUrl() != null) {
            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", embed.getThumbnailUrl());
            json.add("thumbnail", thumbnail);
        }

        if (embed.getAuthorName() != null) {
            JsonObject author = new JsonObject();
            author.addProperty("name", embed.getAuthorName());
            if (embed.getAuthorUrl() != null) {
                author.addProperty("url", embed.getAuthorUrl());
            }
            if (embed.getAuthorIconUrl() != null) {
                author.addProperty("icon_url", embed.getAuthorIconUrl());
            }
            json.add("author", author);
        }

        if (!embed.getFields().isEmpty()) {
            JsonArray fieldsArray = new JsonArray();
            for (WebhookField field : embed.getFields()) {
                JsonObject fieldJson = new JsonObject();
                fieldJson.addProperty("name", field.getName());
                fieldJson.addProperty("value", field.getValue());
                fieldJson.addProperty("inline", field.isInline());
                fieldsArray.add(fieldJson);
            }
            json.add("fields", fieldsArray);
        }

        return json;
    }
}
