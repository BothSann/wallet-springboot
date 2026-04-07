package com.bothsann.wallet.shared.exception;

public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String fromCurrency, String toCurrency) {
        super("Exchange rate unavailable for " + fromCurrency + " \u2192 " + toCurrency
                + ". Rates are refreshed every hour \u2014 please try again shortly.");
    }
}
