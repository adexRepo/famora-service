package com.famora.vault.spec;

import com.famora.vault.entity.VaultItem;
import org.springframework.data.jpa.domain.Specification;

public final class VaultItemSpecifications {
  
  private VaultItemSpecifications() {
  }
  
  public static Specification<VaultItem> keyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) {
        return cb.conjunction();
      }
      
      String like = "%" + keyword.trim().toLowerCase() + "%";
      
      return cb.or(
          cb.like(cb.lower(root.get("title")), like),
          cb.like(cb.lower(cb.coalesce(root.get("username"), "")), like),
          cb.like(cb.lower(cb.coalesce(root.get("url"), "")), like)
      );
    };
  }
}
