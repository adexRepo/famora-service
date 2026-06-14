package com.famora.file.service;

import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.family.dto.FamilyContext;
import com.famora.file.entity.FileAsset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FilePermissionService {
  
  public void assertCanAccess(FileAsset file, FamilyContext ctx) {
    if (file.getStatus() != Status.ACTIVE) {
      throw new AppException(HttpStatus.NOT_FOUND, "File not found");
    }
    boolean allowed = switch (file.getVisibility()) {
      case FAMILY -> true;
      case PRIVATE -> file.getUploadedByUserId().equals(ctx.userId().getId());
      case OWNER_ONLY -> ctx.owner();
    };
    if (!allowed) {
      throw new AppException(HttpStatus.FORBIDDEN,
          "You do not have permission to access this com.famora.file");
    }
  }
}
