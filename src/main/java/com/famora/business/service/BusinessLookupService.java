package com.famora.business.service;

import com.famora.business.dto.response.LookupItemResponse;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.LossReason;
import com.famora.business.enums.PaymentMethod;
import com.famora.business.enums.UnitType;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class BusinessLookupService {
  
  public List<LookupItemResponse> roles() {
    return fromEnum(BusinessRole.values());
  }
  
  public List<LookupItemResponse> paymentMethods() {
    return fromEnum(PaymentMethod.values());
  }
  
  public List<LookupItemResponse> expenseCategories() {
    return fromEnum(ExpenseCategory.values());
  }
  
  public List<LookupItemResponse> lossReasons() {
    return fromEnum(LossReason.values());
  }
  
  public List<LookupItemResponse> units() {
    return fromEnum(UnitType.values());
  }
  
  private List<LookupItemResponse> fromEnum(Enum<?>[] values) {
    return Stream.of(values)
        .map(v -> new LookupItemResponse(v.name(), toLabel(v.name())))
        .toList();
  }
  
  private String toLabel(String value) {
    String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
