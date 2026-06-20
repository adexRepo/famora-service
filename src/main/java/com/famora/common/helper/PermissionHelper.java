package com.famora.common.helper;

import com.famora.common.exception.AppException;
import com.famora.family.dto.FamilyContext;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class PermissionHelper {
  
  public static void assertCanAccess(Visibility visibility, Status status, UUID createdBy,
      FamilyContext ctx) {
    if (status != Status.ACTIVE) {
      throw new AppException(HttpStatus.NOT_FOUND, "Data not found");
    }
    boolean allowed = switch (visibility) {
      case FAMILY -> true;
      case PRIVATE -> createdBy.equals(ctx.user().getId());
      case OWNER_ONLY -> ctx.owner();
    };
    
    if (!allowed) {
      throw new AppException(HttpStatus.FORBIDDEN,
          "You do not have permission to access this resource");
    }
  }
}
