package com.famora.business.dto.response;

import com.famora.business.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentBreakdownResponse(UUID id, PaymentMethod paymentMethod, BigDecimal amount, String notes) {}
