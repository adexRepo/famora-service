package com.famora.finance.spec;

import com.famora.common.helper.Status;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FinanceTransactionSpecifications {
  
  private FinanceTransactionSpecifications() {
  }
  
  public static Specification<FinanceTransaction> family(UUID familyId) {
    return (root, query, cb) ->
        cb.equal(root.get("family").get("id"), familyId);
  }
  
  public static Specification<FinanceTransaction> status(Status status) {
    return (root, query, cb) ->
        cb.equal(root.get("status"), status);
  }
  
  public static Specification<FinanceTransaction> transactionDateBetween(
      LocalDate startDate,
      LocalDate endDate
  ) {
    return (root, query, cb) ->
        cb.between(root.get("transactionDate"), startDate, endDate);
  }
  
  public static Specification<FinanceTransaction> type(FinanceTransactionType type) {
    return (root, query, cb) -> {
      if (type == null) {
        return cb.conjunction();
      }
      
      return cb.equal(root.get("type"), type);
    };
  }
  
  public static Specification<FinanceTransaction> category(String category) {
    return (root, query, cb) -> {
      if (category == null || category.isBlank()) {
        return cb.conjunction();
      }
      
      return cb.equal(
          cb.lower(cb.coalesce(root.get("category"), "")),
          category.trim().toLowerCase()
      );
    };
  }
}
