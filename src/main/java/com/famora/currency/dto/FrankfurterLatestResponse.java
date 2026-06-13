package com.famora.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record FrankfurterLatestResponse(
    BigDecimal amount,
    String base,
    LocalDate date,
    Map<String, BigDecimal> rates
) {
}
