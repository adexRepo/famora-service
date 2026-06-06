package com.famora.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FinanceTransactionResponse(
    UUID id,
    String type,
    BigDecimal amount,
    String currency,
    String category,
    String description,
    LocalDate transactionDate,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
