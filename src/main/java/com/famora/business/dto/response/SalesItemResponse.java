package com.famora.business.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesItemResponse(UUID id, UUID productId, String itemName, String unit,
                                BigDecimal quantitySold, BigDecimal unitPrice,
                                BigDecimal totalAmount, String notes) {}
