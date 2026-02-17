package net.exylia.commons.v2.economy.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TransferResult {
    private final boolean success;
    private final String message;
    @Builder.Default
    private final BigDecimal amount = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal fromBalance = BigDecimal.ZERO;
    @Builder.Default
    private final BigDecimal toBalance = BigDecimal.ZERO;

    public static TransferResult success(BigDecimal amount, BigDecimal fromBalance, BigDecimal toBalance) {
        return TransferResult.builder()
                .success(true)
                .message("Transfer successful")
                .amount(amount)
                .fromBalance(fromBalance)
                .toBalance(toBalance)
                .build();
    }

    public static TransferResult failure(String message) {
        return TransferResult.builder()
                .success(false)
                .message(message)
                .build();
    }
}
