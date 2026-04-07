package com.bothsann.wallet.shared.currency;

import com.bothsann.wallet.shared.exception.ExchangeRateUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final CurrencyProperties currencyProperties;
    private final RestClient restClient;

    public ExchangeRateService(ExchangeRateRepository repository,
                               CurrencyProperties currencyProperties,
                               RestClient exchangeRateRestClient) {
        this.repository = repository;
        this.currencyProperties = currencyProperties;
        this.restClient = exchangeRateRestClient;
    }

    /**
     * Returns the exchange rate from one currency to another.
     * Reads from the DB cache populated by refreshRates().
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        return repository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new ExchangeRateUnavailableException(fromCurrency, toCurrency));
    }

    /**
     * Converts an amount from one currency to another using the cached rate.
     * Result is rounded to 4 decimal places (matching wallet balance precision).
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Runs once on startup so rates are available before the first transfer request.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refreshRates();
    }

    /**
     * Fetches all rates relative to USD from ExchangeRate-API in a single call,
     * then computes and upserts all supported currency pair rates into the DB.
     * Runs every hour (configurable via application.currency.refresh-interval-ms).
     * A failure does not throw — the app continues serving transfers from cached rates.
     */
    @Scheduled(fixedRateString = "${application.currency.refresh-interval-ms:3600000}")
    public void refreshRates() {
        try {
            ExchangeRateApiResponse response = restClient.get()
                    .uri("/v6/{key}/latest/USD", currencyProperties.getApiKey())
                    .retrieve()
                    .body(ExchangeRateApiResponse.class);

            if (response == null || !"success".equals(response.result())) {
                log.error("Exchange rate API returned error: {}",
                        response != null ? response.errorType() : "null response");
                return;
            }

            Map<String, BigDecimal> usdRates = response.conversionRates();
            List<String> supported = currencyProperties.getSupported();
            int count = 0;

            for (String from : supported) {
                for (String to : supported) {
                    if (from.equals(to)) continue;
                    BigDecimal rate = computeRate(from, to, usdRates);
                    if (rate != null) {
                        upsertRate(from, to, rate);
                        count++;
                    } else {
                        log.warn("Could not compute rate for {} → {}: missing USD base rate", from, to);
                    }
                }
            }
            log.info("Refreshed {} exchange rate pairs", count);

        } catch (Exception e) {
            log.error("Failed to refresh exchange rates: {}", e.getMessage());
        }
    }

    /**
     * Computes the rate from → to using USD as the intermediary base.
     *
     * USD→X  : usdRates[X]
     * X→USD  : 1 / usdRates[X]
     * X→Y    : usdRates[Y] / usdRates[X]   (cross-rate via USD)
     */
    private BigDecimal computeRate(String from, String to, Map<String, BigDecimal> usdRates) {
        BigDecimal fromRate = usdRates.get(from); // USD → from
        BigDecimal toRate   = usdRates.get(to);   // USD → to
        if (fromRate == null || toRate == null || fromRate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }

    private void upsertRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        ExchangeRate record = repository
                .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency)
                .orElse(new ExchangeRate(fromCurrency, toCurrency, BigDecimal.ZERO));
        record.setRate(rate);
        repository.save(record);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExchangeRateApiResponse(
            String result,
            @JsonProperty("base_code")        String baseCode,
            @JsonProperty("conversion_rates") Map<String, BigDecimal> conversionRates,
            @JsonProperty("error-type")       String errorType
    ) {}
}
