package net.exylia.commons.v2.clan.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ClanProviderRegistry {

    private static final ClanProviderRegistry INSTANCE = new ClanProviderRegistry();

    private final List<RegisteredProvider> providers = new ArrayList<>();
    private final Object lock = new Object();

    private ClanProviderRegistry() {}

    public static ClanProviderRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ClanProvider provider, int priority) {
        synchronized (lock) {
            providers.removeIf(rp -> rp.provider().getProviderName().equals(provider.getProviderName()));
            providers.add(new RegisteredProvider(provider, priority));
            providers.sort(Comparator.comparingInt(RegisteredProvider::priority).reversed());
        }
    }

    public void register(ClanProvider provider) {
        register(provider, 100);
    }

    public void unregister(String providerName) {
        synchronized (lock) {
            providers.removeIf(rp -> rp.provider().getProviderName().equals(providerName));
        }
    }

    public List<ClanProvider> getOrderedProviders() {
        synchronized (lock) {
            if (providers.isEmpty()) {
                return Collections.emptyList();
            }
            List<ClanProvider> result = new ArrayList<>(providers.size());
            for (RegisteredProvider rp : providers) {
                result.add(rp.provider());
            }
            return result;
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return providers.isEmpty();
        }
    }

    private record RegisteredProvider(ClanProvider provider, int priority) {}
}
