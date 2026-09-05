package com.famora.admin.spec;

import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class AdminUserSpecifications {

  public static Specification<User> matchesQuery(String query) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      if (query == null || query.isBlank()) {
        return criteriaBuilder.conjunction();
      }
      String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
      return criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern, '\\'),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern, '\\')
      );
    };
  }

  public static Specification<User> hasStatus(UserStatus status) {
    return (root, criteriaQuery, criteriaBuilder) -> status == null
        ? criteriaBuilder.conjunction()
        : criteriaBuilder.equal(root.get("status"), status);
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }
}
