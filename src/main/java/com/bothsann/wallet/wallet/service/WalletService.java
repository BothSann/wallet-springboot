package com.bothsann.wallet.wallet.service;

import com.bothsann.wallet.shared.currency.CurrencyProperties;
import com.bothsann.wallet.shared.enums.TransactionStatus;
import com.bothsann.wallet.shared.enums.TransactionType;
import com.bothsann.wallet.shared.currency.ExchangeRateService;
import com.bothsann.wallet.shared.event.DepositSuccessEvent;
import com.bothsann.wallet.shared.event.TransferReceivedEvent;
import com.bothsann.wallet.shared.exception.DailyLimitCapExceededException;
import com.bothsann.wallet.shared.exception.DuplicateIdempotencyKeyException;
import com.bothsann.wallet.shared.exception.InsufficientBalanceException;
import com.bothsann.wallet.shared.exception.InvalidPinException;
import com.bothsann.wallet.shared.exception.PinAlreadySetException;
import com.bothsann.wallet.shared.exception.PinNotSetException;
import com.bothsann.wallet.shared.exception.SelfTransferException;
import com.bothsann.wallet.shared.exception.UnsupportedCurrencyException;
import com.bothsann.wallet.shared.exception.UserNotFoundException;
import com.bothsann.wallet.shared.exception.WalletNotFoundException;
import com.bothsann.wallet.transaction.dto.TransactionResponse;
import com.bothsann.wallet.transaction.entity.Transaction;
import com.bothsann.wallet.transaction.repository.TransactionRepository;
import com.bothsann.wallet.user.entity.User;
import com.bothsann.wallet.user.repository.UserRepository;
import com.bothsann.wallet.wallet.dto.ChangePinRequest;
import com.bothsann.wallet.wallet.dto.CreateWalletRequest;
import com.bothsann.wallet.wallet.dto.DailyLimitResponse;
import com.bothsann.wallet.wallet.dto.DepositRequest;
import com.bothsann.wallet.wallet.dto.SetPinRequest;
import com.bothsann.wallet.wallet.dto.TransferRequest;
import com.bothsann.wallet.wallet.dto.UpdateDailyLimitRequest;
import com.bothsann.wallet.wallet.dto.WalletResponse;
import com.bothsann.wallet.wallet.dto.WithdrawRequest;
import com.bothsann.wallet.wallet.entity.Wallet;
import com.bothsann.wallet.wallet.repository.WalletRepository;
import com.bothsann.wallet.shared.config.WalletProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DailyLimitService dailyLimitService;
    private final WalletProperties walletProperties;
    private final CurrencyProperties currencyProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ExchangeRateService exchangeRateService;

    @Transactional(readOnly = true)
    public List<WalletResponse> listWallets(UUID userId) {
        return walletRepository.findAllByUserId(userId)
                .stream()
                .map(WalletResponse::from)
                .toList();
    }

    public WalletResponse createWallet(UUID userId, CreateWalletRequest req) {
        String currency = req.currency().toUpperCase();
        if (!currencyProperties.getWalletCurrencies().contains(currency)) {
            throw new UnsupportedCurrencyException(currency, currencyProperties.getWalletCurrencies());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .isDefault(false)
                .build());
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId, UUID walletId) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(WalletNotFoundException::new);
        return WalletResponse.from(wallet);
    }

    public void setPin(UUID userId, SetPinRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        if (user.getPinHash() != null) {
            throw new PinAlreadySetException();
        }
        user.setPinHash(passwordEncoder.encode(req.pin()));
        userRepository.save(user);
    }

    public void changePin(UUID userId, ChangePinRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        if (user.getPinHash() == null) {
            throw new PinNotSetException();
        }
        if (!passwordEncoder.matches(req.currentPin(), user.getPinHash())) {
            throw new InvalidPinException();
        }
        user.setPinHash(passwordEncoder.encode(req.newPin()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public DailyLimitResponse getDailyLimitStatus(UUID userId, UUID walletId) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(WalletNotFoundException::new);
        BigDecimal todaySpend = dailyLimitService.getTodaySpendInUsd(wallet.getId(), wallet.getCurrency());
        BigDecimal remaining = wallet.getDailyLimit().subtract(todaySpend);
        Instant resetAt = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new DailyLimitResponse(wallet.getDailyLimit(), todaySpend, remaining, resetAt);
    }

    public DailyLimitResponse updateDailyLimit(UUID userId, UUID walletId, UpdateDailyLimitRequest req) {
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(WalletNotFoundException::new);
        if (req.dailyLimit().compareTo(walletProperties.getMaxDailyLimit()) > 0) {
            throw new DailyLimitCapExceededException(walletProperties.getMaxDailyLimit());
        }
        wallet.setDailyLimit(req.dailyLimit());
        walletRepository.save(wallet);
        return getDailyLimitStatus(userId, walletId);
    }

    public TransactionResponse deposit(UUID userId, UUID walletId, DepositRequest req, String idempotencyKey) {
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateIdempotencyKeyException(idempotencyKey);
        }
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(WalletNotFoundException::new);

        BigDecimal balanceBefore = wallet.getBalance();
        Transaction tx = transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .idempotencyKey(idempotencyKey)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .amount(req.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore)
                .description(req.description())
                .build());

        wallet.setBalance(balanceBefore.add(req.amount()));
        walletRepository.save(wallet);

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setBalanceAfter(wallet.getBalance());
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new DepositSuccessEvent(
                wallet.getUser().getEmail(),
                wallet.getUser().getFullName(),
                req.amount(),
                wallet.getBalance()
        ));

        return TransactionResponse.from(tx);
    }

    public TransactionResponse withdraw(UUID userId, UUID walletId, WithdrawRequest req, String idempotencyKey) {
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateIdempotencyKeyException(idempotencyKey);
        }
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(WalletNotFoundException::new);

        verifyPin(wallet.getUser(), req.pin());
        dailyLimitService.checkLimit(wallet, req.amount());

        if (wallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(wallet.getBalance(), req.amount());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        Transaction tx = transactionRepository.save(Transaction.builder()
                .wallet(wallet)
                .idempotencyKey(idempotencyKey)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .amount(req.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore)
                .description(req.description())
                .build());

        wallet.setBalance(balanceBefore.subtract(req.amount()));
        walletRepository.save(wallet);

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setBalanceAfter(wallet.getBalance());
        transactionRepository.save(tx);

        return TransactionResponse.from(tx);
    }

    public TransactionResponse transfer(UUID senderId, TransferRequest req, String idempotencyKey) {
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateIdempotencyKeyException(idempotencyKey);
        }

        Wallet senderWallet = walletRepository.findByIdAndUserId(req.fromWalletId(), senderId)
                .orElseThrow(WalletNotFoundException::new);

        verifyPin(senderWallet.getUser(), req.pin());
        dailyLimitService.checkLimit(senderWallet, req.amount());

        var recipient = userRepository.findByEmail(req.recipientEmail())
                .orElseThrow(() -> new UserNotFoundException(req.recipientEmail()));

        if (senderId.equals(recipient.getId())) {
            throw new SelfTransferException();
        }

        Wallet recipientWallet = walletRepository.findByIdAndUserId(req.recipientWalletId(), recipient.getId())
                .orElseThrow(WalletNotFoundException::new);

        if (senderWallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(senderWallet.getBalance(), req.amount());
        }

        // Resolve currency conversion if wallets use different currencies
        String senderCurrency = senderWallet.getCurrency();
        String recipientCurrency = recipientWallet.getCurrency();
        boolean isCrossCurrency = !senderCurrency.equals(recipientCurrency);

        BigDecimal senderAmount = req.amount();
        BigDecimal recipientAmount = senderAmount;
        BigDecimal appliedRate = null;

        if (isCrossCurrency) {
            appliedRate = exchangeRateService.getRate(senderCurrency, recipientCurrency);
            recipientAmount = exchangeRateService.convert(senderAmount, senderCurrency, recipientCurrency);
        }

        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        BigDecimal recipientBalanceBefore = recipientWallet.getBalance();

        Transaction senderTx = transactionRepository.save(Transaction.builder()
                .wallet(senderWallet)
                .idempotencyKey(idempotencyKey)
                .type(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.PENDING)
                .amount(senderAmount)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceBefore)
                .description(req.description())
                .build());

        Transaction recipientTx = transactionRepository.save(Transaction.builder()
                .wallet(recipientWallet)
                .idempotencyKey(idempotencyKey + "-in")
                .type(TransactionType.TRANSFER_IN)
                .status(TransactionStatus.PENDING)
                .amount(recipientAmount)
                .balanceBefore(recipientBalanceBefore)
                .balanceAfter(recipientBalanceBefore)
                .description(req.description())
                .exchangeRate(appliedRate)
                .build());

        senderWallet.setBalance(senderBalanceBefore.subtract(senderAmount));
        walletRepository.save(senderWallet);

        recipientWallet.setBalance(recipientBalanceBefore.add(recipientAmount));
        walletRepository.save(recipientWallet);

        senderTx.setStatus(TransactionStatus.SUCCESS);
        senderTx.setBalanceAfter(senderWallet.getBalance());
        transactionRepository.save(senderTx);

        recipientTx.setStatus(TransactionStatus.SUCCESS);
        recipientTx.setBalanceAfter(recipientWallet.getBalance());
        transactionRepository.save(recipientTx);

        eventPublisher.publishEvent(new TransferReceivedEvent(
                recipient.getEmail(),
                recipient.getFullName(),
                senderWallet.getUser().getEmail(),
                recipientAmount,
                recipientWallet.getBalance()
        ));

        return TransactionResponse.from(senderTx);
    }

    private void verifyPin(User user, String submittedPin) {
        if (user.getPinHash() == null) {
            throw new PinNotSetException();
        }
        if (submittedPin == null || submittedPin.isBlank() || !passwordEncoder.matches(submittedPin, user.getPinHash())) {
            throw new InvalidPinException();
        }
    }
}
