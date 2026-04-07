package com.bothsann.wallet.admin.service;

import com.bothsann.wallet.admin.dto.UserDetailResponse;
import com.bothsann.wallet.admin.dto.UserSummaryResponse;
import com.bothsann.wallet.shared.dto.PageResponse;
import com.bothsann.wallet.shared.exception.SelfDeactivationException;
import com.bothsann.wallet.shared.exception.UserNotFoundException;
import com.bothsann.wallet.user.entity.User;
import com.bothsann.wallet.user.repository.UserRepository;
import com.bothsann.wallet.wallet.dto.WalletResponse;
import com.bothsann.wallet.wallet.entity.Wallet;
import com.bothsann.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public PageResponse<UserSummaryResponse> getAllUsers(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(UserSummaryResponse::from));
    }

    public UserDetailResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        List<Wallet> wallets = walletRepository.findAllByUserId(id);
        return UserDetailResponse.from(user, wallets);
    }

    @Transactional
    public UserSummaryResponse deactivateUser(UUID id, UUID currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new SelfDeactivationException();
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
        userRepository.save(user);
        return UserSummaryResponse.from(user);
    }

    public PageResponse<WalletResponse> getAllWallets(Pageable pageable) {
        return PageResponse.of(walletRepository.findAll(pageable).map(WalletResponse::from));
    }

}
