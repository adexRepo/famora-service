package com.famora.audit.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.entity.AuditLog;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
  
  private final AuditLogRepository auditLogRepository;
  private final HttpServletRequest request;
  
  public void log(Family family, User user, AuditAction action, String entityType, UUID entityId,
      String metadataJson) {
    AuditLog auditLog = AuditLog.builder()
        .family(family).user(user).action(action).entityType(entityType).entityId(entityId)
        .ipAddress(getClientIp()).userAgent(request.getHeader("User-Agent")).metadata(metadataJson)
        .build();
    auditLogRepository.save(auditLog);
  }
  
  private String getClientIp() {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
