package com.bothsann.wallet.shared.currency;

import com.bothsann.wallet.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_exchange_rate_pair",
                columnNames = {"from_currency", "to_currency"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRate extends AuditableEntity {

    @Column(name = "from_currency", nullable = false, length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 10)
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
    }
}
