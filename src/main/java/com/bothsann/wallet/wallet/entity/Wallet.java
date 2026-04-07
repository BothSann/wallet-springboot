package com.bothsann.wallet.wallet.entity;

import com.bothsann.wallet.shared.entity.AuditableEntity;
import com.bothsann.wallet.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Builder.Default
    @Column(name = "daily_limit", nullable = false)
    private BigDecimal dailyLimit = new BigDecimal("10000.0000");
}
