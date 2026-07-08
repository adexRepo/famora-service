package com.famora.audit.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.entity.AuditLog;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.business.entity.Business;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogAsyncWriter {
  
  private final AuditLogRepository auditLogRepository;
  private final EntityManager entityManager;
  
  @Async("auditTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void write(UUID familyId, UUID userId, UUID businessId, AuditAction action,
      String entityType, UUID entityId, String ipAddress, String userAgent, String metadataJson) {
    try {
      AuditLog auditLog = AuditLog.builder()
          .family(reference(Family.class, familyId))
          .user(reference(User.class, userId))
          .business(reference(Business.class, businessId))
          .action(action)
          .entityType(entityType)
          .entityId(entityId)
          .ipAddress(ipAddress)
          .userAgent(userAgent)
          .metadata(metadataJson)
          .build();
      auditLogRepository.save(auditLog);
    } catch (Exception exception) {
      log.warn("Failed to write audit log action={} entityType={} entityId={}", action, entityType,
          entityId, exception);
    }
  }
  
  private <T> T reference(Class<T> entityClass, UUID id) {
    return id == null ? null : entityManager.getReference(entityClass, id);
  }
}
