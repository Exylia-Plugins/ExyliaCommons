package net.exylia.commons.utils;

import net.exylia.commons.utils.versions.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class AdapterFactory {

    private static ItemMetaAdapter itemMetaAdapter;
    private static InventoryAdapter inventoryAdapter;
    private static MessageAdapter messageAdapter;
    private static JavaPlugin plugin;

    public static void initialize(JavaPlugin mainPlugin) {
        plugin = mainPlugin;

        String version = Bukkit.getBukkitVersion();

        if (version.startsWith("1.17") || version.startsWith("1.16") || version.startsWith("1.15") || version.startsWith("1.14")) {
            itemMetaAdapter = new ItemMeta_1_17_Adapter();
            inventoryAdapter = new Inventory_1_17_Adapter();
            messageAdapter = new Message_1_17_Adapter(plugin);
        } else {
            itemMetaAdapter = new ItemMeta_1_18_Adapter();
            inventoryAdapter = new Inventory_1_18_Adapter();
            messageAdapter = new Message_1_18_Adapter(plugin);
        }
    }

    public static void close() {
        if (messageAdapter instanceof Message_1_17_Adapter) {
            ((Message_1_17_Adapter) messageAdapter).close();
        }
    }

    public static ItemMetaAdapter getItemMetaAdapter() {
        return itemMetaAdapter;
    }

    public static InventoryAdapter getInventoryAdapter() {
        return inventoryAdapter;
    }

    public static MessageAdapter getMessageAdapter() {
        if (messageAdapter == null) {
            logInternalWarn("messageAdapter es nulo! Intentando inicializar...");
            if (plugin != null) {
                initialize(plugin);
            } else {
                logInternalError("No se puede inicializar porque plugin es nulo!");
            }
        }
        return messageAdapter;
    }
}
