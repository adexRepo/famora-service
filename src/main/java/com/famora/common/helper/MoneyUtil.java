package com.famora.common.helper;

import com.famora.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {
  
  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  
  private MoneyUtil() {
  }
  
  public static BigDecimal nvl(BigDecimal value) {
    return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }
  
  public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
    return nvl(a).multiply(nvl(b)).setScale(2, RoundingMode.HALF_UP);
  }
  
  public static void requireNonNegative(BigDecimal value, String field) {
    if (nvl(value).compareTo(BigDecimal.ZERO) < 0) {
      throw BusinessException.validation(field + " must be >= 0");
    }
  }
  
  public static void requirePositive(BigDecimal value, String field) {
    if (nvl(value).compareTo(BigDecimal.ZERO) <= 0) {
      throw BusinessException.validation(field + " must be > 0");
    }
  }
}
