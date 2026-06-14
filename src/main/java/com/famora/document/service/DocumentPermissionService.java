package com.famora.document.service;

import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.document.entity.Document;
import com.famora.family.dto.FamilyContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DocumentPermissionService {
  
  public void assertCanAccess(Document d, FamilyContext ctx) {
    if (d.getStatus() != Status.ACTIVE) {
      throw new AppException(HttpStatus.NOT_FOUND, "Document not found");
    }
    boolean allowed = switch (d.getVisibility()) {
      case FAMILY -> true;
      case PRIVATE -> d.getOwnerUserId() != null && d.getOwnerUserId().equals(ctx.userId());
      case OWNER_ONLY -> ctx.owner();
    };
    if (!allowed) {
      throw new AppException(HttpStatus.FORBIDDEN,
          "You do not have permission to access this com.famora.document");
    }
  }
}
