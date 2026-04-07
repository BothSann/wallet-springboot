package com.bothsann.wallet.transaction.dto;

import com.bothsann.wallet.shared.enums.TransactionStatus;
import com.bothsann.wallet.shared.enums.TransactionType;
import com.bothsann.wallet.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String idempotencyKey,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String description,
        BigDecimal exchangeRate,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getIdempotencyKey(),
                tx.getType(),
                tx.getStatus(),
                tx.getAmount(),
                tx.getWallet().getCurrency(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getDescription(),
                tx.getExchangeRate(),
                tx.getCreatedAt()
        );
    }
}
