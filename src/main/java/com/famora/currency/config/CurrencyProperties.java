package com.famora.currency.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.currency")
public record CurrencyProperties(
    String baseCurrency,
    List<String> supportedCurrencies,
    String frankfurterUrl,
    int refreshHours
) {
}
