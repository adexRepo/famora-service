package com.famora.audit.repository;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.entity.AuditLog;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  
  @Query("""
      select a from AuditLog a
      where a.family.id = :familyId
        and a.action in :actions
      order by a.createdAt desc
      """)
  List<AuditLog> findRecentFamilyActivities(UUID familyId, Collection<AuditAction> actions,
      Pageable pageable);
  
  @Query("""
      select a from AuditLog a
      where a.business.id = :businessId
        and a.action in :actions
      order by a.createdAt desc
      """)
  List<AuditLog> findRecentBusinessActivities(UUID businessId, Collection<AuditAction> actions,
      Pageable pageable);
}
