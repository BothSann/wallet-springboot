package com.bothsann.wallet.shared.exception;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currency, java.util.List<String> supported) {
        super("Unsupported currency: " + currency + ". Supported currencies are: " + String.join(", ", supported));
    }
}
