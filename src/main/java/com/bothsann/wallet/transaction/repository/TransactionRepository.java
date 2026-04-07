package com.bothsann.wallet.transaction.repository;

import com.bothsann.wallet.shared.enums.TransactionStatus;
import com.bothsann.wallet.shared.enums.TransactionType;
import com.bothsann.wallet.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    Page<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type, Pageable pageable);

    Optional<Transaction> findByIdAndWalletId(UUID id, UUID walletId);

    Page<Transaction> findByWalletIdIn(List<UUID> walletIds, Pageable pageable);

    Page<Transaction> findByWalletIdInAndType(List<UUID> walletIds, TransactionType type, Pageable pageable);

    Optional<Transaction> findByIdAndWalletIdIn(UUID id, List<UUID> walletIds);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.wallet.id = :walletId " +
           "AND t.type IN :types " +
           "AND t.status = :status " +
           "AND t.createdAt >= :since")
    BigDecimal sumAmountByWalletIdAndTypeInAndStatusAndCreatedAtAfter(
            @Param("walletId") UUID walletId,
            @Param("types") List<TransactionType> types,
            @Param("status") TransactionStatus status,
            @Param("since") LocalDateTime since);
}
