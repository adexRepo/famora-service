package com.famora.finance.dto;

import java.math.BigDecimal;

public interface CurrencyAmountProjection {
  
  String getCurrency();
  
  BigDecimal getTotalAmount();
}
