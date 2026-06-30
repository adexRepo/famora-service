package com.famora.business.spec;

import com.famora.business.entity.BusinessExpense;
import com.famora.common.helper.Status;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessExpenseSpecifications {
  
  private BusinessExpenseSpecifications() {
  }
  
  public static Specification<BusinessExpense> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessExpense> expenseDateBetween(LocalDate fromDate,
      LocalDate toDate) {
    return (root, query, cb) -> cb.between(root.get("expenseDate"), fromDate, toDate);
  }
  
  public static Specification<BusinessExpense> status(Status status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
  
  public static Specification<BusinessExpense> statusNot(Status status) {
    return (root, query, cb) -> cb.notEqual(root.get("status"), status);
  }
}
