package net.exylia.commons.v2.clientapi.cooldown.adapter.impl;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.icon.ItemStackIcon;
import com.lunarclient.apollo.common.icon.SimpleResourceLocationIcon;
import com.lunarclient.apollo.mods.impl.ModCooldowns;
import com.lunarclient.apollo.module.cooldown.Cooldown;
import com.lunarclient.apollo.module.cooldown.CooldownModule;
import com.lunarclient.apollo.module.modsetting.ModSettingModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import net.exylia.commons.v2.clientapi.cooldown.adapter.CooldownAdapter;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownDefinition;
import net.exylia.commons.v2.clientapi.cooldown.model.CooldownIcon;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ApolloCooldownAdapter implements CooldownAdapter {

    private final CooldownModule cooldownModule;
    private final ModSettingModule modSettingModule;

    public ApolloCooldownAdapter() {
        this.cooldownModule = Apollo.getModuleManager().getModule(CooldownModule.class);
        this.modSettingModule = Apollo.getModuleManager().getModule(ModSettingModule.class);
    }

    @Override
    public boolean isAvailable() {
        return cooldownModule != null;
    }

    @Override
    public boolean supportsPlayer(Player player) {
        return Apollo.getPlayerManager().getPlayer(player.getUniqueId()).isPresent();
    }

    @Override
    public void display(Player player, CooldownDefinition definition) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (apolloPlayerOpt.isEmpty()) return;

        ApolloPlayer apolloPlayer = apolloPlayerOpt.get();

        if (modSettingModule != null) {
            modSettingModule.getOptions().set(apolloPlayer, ModCooldowns.ENABLED, true);
        }

        Cooldown.CooldownBuilder builder = Cooldown.builder()
                .name(definition.getName())
                .duration(definition.getDuration());

        CooldownIcon icon = definition.getIcon();
        if (icon instanceof CooldownIcon.ItemCooldownIcon itemIcon) {
            builder.icon(ItemStackIcon.builder()
                    .itemName(itemIcon.getItemName())
                    .build());
        } else if (icon instanceof CooldownIcon.ResourceCooldownIcon resourceIcon) {
            builder.icon(SimpleResourceLocationIcon.builder()
                    .resourceLocation(resourceIcon.getResourceLocation())
                    .size(resourceIcon.getSize())
                    .build());
        }

        cooldownModule.displayCooldown(apolloPlayer, builder.build());
    }

    @Override
    public void remove(Player player, String name) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(apolloPlayer -> cooldownModule.removeCooldown(apolloPlayer, name));
    }

    @Override
    public void removeAll(Player player) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        apolloPlayerOpt.ifPresent(cooldownModule::resetCooldowns);
    }
}
