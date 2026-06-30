package com.famora.business.dto.response;

import com.famora.business.enums.LossReason;
import java.math.BigDecimal;

public record LossSummaryResponse(LossReason reason, BigDecimal quantityLoss, BigDecimal estimatedTotalValue) {}
