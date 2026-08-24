package net.exylia.commons.v2.clan.integration;

import net.exylia.commons.v2.clan.provider.ClanProvider;
import net.exylia.commons.v2.clan.provider.ClanProviderBridge;

import java.lang.reflect.Method;

/**
 * Entry point for external plugin integration with ExyliaCommons' ClanAPI.
 *
 * <p>Uses reflection internally, so ExyliaCommons is NOT required as a compile-time
 * dependency. Just add this artifact as {@code compileOnly} and call these methods
 * at runtime if ExyliaCommons is present on the server.
 *
 * <h3>Minimal integration (Bridge — no Bukkit/Exylia imports needed):</h3>
 * <pre>{@code
 * // plugin.yml: softdepend: [ExyliaCommons]
 *
 * // onEnable:
 * if (ClanIntegration.isAvailable()) {
 *     ClanIntegration.register(new MyBridge());
 * }
 *
 * // onDisable:
 * ClanIntegration.unregister("MyPlugin");
 * }</pre>
 *
 * <h3>Full API integration (AbstractClanProvider — needs compileOnly dep on this jar):</h3>
 * <pre>{@code
 * if (ClanIntegration.isAvailable()) {
 *     ClanIntegration.registerProvider(new MyProvider());
 * }
 * }</pre>
 */
public final class ClanIntegration {

    private static final String CLAN_API = "net.exylia.commons.v2.clan.api.ClanAPI";

    private ClanIntegration() {}

    public static boolean isAvailable() {
        return OptionalClassCache.resolve(CLAN_API) != null;
    }

    public static boolean register(ClanProviderBridge bridge) {
        return invokeStatic("registerBridge", new Class[]{ClanProviderBridge.class}, bridge);
    }

    public static boolean register(ClanProviderBridge bridge, int priority) {
        return invokeStatic("registerBridge", new Class[]{ClanProviderBridge.class, int.class}, bridge, priority);
    }

    public static boolean registerProvider(ClanProvider provider) {
        return invokeStatic("registerProvider", new Class[]{ClanProvider.class}, provider);
    }

    public static boolean registerProvider(ClanProvider provider, int priority) {
        return invokeStatic("registerProvider", new Class[]{ClanProvider.class, int.class}, provider, priority);
    }

    public static boolean unregister(String providerName) {
        return invokeStatic("unregisterProvider", new Class[]{String.class}, providerName);
    }

    private static boolean invokeStatic(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> clanApi = OptionalClassCache.resolve(CLAN_API);
            if (clanApi == null) {
                return false;
            }
            Method method = clanApi.getMethod(methodName, paramTypes);
            method.invoke(null, args);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
