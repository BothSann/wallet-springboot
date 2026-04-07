package com.bothsann.wallet.wallet.repository;

import com.bothsann.wallet.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findAllByUserId(UUID userId);

    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);
}
