package com.famora.tracker.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogAsyncWriter;
import com.famora.tracker.entity.Tracker;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackerAuditService {
  
  private final AuditLogAsyncWriter asyncWriter;
  private final ObjectProvider<HttpServletRequest> requestProvider;
  
  public void log(Tracker tracker, AuditAction action, String entityType, UUID entityId,
      String metadataJson) {
    HttpServletRequest request = requestProvider.getIfAvailable();
    asyncWriter.write(
        tracker.getFamily() == null ? null : tracker.getFamily().getId(),
        tracker.getOwnerUser() == null ? null : tracker.getOwnerUser().getId(),
        tracker.getBusiness() == null ? null : tracker.getBusiness().getId(),
        action,
        entityType,
        entityId,
        clientIp(request),
        request == null ? null : request.getHeader("User-Agent"),
        metadataJson
    );
  }
  
  private String clientIp(HttpServletRequest request) {
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
