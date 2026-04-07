package com.bothsann.wallet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email,
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format") String phone,
        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String currency
) {}
