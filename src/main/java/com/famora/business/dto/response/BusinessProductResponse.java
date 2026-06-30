package com.famora.business.dto.response;

import com.famora.common.helper.Status;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessProductResponse(UUID id, UUID businessId, String productName, String category, String unit,
                                      BigDecimal defaultSellingPrice, BigDecimal costPrice, Status status,
                                      OffsetDateTime createdDt, OffsetDateTime updatedDt) {}
