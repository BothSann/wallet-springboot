package com.bothsann.wallet.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Source wallet ID is required") UUID fromWalletId,
        @NotBlank(message = "Recipient email is required") String recipientEmail,
        @NotNull(message = "Recipient wallet ID is required") UUID recipientWalletId,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,
        String description,
        String pin
) {}
