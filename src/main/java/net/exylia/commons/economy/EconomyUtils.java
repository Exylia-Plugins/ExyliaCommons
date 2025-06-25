package net.exylia.commons.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Utilidades convenientes para trabajar con economía
 */
public class EconomyUtils {

    /**
     * Obtiene el proveedor de economía actual
     */
    public static EconomyProvider getEconomy() {
        return EconomyRegister.getCurrentProvider();
    }

    /**
     * Verifica si hay economía disponible
     */
    public static boolean isAvailable() {
        return EconomyRegister.isEconomyAvailable();
    }

    /**
     * Obtiene el balance de un jugador de forma segura
     */
    public static BigDecimal getBalance(Player player) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return getEconomy().getBalance(player);
    }

    /**
     * Obtiene el balance de un jugador de forma segura
     */
    public static BigDecimal getBalance(UUID playerId) {
        if (!isAvailable()) return BigDecimal.ZERO;
        return getEconomy().getBalance(playerId);
    }

    /**
     * Verifica si un jugador tiene suficiente dinero
     */
    public static boolean canAfford(Player player, BigDecimal amount) {
        if (!isAvailable()) return false;
        return getEconomy().hasBalance(player, amount);
    }

    /**
     * Verifica si un jugador tiene suficiente dinero
     */
    public static boolean canAfford(Player player, double amount) {
        return canAfford(player, BigDecimal.valueOf(amount));
    }

    /**
     * Cobra dinero a un jugador si tiene suficiente
     */
    public static boolean charge(Player player, BigDecimal amount) {
        if (!isAvailable()) return false;
        if (!canAfford(player, amount)) return false;

        EconomyResponse response = getEconomy().withdraw(player, amount);
        return response.isSuccess();
    }

    /**
     * Cobra dinero a un jugador si tiene suficiente
     */
    public static boolean charge(Player player, double amount) {
        return charge(player, BigDecimal.valueOf(amount));
    }

    /**
     * Paga dinero a un jugador
     */
    public static boolean pay(Player player, BigDecimal amount) {
        if (!isAvailable()) return false;

        EconomyResponse response = getEconomy().deposit(player, amount);
        return response.isSuccess();
    }

    /**
     * Paga dinero a un jugador
     */
    public static boolean pay(Player player, double amount) {
        return pay(player, BigDecimal.valueOf(amount));
    }

    /**
     * Paga dinero a un jugador offline
     */
    public static boolean pay(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) return false;

        EconomyResponse response = getEconomy().deposit(player, amount);
        return response.isSuccess();
    }

    /**
     * Paga dinero a un jugador offline
     */
    public static boolean pay(OfflinePlayer player, double amount) {
        return pay(player, BigDecimal.valueOf(amount));
    }

    /**
     * Transfiere dinero entre jugadores
     */
    public static TransferResult transfer(Player from, Player to, BigDecimal amount) {
        if (!isAvailable()) {
            return TransferResult.failure("Sistema de economía no disponible");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return TransferResult.failure("Cantidad inválida");
        }

        if (!canAfford(from, amount)) {
            return TransferResult.failure("Fondos insuficientes");
        }

        // Retirar del remitente
        EconomyResponse withdrawResult = getEconomy().withdraw(from, amount);
        if (!withdrawResult.isSuccess()) {
            return TransferResult.failure("Error retirando fondos: " + withdrawResult.getMessage());
        }

        // Depositar al destinatario
        EconomyResponse depositResult = getEconomy().deposit(to, amount);
        if (!depositResult.isSuccess()) {
            // Revertir retiro
            getEconomy().deposit(from, amount);
            return TransferResult.failure("Error depositando fondos: " + depositResult.getMessage());
        }

        return TransferResult.success(amount, withdrawResult.getBalance(), depositResult.getBalance());
    }

    /**
     * Transfiere dinero entre jugadores
     */
    public static TransferResult transfer(Player from, Player to, double amount) {
        return transfer(from, to, BigDecimal.valueOf(amount));
    }

    /**
     * Formatea una cantidad de dinero
     */
    public static String format(BigDecimal amount) {
        if (!isAvailable()) return "$" + amount.toString();
        return getEconomy().format(amount);
    }

    /**
     * Formatea una cantidad de dinero
     */
    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    /**
     * Obtiene el símbolo de la moneda
     */
    public static String getCurrencySymbol() {
        if (!isAvailable()) return "$";
        return getEconomy().getCurrencySymbol();
    }

    /**
     * Obtiene el nombre de la moneda
     */
    public static String getCurrencyName(boolean plural) {
        if (!isAvailable()) return plural ? "coins" : "coin";
        return plural ? getEconomy().getCurrencyPluralName() : getEconomy().getCurrencyName();
    }

    /**
     * Establece el balance de un jugador
     */
    public static boolean setBalance(Player player, BigDecimal amount) {
        if (!isAvailable()) return false;

        EconomyResponse response = getEconomy().setBalance(player, amount);
        return response.isSuccess();
    }

    /**
     * Establece el balance de un jugador
     */
    public static boolean setBalance(Player player, double amount) {
        return setBalance(player, BigDecimal.valueOf(amount));
    }

    /**
     * Asegura que un jugador tenga una cuenta
     */
    public static boolean ensureAccount(Player player) {
        if (!isAvailable()) return false;

        if (!getEconomy().hasAccount(player)) {
            return getEconomy().createAccount(player);
        }

        return true;
    }

    /**
     * Resultado de una transferencia de dinero
     */
    public static class TransferResult {
        private final boolean success;
        private final String message;
        private final BigDecimal amount;
        private final BigDecimal fromBalance;
        private final BigDecimal toBalance;

        private TransferResult(boolean success, String message, BigDecimal amount, BigDecimal fromBalance, BigDecimal toBalance) {
            this.success = success;
            this.message = message;
            this.amount = amount;
            this.fromBalance = fromBalance;
            this.toBalance = toBalance;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getFromBalance() { return fromBalance; }
        public BigDecimal getToBalance() { return toBalance; }

        public static TransferResult success(BigDecimal amount, BigDecimal fromBalance, BigDecimal toBalance) {
            return new TransferResult(true, "Transferencia exitosa", amount, fromBalance, toBalance);
        }

        public static TransferResult failure(String message) {
            return new TransferResult(false, message, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}