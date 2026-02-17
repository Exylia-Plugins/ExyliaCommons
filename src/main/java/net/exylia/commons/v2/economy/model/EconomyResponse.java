package net.exylia.commons.v2.economy.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EconomyResponse {
    private final boolean success;
    private final String message;
    @Builder.Default
    private final BigDecimal amount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal balance = BigDecimal.ZERO;
    @Builder.Default
    private final ResponseType type = ResponseType.SUCCESS;

    public enum ResponseType {
        SUCCESS,
        FAILURE,
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        INVALID_AMOUNT,
        NOT_AVAILABLE
    }

    public static EconomyResponse success(BigDecimal amount, BigDecimal balance) {
        return EconomyResponse.builder()
                .success(true)
                .amount(amount)
                .balance(balance)
                .type(ResponseType.SUCCESS)
                .build();
    }

    public static EconomyResponse failure(String message) {
        return EconomyResponse.builder()
                .success(false)
                .message(message)
                .type(ResponseType.FAILURE)
                .build();
    }

    public static EconomyResponse insufficientFunds(BigDecimal amount, BigDecimal balance) {
        return EconomyResponse.builder()
                .success(false)
                .message("Insufficient funds")
                .amount(amount)
                .balance(balance)
                .type(ResponseType.INSUFFICIENT_FUNDS)
                .build();
    }

    public static EconomyResponse accountNotFound() {
        return EconomyResponse.builder()
                .success(false)
                .message("Account not found")
                .type(ResponseType.ACCOUNT_NOT_FOUND)
                .build();
    }

    public static EconomyResponse invalidAmount() {
        return EconomyResponse.builder()
                .success(false)
                .message("Invalid amount")
                .type(ResponseType.INVALID_AMOUNT)
                .build();
    }

    public static EconomyResponse notAvailable() {
        return EconomyResponse.builder()
                .success(false)
                .message("Economy provider not available")
                .type(ResponseType.NOT_AVAILABLE)
                .build();
    }
}
