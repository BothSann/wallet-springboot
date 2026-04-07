package com.bothsann.wallet.wallet.controller;

import com.bothsann.wallet.transaction.dto.TransactionResponse;
import com.bothsann.wallet.user.entity.User;
import com.bothsann.wallet.wallet.dto.ChangePinRequest;
import com.bothsann.wallet.wallet.dto.CreateWalletRequest;
import com.bothsann.wallet.wallet.dto.DailyLimitResponse;
import com.bothsann.wallet.wallet.dto.DepositRequest;
import com.bothsann.wallet.wallet.dto.SetPinRequest;
import com.bothsann.wallet.wallet.dto.TransferRequest;
import com.bothsann.wallet.wallet.dto.UpdateDailyLimitRequest;
import com.bothsann.wallet.wallet.dto.WalletResponse;
import com.bothsann.wallet.wallet.dto.WithdrawRequest;
import com.bothsann.wallet.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<List<WalletResponse>> listWallets(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(walletService.listWallets(currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateWalletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(currentUser.getId(), req));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getWallet(currentUser.getId(), walletId));
    }

    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID walletId,
            @Valid @RequestBody DepositRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(walletService.deposit(currentUser.getId(), walletId, req, idempotencyKey));
    }

    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID walletId,
            @Valid @RequestBody WithdrawRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(walletService.withdraw(currentUser.getId(), walletId, req, idempotencyKey));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TransferRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(walletService.transfer(currentUser.getId(), req, idempotencyKey));
    }

    @GetMapping("/{walletId}/daily-limit")
    public ResponseEntity<DailyLimitResponse> getDailyLimit(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getDailyLimitStatus(currentUser.getId(), walletId));
    }

    @PatchMapping("/{walletId}/daily-limit")
    public ResponseEntity<DailyLimitResponse> updateDailyLimit(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID walletId,
            @Valid @RequestBody UpdateDailyLimitRequest request) {
        return ResponseEntity.ok(walletService.updateDailyLimit(currentUser.getId(), walletId, request));
    }

    @PostMapping("/pin")
    public ResponseEntity<Void> setPin(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SetPinRequest req) {
        walletService.setPin(currentUser.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/pin")
    public ResponseEntity<Void> changePin(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePinRequest req) {
        walletService.changePin(currentUser.getId(), req);
        return ResponseEntity.noContent().build();
    }
}
