package com.bothsann.wallet.shared.exception;

public class SameWalletExchangeException extends RuntimeException {
    public SameWalletExchangeException() {
        super("Source and destination wallets must be different");
    }
}
