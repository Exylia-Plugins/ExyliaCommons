package net.exylia.commons.placeholdersV2.example;

import net.exylia.commons.placeholdersV2.annotation.Placeholder;
import net.exylia.commons.placeholdersV2.annotation.PlaceholderScope;
import net.exylia.commons.placeholdersV2.context.PlaceholderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExamplePlaceholders {

    @Placeholder(
            name = "server_motd",
            description = "MOTD del servidor",
            scope = PlaceholderScope.GLOBAL
    )
    public String getServerMotd() {
        return Bukkit.getMotd();
    }

    @Placeholder(
            name = "online_players",
            description = "Cantidad de jugadores online",
            scope = PlaceholderScope.GLOBAL,
            cacheable = true,
            cacheTtlMs = 5000
    )
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Placeholder(
            name = "max_players",
            description = "Máximo de jugadores",
            scope = PlaceholderScope.GLOBAL
    )
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Placeholder(
            name = "server_time",
            description = "Hora actual del servidor",
            scope = PlaceholderScope.GLOBAL,
            cacheable = true,
            cacheTtlMs = 1000
    )
    public String getServerTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    @Placeholder(
            name = "player_name",
            description = "Nombre del jugador",
            scope = PlaceholderScope.PLAYER,
            cacheable = true,
            cacheTtlMs = 30000
    )
    public String getPlayerName(Player player) {
        return player.getName();
    }

    @Placeholder(
            name = "player_level",
            description = "Nivel del jugador",
            scope = PlaceholderScope.PLAYER
    )
    public int getPlayerLevel(Player player) {
        return player.getLevel();
    }

    @Placeholder(
            name = "player_health",
            description = "Vida del jugador",
            scope = PlaceholderScope.PLAYER,
            cacheable = true,
            cacheTtlMs = 500
    )
    public String getPlayerHealth(Player player) {
        return String.format("%.1f", player.getHealth());
    }

    @Placeholder(
            name = "player_food",
            description = "Hambre del jugador",
            scope = PlaceholderScope.PLAYER,
            cacheable = true,
            cacheTtlMs = 500
    )
    public int getPlayerFood(Player player) {
        return player.getFoodLevel();
    }

    @Placeholder(
            name = "player_exp",
            description = "Experiencia del jugador",
            scope = PlaceholderScope.PLAYER
    )
    public int getPlayerExp(Player player) {
        return (int) (player.getExp() * player.getExpToLevel());
    }

    @Placeholder(
            name = "player_location",
            description = "Ubicación del jugador",
            scope = PlaceholderScope.PLAYER,
            cacheable = false
    )
    public String getPlayerLocation(Player player) {
        var loc = player.getLocation();
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    @Placeholder(
            name = "player_world",
            description = "Mundo del jugador",
            scope = PlaceholderScope.PLAYER
    )
    public String getPlayerWorld(Player player) {
        return player.getWorld().getName();
    }

    @Placeholder(
            name = "game_mode",
            description = "Modo de juego del jugador",
            scope = PlaceholderScope.PLAYER
    )
    public String getGameMode(Player player) {
        return player.getGameMode().toString();
    }

    @Placeholder(
            name = "context_data",
            description = "Dato personalizado del contexto",
            scope = PlaceholderScope.CONTEXT,
            cacheable = false
    )
    public String getContextData(PlaceholderContext context, Player player) {
        String customData = context.get("custom_data", String.class);
        return customData != null ? customData : "N/A";
    }

    @Placeholder(
            name = "context_object",
            description = "Objeto del contexto por tipo",
            scope = PlaceholderScope.CONTEXT
    )
    public String getContextObject(PlaceholderContext context) {
        MyCustomData data = context.find(MyCustomData.class);
        return data != null ? data.getValue() : "No data";
    }

    @Placeholder(
            name = "player_uuid",
            description = "UUID del jugador",
            scope = PlaceholderScope.PLAYER
    )
    public String getPlayerUUID(Player player) {
        return player.getUniqueId().toString();
    }

    public static class MyCustomData {
        private final String value;

        public MyCustomData(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
