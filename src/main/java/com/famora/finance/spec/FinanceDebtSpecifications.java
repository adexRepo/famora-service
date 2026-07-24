package com.famora.finance.spec;

import com.famora.common.helper.Status;
import com.famora.finance.entity.FinanceDebt;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FinanceDebtSpecifications {
  
  private FinanceDebtSpecifications() {
  }
  
  public static Specification<FinanceDebt> family(UUID familyId) {
    return (root, query, cb) -> cb.equal(root.get("family").get("id"), familyId);
  }
  
  public static Specification<FinanceDebt> status(Status status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
  
  public static Specification<FinanceDebt> debtType(FinanceDebtType type) {
    return (root, query, cb) -> type == null
        ? cb.conjunction()
        : cb.equal(root.get("debtType"), type);
  }
  
  public static Specification<FinanceDebt> debtStatus(FinanceDebtStatus status) {
    return (root, query, cb) -> status == null
        ? cb.conjunction()
        : cb.equal(root.get("debtStatus"), status);
  }
  
  public static Specification<FinanceDebt> excludeCancelledWhenNoStatus(
      FinanceDebtStatus status) {
    return (root, query, cb) -> status != null
        ? cb.conjunction()
        : cb.notEqual(root.get("debtStatus"), FinanceDebtStatus.CANCELLED);
  }
  
  public static Specification<FinanceDebt> keyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) {
        return cb.conjunction();
      }
      String like = "%" + keyword.trim().toLowerCase() + "%";
      return cb.like(cb.lower(root.get("counterpartyName")), like);
    };
  }
}
