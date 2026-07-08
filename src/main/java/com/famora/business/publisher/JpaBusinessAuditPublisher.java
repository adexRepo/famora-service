package com.famora.business.publisher;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogAsyncWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaBusinessAuditPublisher implements BusinessAuditPublisher {
  
  private final AuditLogAsyncWriter asyncWriter;
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
    asyncWriter.write(null, userId, businessId, action, entityType, entityId, clientIp(),
        userAgent(), metadataJson(metadata));
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
