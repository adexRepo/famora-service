package com.famora.audit.service;

import com.famora.audit.entity.AuditAction;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
  
  private final AuditLogAsyncWriter asyncWriter;
  private final ObjectProvider<HttpServletRequest> requestProvider;
  
  public void log(Family family, User user, AuditAction action, String entityType, UUID entityId,
      String metadataJson) {
    HttpServletRequest request = requestProvider.getIfAvailable();
    asyncWriter.write(
        family == null ? null : family.getId(),
        user == null ? null : user.getId(),
        null,
        action,
        entityType,
        entityId,
        getClientIp(request),
        request == null ? null : request.getHeader("User-Agent"),
        metadataJson
    );
  }
  
  private String getClientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
