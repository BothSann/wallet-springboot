package com.bothsann.wallet.shared.currency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "application.currency")
@Getter
@Setter
public class CurrencyProperties {

    private List<String> supported = List.of("USD", "KHR");
    private List<String> walletCurrencies = List.of("USD", "KHR");
    private String apiUrl = "https://v6.exchangerate-api.com";
    private String apiKey = "";
    private long refreshIntervalMs = 3_600_000;
}
