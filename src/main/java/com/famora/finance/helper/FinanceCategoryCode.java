package com.famora.finance.helper;

import com.famora.common.exception.AppException;
import com.famora.finance.entity.FinanceTransactionType;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class FinanceCategoryCode {
  
  public static final String DEBT_RECEIVED = "DEBT_RECEIVED";
  public static final String RECEIVABLE_REPAYMENT = "RECEIVABLE_REPAYMENT";
  public static final String RECEIVABLE_DISBURSEMENT = "RECEIVABLE_DISBURSEMENT";
  public static final String DEBT_REPAYMENT = "DEBT_REPAYMENT";
  
  private static final Set<String> INCOME_CODES = Set.of(
      "SALARY",
      "BUSINESS",
      "CAPITAL_GAIN",
      "DIVIDEND",
      "FREELANCE",
      "GIFT",
      "BONUS",
      "REFUND",
      DEBT_RECEIVED,
      RECEIVABLE_REPAYMENT,
      "OTHER"
  );
  
  private static final Set<String> EXPENSE_CODES = Set.of(
      "FOOD",
      "GROCERY",
      "TRANSPORTATION",
      "ENTERTAINMENT",
      "UTILITIES",
      "HEALTHCARE",
      "SHOPPING",
      "EDUCATION",
      "HOUSING",
      "INSURANCE",
      RECEIVABLE_DISBURSEMENT,
      DEBT_REPAYMENT,
      "CHARITY",
      "OTHER"
  );
  
  private FinanceCategoryCode() {
  }
  
  public static String normalizeAndValidate(FinanceTransactionType type, String category) {
    if (type == null) {
      throw new AppException(HttpStatus.BAD_REQUEST, "type is required");
    }
    if (category == null || category.isBlank()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "category is required");
    }
    
    String code = category.trim().toUpperCase();
    boolean allowed = switch (type) {
      case INCOME -> INCOME_CODES.contains(code);
      case EXPENSE -> EXPENSE_CODES.contains(code);
    };
    
    if (!allowed) {
      throw new AppException(HttpStatus.BAD_REQUEST,
          "Invalid finance category for " + type + ": " + code);
    }
    
    return code;
  }
}
