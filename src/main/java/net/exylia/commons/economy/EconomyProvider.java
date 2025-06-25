package net.exylia.commons.economy;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interfaz unificada para sistemas de economía
 */
public interface EconomyProvider {

    /**
     * Obtiene el nombre del proveedor de economía
     */
    String getProviderName();

    /**
     * Verifica si el proveedor está disponible y funcionando
     */
    boolean isAvailable();

    /**
     * Obtiene el balance de un jugador
     */
    BigDecimal getBalance(UUID playerId);

    /**
     * Obtiene el balance de un jugador
     */
    BigDecimal getBalance(OfflinePlayer player);

    /**
     * Verifica si un jugador tiene suficiente dinero
     */
    boolean hasBalance(UUID playerId, BigDecimal amount);

    /**
     * Verifica si un jugador tiene suficiente dinero
     */
    boolean hasBalance(OfflinePlayer player, BigDecimal amount);

    /**
     * Deposita dinero a un jugador
     */
    EconomyResponse deposit(UUID playerId, BigDecimal amount);

    /**
     * Deposita dinero a un jugador
     */
    EconomyResponse deposit(OfflinePlayer player, BigDecimal amount);

    /**
     * Retira dinero de un jugador
     */
    EconomyResponse withdraw(UUID playerId, BigDecimal amount);

    /**
     * Retira dinero de un jugador
     */
    EconomyResponse withdraw(OfflinePlayer player, BigDecimal amount);

    /**
     * Establece el balance de un jugador
     */
    EconomyResponse setBalance(UUID playerId, BigDecimal amount);

    /**
     * Establece el balance de un jugador
     */
    EconomyResponse setBalance(OfflinePlayer player, BigDecimal amount);

    /**
     * Obtiene el símbolo de la moneda
     */
    String getCurrencySymbol();

    /**
     * Obtiene el nombre de la moneda (singular)
     */
    String getCurrencyName();

    /**
     * Obtiene el nombre de la moneda (plural)
     */
    String getCurrencyPluralName();

    /**
     * Formatea una cantidad para mostrar
     */
    String format(BigDecimal amount);

    /**
     * Verifica si el jugador tiene una cuenta
     */
    boolean hasAccount(UUID playerId);

    /**
     * Verifica si el jugador tiene una cuenta
     */
    boolean hasAccount(OfflinePlayer player);

    /**
     * Crea una cuenta para el jugador
     */
    boolean createAccount(UUID playerId);

    /**
     * Crea una cuenta para el jugador
     */
    boolean createAccount(OfflinePlayer player);
}