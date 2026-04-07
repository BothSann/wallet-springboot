package com.bothsann.wallet.wallet.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
        @NotBlank(message = "Currency is required") String currency
) {}
