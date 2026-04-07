package com.bothsann.wallet.wallet.dto;

import com.bothsann.wallet.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        BigDecimal balance,
        String currency,
        boolean isDefault,
        LocalDateTime updatedAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.isDefault(),
                wallet.getUpdatedAt()
        );
    }
}
