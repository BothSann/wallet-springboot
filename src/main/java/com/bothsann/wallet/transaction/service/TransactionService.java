package com.bothsann.wallet.transaction.service;

import com.bothsann.wallet.shared.dto.PageResponse;
import com.bothsann.wallet.shared.enums.TransactionType;
import com.bothsann.wallet.shared.exception.TransactionNotFoundException;
import com.bothsann.wallet.shared.exception.WalletNotFoundException;
import com.bothsann.wallet.transaction.dto.TransactionResponse;
import com.bothsann.wallet.transaction.repository.TransactionRepository;
import com.bothsann.wallet.wallet.entity.Wallet;
import com.bothsann.wallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public PageResponse<TransactionResponse> getHistory(UUID userId, TransactionType type, Pageable pageable) {
        List<UUID> walletIds = walletRepository.findAllByUserId(userId)
                .stream().map(Wallet::getId).toList();
        if (walletIds.isEmpty()) {
            throw new WalletNotFoundException();
        }

        Page<TransactionResponse> page = type != null
                ? transactionRepository.findByWalletIdInAndType(walletIds, type, pageable)
                        .map(TransactionResponse::from)
                : transactionRepository.findByWalletIdIn(walletIds, pageable)
                        .map(TransactionResponse::from);

        return PageResponse.of(page);
    }

    public TransactionResponse getById(UUID userId, UUID transactionId) {
        List<UUID> walletIds = walletRepository.findAllByUserId(userId)
                .stream().map(Wallet::getId).toList();
        if (walletIds.isEmpty()) {
            throw new WalletNotFoundException();
        }

        return transactionRepository.findByIdAndWalletIdIn(transactionId, walletIds)
                .map(TransactionResponse::from)
                .orElseThrow(TransactionNotFoundException::new);
    }
}
