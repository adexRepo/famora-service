package com.famora.business.dto.response;

import com.famora.business.enums.LossReason;
import java.math.BigDecimal;
import java.util.UUID;

public record LossItemResponse(UUID id, String itemName, String unit, BigDecimal quantityLoss,
                               BigDecimal estimatedUnitValue, BigDecimal estimatedTotalValue,
                               LossReason reason, String notes) {}
