package com.famora.document.spec;

import com.famora.document.entity.Document;
import com.famora.document.helper.DocumentType;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class DocumentSpecifications {
  
  private DocumentSpecifications() {
  }
  
  public static Specification<Document> documentType(DocumentType type) {
    return (root, query, cb) -> {
      if (type == null) {
        return cb.conjunction();
      }
      
      return cb.equal(root.get("documentType"), type);
    };
  }
  
  public static Specification<Document> expiringSoon(Boolean expiringSoon, Integer days) {
    return (root, query, cb) -> {
      if (!Boolean.TRUE.equals(expiringSoon)) {
        return cb.conjunction();
      }
      
      int safeDays = days == null || days <= 0 ? 30 : days;
      
      LocalDate today = LocalDate.now();
      LocalDate endDate = today.plusDays(safeDays);
      
      return cb.between(root.get("expiryDate"), today, endDate);
    };
  }
}
