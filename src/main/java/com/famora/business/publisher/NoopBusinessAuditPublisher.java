package com.famora.business.publisher;

import com.famora.audit.entity.AuditAction;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Safe default so the workflow code works before wiring the real AuditService.
 * Replace by providing your own BusinessAuditPublisher bean.
 */
@Component
@ConditionalOnMissingBean(BusinessAuditPublisher.class)
public class NoopBusinessAuditPublisher implements BusinessAuditPublisher {
  @Override
  public void publishBusinessEvent(
      UUID userId,
      UUID businessId,
      AuditAction action,
      String entityType,
      UUID entityId,
      Map<String, Object> metadata
  ) {
    // intentionally empty
  }
}
