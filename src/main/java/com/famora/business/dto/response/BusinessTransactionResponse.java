package com.famora.business.dto.response;

import com.famora.business.enums.BusinessTransactionType;
import com.famora.business.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessTransactionResponse(
    UUID id,
    UUID businessId,
    String sourceType,
    UUID sourceId,
    UUID dailyReportId,
    LocalDate transactionDate,
    BusinessTransactionType type,
    String name,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    String category,
    String notes,
    OffsetDateTime createdAt
) {
}
