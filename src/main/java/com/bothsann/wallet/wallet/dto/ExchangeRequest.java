package com.bothsann.wallet.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ExchangeRequest(
        @NotNull UUID fromWalletId,
        @NotNull UUID toWalletId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String description,
        String pin
) {}
