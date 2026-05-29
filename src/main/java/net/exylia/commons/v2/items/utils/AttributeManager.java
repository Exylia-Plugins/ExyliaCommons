package net.exylia.commons.v2.items.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class AttributeManager {

    private static Class<?> attributeClass;
    private static Class<?> attributeModifierClass;
    private static Method addAttributeModifierMethod;
    private static Object addNumberOperation;

    static {
        try {
            attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            attributeModifierClass = Class.forName("org.bukkit.attribute.AttributeModifier");

            Class<?> operationClass = Class.forName("org.bukkit.attribute.AttributeModifier$Operation");
            addNumberOperation = operationClass.getField("ADD_NUMBER").get(null);

            addAttributeModifierMethod = ItemMeta.class.getMethod("addAttributeModifier", attributeClass, attributeModifierClass);
        } catch (Exception ignored) {
        }
    }

    public static void applyAttributes(ItemStack itemStack, List<String> rawAttributes) {
        if (attributeClass == null || addAttributeModifierMethod == null) {
            return;
        }

        if (itemStack == null || rawAttributes == null || rawAttributes.isEmpty()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        for (String attributeString : rawAttributes) {
            applyAttribute(meta, attributeString);
        }

        itemStack.setItemMeta(meta);
    }

    private static void applyAttribute(ItemMeta meta, String attributeString) {
        String[] parts = attributeString.split("\\|");
        if (parts.length < 2) {
            return;
        }

        String attributeName = parts[0].trim();
        String valueStr = parts[1].trim();

        try {
            Object attribute = getAttributeByName(attributeName);
            if (attribute == null) {
                return;
            }

            double value = parseValue(valueStr);
            String modifierName = "item_" + UUID.randomUUID();

            Object modifier = attributeModifierClass.getDeclaredConstructor(
                    UUID.class,
                    String.class,
                    double.class,
                    addNumberOperation.getClass()
            ).newInstance(UUID.randomUUID(), modifierName, value, addNumberOperation);

            addAttributeModifierMethod.invoke(meta, attribute, modifier);
        } catch (Exception ignored) {
        }
    }

    private static Object getAttributeByName(String name) {
        try {
            Method valueOfMethod = attributeClass.getMethod("valueOf", String.class);
            return valueOfMethod.invoke(null, name.toUpperCase());
        } catch (Exception e) {
            return getAttributeByAlias(name);
        }
    }

    private static Object getAttributeByAlias(String name) {
        String[] aliasNames = switch (name.toLowerCase()) {
            case "max_health", "maxhealth", "health" -> new String[]{"GENERIC_MAX_HEALTH"};
            case "follow_range", "followrange" -> new String[]{"GENERIC_FOLLOW_RANGE"};
            case "knockback_resistance", "knockbackresistance" -> new String[]{"GENERIC_KNOCKBACK_RESISTANCE"};
            case "movement_speed", "movementspeed" -> new String[]{"GENERIC_MOVEMENT_SPEED"};
            case "attack_damage", "attackdamage", "damage" -> new String[]{"GENERIC_ATTACK_DAMAGE"};
            case "attack_speed", "attackspeed" -> new String[]{"GENERIC_ATTACK_SPEED"};
            case "armor" -> new String[]{"GENERIC_ARMOR"};
            case "armor_toughness", "armortoughness" -> new String[]{"GENERIC_ARMOR_TOUGHNESS"};
            case "flying_speed", "flyingspeed" -> new String[]{"GENERIC_FLYING_SPEED"};
            case "step_height", "stepheight" -> new String[]{"GENERIC_STEP_HEIGHT"};
            default -> new String[]{};
        };

        for (String aliasName : aliasNames) {
            try {
                Method valueOfMethod = attributeClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, aliasName);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public static void applyBlockInteractionRange(ItemStack itemStack, double value) {
        if (attributeClass == null || addAttributeModifierMethod == null) return;
        if (itemStack == null) return;

        Object attribute = null;
        for (String name : new String[]{"PLAYER_BLOCK_INTERACTION_RANGE", "BLOCK_INTERACTION_RANGE"}) {
            try {
                attribute = attributeClass.getMethod("valueOf", String.class).invoke(null, name);
                break;
            } catch (Exception ignored) {}
        }
        if (attribute == null) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        try {
            Object modifier = attributeModifierClass.getDeclaredConstructor(
                UUID.class, String.class, double.class, addNumberOperation.getClass()
            ).newInstance(UUID.randomUUID(), "force_consumable_range", value, addNumberOperation);
            addAttributeModifierMethod.invoke(meta, attribute, modifier);
            itemStack.setItemMeta(meta);
        } catch (Exception ignored) {}
    }

    private static double parseValue(String valueStr) {
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
