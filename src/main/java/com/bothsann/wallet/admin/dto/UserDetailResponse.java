package com.bothsann.wallet.admin.dto;

import com.bothsann.wallet.shared.enums.Role;
import com.bothsann.wallet.user.entity.User;
import com.bothsann.wallet.wallet.dto.WalletResponse;
import com.bothsann.wallet.wallet.entity.Wallet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserDetailResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean isActive,
        LocalDateTime createdAt,
        List<WalletResponse> wallets
) {
    public static UserDetailResponse from(User user, List<Wallet> wallets) {
        return new UserDetailResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                wallets.stream().map(WalletResponse::from).toList()
        );
    }
}
