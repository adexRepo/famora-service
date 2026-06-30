package com.famora.business.dto.response;

import java.math.BigDecimal;

public record TopSalesItemResponse(String itemName, BigDecimal quantitySold, BigDecimal totalAmount) {}
