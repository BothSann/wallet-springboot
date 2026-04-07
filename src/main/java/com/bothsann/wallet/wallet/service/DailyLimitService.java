package com.bothsann.wallet.wallet.service;

import com.bothsann.wallet.shared.currency.ExchangeRateService;
import com.bothsann.wallet.shared.enums.TransactionStatus;
import com.bothsann.wallet.shared.enums.TransactionType;
import com.bothsann.wallet.shared.exception.DailyLimitExceededException;
import com.bothsann.wallet.transaction.repository.TransactionRepository;
import com.bothsann.wallet.wallet.entity.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyLimitService {

    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;

    private BigDecimal getTodaySpend(UUID walletId) {
        LocalDateTime todayMidnightUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        return transactionRepository.sumAmountByWalletIdAndTypeInAndStatusAndCreatedAtAfter(
                walletId,
                List.of(TransactionType.WITHDRAWAL, TransactionType.TRANSFER_OUT),
                TransactionStatus.SUCCESS,
                todayMidnightUtc
        );
    }

    /** Today's spend converted to USD — used for limit display and enforcement. */
    public BigDecimal getTodaySpendInUsd(UUID walletId, String walletCurrency) {
        BigDecimal rawSpend = getTodaySpend(walletId);
        if ("USD".equals(walletCurrency)) return rawSpend;
        return exchangeRateService.convert(rawSpend, walletCurrency, "USD");
    }

    public void checkLimit(Wallet wallet, BigDecimal requestedAmount) {
        String currency = wallet.getCurrency();
        BigDecimal requestedUsd = "USD".equals(currency)
                ? requestedAmount
                : exchangeRateService.convert(requestedAmount, currency, "USD");
        BigDecimal todaySpendUsd = getTodaySpendInUsd(wallet.getId(), currency);
        if (todaySpendUsd.add(requestedUsd).compareTo(wallet.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException(wallet.getDailyLimit(), todaySpendUsd, requestedUsd);
        }
    }
}
