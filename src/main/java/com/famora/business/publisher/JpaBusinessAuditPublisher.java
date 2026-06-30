package com.famora.business.publisher;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.entity.AuditLog;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.business.entity.Business;
import com.famora.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaBusinessAuditPublisher implements BusinessAuditPublisher {
  
  private final AuditLogRepository auditLogRepository;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<HttpServletRequest> requestProvider;
  
  @Override
  public void publishBusinessEvent(
      UUID userId,
      UUID businessId,
      AuditAction action,
      String entityType,
      UUID entityId,
      Map<String, Object> metadata
  ) {
    AuditLog auditLog = AuditLog.builder()
        .user(entityManager.getReference(User.class, userId))
        .business(entityManager.getReference(Business.class, businessId))
        .action(action)
        .entityType(entityType)
        .entityId(entityId)
        .ipAddress(clientIp())
        .userAgent(userAgent())
        .metadata(metadataJson(metadata))
        .build();
    
    auditLogRepository.save(auditLog);
  }
  
  private String metadataJson(Map<String, Object> metadata) {
    try {
      return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
    } catch (JsonProcessingException exception) {
      return "{}";
    }
  }
  
  private String clientIp() {
    HttpServletRequest request = requestProvider.getIfAvailable();
    if (request == null) {
      return null;
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
  
  private String userAgent() {
    HttpServletRequest request = requestProvider.getIfAvailable();
    return request == null ? null : request.getHeader("User-Agent");
  }
}
