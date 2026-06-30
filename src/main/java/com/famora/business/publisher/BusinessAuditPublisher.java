package com.famora.business.publisher;

import com.famora.audit.entity.AuditAction;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter port for existing AuditService.
 *
 * Important:
 * - Publish write/status-change events only.
 * - Do not publish GET/list/search/summary/dropdown events.
 * - Keep metadata small.
 * - Do not store full report snapshots in audit_logs.
 */
public interface BusinessAuditPublisher {

  void publishBusinessEvent(
      UUID userId,
      UUID businessId,
      AuditAction action,
      String entityType,
      UUID entityId,
      Map<String, Object> metadata
  );
}
