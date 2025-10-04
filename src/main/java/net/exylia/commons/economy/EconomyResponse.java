package net.exylia.commons.economy;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class EconomyResponse {
    /**
     * -- GETTER --
     *  Si la operación fue exitosa
     */
    private final boolean success;
    /**
     * -- GETTER --
     *  Mensaje descriptivo del resultado
     */
    private final String message;
    /**
     * -- GETTER --
     *  Cantidad involucrada en la transacción
     */
    private final BigDecimal amount;
    /**
     * -- GETTER --
     *  Balance resultante después de la operación
     */
    private final BigDecimal balance;
    /**
     * -- GETTER --
     *  Tipo de respuesta
     */
    private final ResponseType type;

    public EconomyResponse(boolean success, String message, BigDecimal amount, BigDecimal balance, ResponseType type) {
        this.success = success;
        this.message = message;
        this.amount = amount;
        this.balance = balance;
        this.type = type;
    }

    public enum ResponseType {
        SUCCESS,
        FAILURE,
        NOT_IMPLEMENTED,
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        INVALID_AMOUNT
    }

    // Métodos de conveniencia para crear respuestas
    public static EconomyResponse success(BigDecimal amount, BigDecimal balance, String message) {
        return new EconomyResponse(true, message, amount, balance, ResponseType.SUCCESS);
    }

    public static EconomyResponse failure(String message) {
        return new EconomyResponse(false, message, BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.FAILURE);
    }

    public static EconomyResponse insufficientFunds(BigDecimal amount, BigDecimal balance) {
        return new EconomyResponse(false, "Fondos insuficientes", amount, balance, ResponseType.INSUFFICIENT_FUNDS);
    }

    public static EconomyResponse accountNotFound() {
        return new EconomyResponse(false, "Cuenta no encontrada", BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.ACCOUNT_NOT_FOUND);
    }

    public static EconomyResponse invalidAmount() {
        return new EconomyResponse(false, "Cantidad inválida", BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.INVALID_AMOUNT);
    }

    public static EconomyResponse notImplemented() {
        return new EconomyResponse(false, "Función no implementada", BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.NOT_IMPLEMENTED);
    }
}