package com.famora.currency.service;


import com.famora.currency.config.CurrencyProperties;
import com.famora.currency.dto.FrankfurterLatestResponse;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
  
  private static final String CACHE_KEY = "latest-rates";
  
  private final RestClient restClient;
  private final CurrencyProperties properties;
  
  private LoadingCache<String, FrankfurterLatestResponse> ratesCache;
  
  @PostConstruct
  void initCache() {
    this.ratesCache = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofHours(properties.refreshHours()))
        .expireAfterWrite(Duration.ofHours(properties.refreshHours() + 1L))
        .maximumSize(1)
        .build(key -> fetchLatestRates());
  }
  
  public BigDecimal convert(String fromCcy, String toCcy, BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount is required");
    }
    
    String from = normalizeCurrency(fromCcy);
    String to = normalizeCurrency(toCcy);
    
    validateSupportedCurrency(from);
    validateSupportedCurrency(to);
    
    if (from.equals(to)) {
      return amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    FrankfurterLatestResponse latestRates = ratesCache.get(CACHE_KEY);
    
    BigDecimal fromRate = getRateAgainstBase(from, latestRates);
    BigDecimal toRate = getRateAgainstBase(to, latestRates);
    
    BigDecimal amountInBase = amount.divide(fromRate, 12, RoundingMode.HALF_UP);
    BigDecimal convertedAmount = amountInBase.multiply(toRate);
    
    log.debug("[Convert] fromCcy: [{}], toCcy: [{}], fromRate: [{}], toRate:[{}]", from, to, fromRate, toRate);
    
    return convertedAmount.setScale(2, RoundingMode.HALF_UP);
  }
  
  private FrankfurterLatestResponse fetchLatestRates() {
    String baseCurrency = normalizeCurrency(properties.baseCurrency());
    
    String symbols = properties.supportedCurrencies()
        .stream()
        .map(this::normalizeCurrency)
        .filter(currency -> !currency.equals(baseCurrency))
        .collect(Collectors.joining(","));
    
    return restClient.get()
        .uri(properties.frankfurterUrl() + "?base={base}&symbols={symbols}",
            baseCurrency,
            symbols)
        .retrieve()
        .body(FrankfurterLatestResponse.class);
  }
  
  private BigDecimal getRateAgainstBase(
      String currency,
      FrankfurterLatestResponse latestRates
  ) {
    String baseCurrency = normalizeCurrency(properties.baseCurrency());
    
    if (currency.equals(baseCurrency)) {
      return BigDecimal.ONE;
    }
    
    Map<String, BigDecimal> rates = latestRates.rates();
    
    BigDecimal rate = rates.get(currency);
    
    if (rate == null) {
      throw new IllegalArgumentException("Rate not found for currency: " + currency);
    }
    
    return rate;
  }
  
  private void validateSupportedCurrency(String currency) {
    Set<String> supportedCurrencies = properties.supportedCurrencies()
        .stream()
        .map(this::normalizeCurrency)
        .collect(Collectors.toSet());
    
    if (!supportedCurrencies.contains(currency)) {
      throw new IllegalArgumentException("Unsupported currency: " + currency);
    }
  }
  
  private String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency is required");
    }
    
    return currency.trim().toUpperCase(Locale.ROOT);
  }
}
