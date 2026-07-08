package com.famora.dashboard;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.entity.AuditLog;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.business.service.BusinessPermissionService;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.document.repository.DocumentRepository;
import com.famora.family.dto.FamilyContext;
import com.famora.file.repository.FileRepository;
import com.famora.note.repository.NoteRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.security.FamilyContextService;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardActivityService {
  
  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 50;
  
  private static final EnumSet<AuditAction> FAMILY_ACTIVITY_ACTIONS = EnumSet.of(
      AuditAction.FAMILY_CREATED,
      AuditAction.FAMILY_UPDATED,
      AuditAction.FAMILY_DEFAULT_SET,
      AuditAction.FAMILY_MEMBER_INVITED,
      AuditAction.FAMILY_MEMBER_JOINED,
      AuditAction.FAMILY_MEMBER_REMOVED,
      AuditAction.FAMILY_MEMBER_ROLE_UPDATED,
      AuditAction.NOTE_CREATED,
      AuditAction.NOTE_UPDATED,
      AuditAction.NOTE_DELETED,
      AuditAction.FINANCE_TRANSACTION_CREATED,
      AuditAction.FINANCE_TRANSACTION_UPDATED,
      AuditAction.FINANCE_TRANSACTION_DELETED,
      AuditAction.DOCUMENT_CREATED,
      AuditAction.DOCUMENT_UPDATED,
      AuditAction.FILE_CREATED,
      AuditAction.FILE_UPDATED,
      AuditAction.EMERGENCY_CONTACT_CREATED,
      AuditAction.EMERGENCY_CONTACT_UPDATED,
      AuditAction.EMERGENCY_CONTACT_DELETED
  );
  
  private static final EnumSet<AuditAction> BUSINESS_ACTIVITY_ACTIONS = EnumSet.of(
      AuditAction.BUSINESS_CREATED,
      AuditAction.BUSINESS_UPDATED,
      AuditAction.BUSINESS_DELETED,
      AuditAction.BUSINESS_DEFAULT_SET,
      AuditAction.BUSINESS_PRODUCT_CREATED,
      AuditAction.BUSINESS_PRODUCT_UPDATED,
      AuditAction.BUSINESS_PRODUCT_DELETED,
      AuditAction.BUSINESS_EXPENSE_CREATED,
      AuditAction.BUSINESS_EXPENSE_UPDATED,
      AuditAction.BUSINESS_EXPENSE_DELETED,
      AuditAction.BUSINESS_INVITATION_CREATED,
      AuditAction.BUSINESS_INVITATION_CANCELLED,
      AuditAction.BUSINESS_INVITATION_ACCEPTED,
      AuditAction.BUSINESS_MEMBER_ROLE_UPDATED,
      AuditAction.BUSINESS_MEMBER_REMOVED,
      AuditAction.BUSINESS_DAILY_REPORT_CREATED,
      AuditAction.BUSINESS_DAILY_REPORT_SUBMITTED,
      AuditAction.BUSINESS_DAILY_REPORT_REVISION_SUBMITTED,
      AuditAction.BUSINESS_DAILY_REPORT_REVISION_REQUESTED,
      AuditAction.BUSINESS_DAILY_REPORT_APPROVED,
      AuditAction.BUSINESS_DAILY_REPORT_REJECTED,
      AuditAction.BUSINESS_DAILY_REPORT_VOIDED
  );
  
  private final AuditLogRepository auditLogRepository;
  private final FamilyContextService families;
  private final BusinessPermissionService businessPermissionService;
  private final CurrentUserProvider currentUserProvider;
  private final DocumentRepository documentRepository;
  private final FileRepository fileRepository;
  private final NoteRepository noteRepository;
  
  @Transactional(readOnly = true)
  public List<RecentActivityResponse> familyActivities(String familyIdHeader, Integer limit) {
    FamilyContext ctx = families.require(familyIdHeader);
    UUID familyId = ctx.family().getId();
    int normalizedLimit = normalizeLimit(limit);
    return auditLogRepository.findRecentFamilyActivities(familyId, FAMILY_ACTIVITY_ACTIONS,
            PageRequest.of(0, queryLimit(normalizedLimit)))
        .stream()
        .filter(log -> visibleToFamilyContext(log, ctx))
        .limit(normalizedLimit)
        .map(this::toResponse)
        .toList();
  }
  
  @Transactional(readOnly = true)
  public List<RecentActivityResponse> businessActivities(UUID businessId, Integer limit) {
    businessPermissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return auditLogRepository.findRecentBusinessActivities(businessId, BUSINESS_ACTIVITY_ACTIONS,
            PageRequest.of(0, normalizeLimit(limit)))
        .stream()
        .map(this::toResponse)
        .toList();
  }
  
  private RecentActivityResponse toResponse(AuditLog log) {
    UUID userId = log.getUser() == null ? null : log.getUser().getId();
    return new RecentActivityResponse(log.getId(), userId, log.getAction().name(),
        log.getEntityType(), log.getEntityId(), humanize(log.getAction()), log.getCreatedAt());
  }
  
  private static int normalizeLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    if (limit < 1) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }
  
  private static int queryLimit(int normalizedLimit) {
    return Math.min(normalizedLimit * 3, MAX_LIMIT * 3);
  }
  
  private boolean visibleToFamilyContext(AuditLog log, FamilyContext ctx) {
    if (log.getEntityId() == null || log.getEntityType() == null) {
      return true;
    }
    
    UUID familyId = ctx.family().getId();
    return switch (log.getEntityType()) {
      case "documents" -> documentRepository
          .findByIdAndFamilyIdAndStatus(log.getEntityId(), familyId, Status.ACTIVE)
          .map(document -> canAccess(document.getVisibility(), document.getCreatedBy().getId(), ctx))
          .orElse(false);
      case "files" -> fileRepository
          .findByIdAndFamilyIdAndStatus(log.getEntityId(), familyId, Status.ACTIVE)
          .map(file -> canAccess(file.getVisibility(), file.getCreatedBy().getId(), ctx))
          .orElse(false);
      case "notes" -> noteRepository
          .findByIdAndFamilyIdAndStatus(log.getEntityId(), familyId, Status.ACTIVE)
          .map(note -> canAccess(note.getVisibility(), note.getCreatedBy().getId(), ctx))
          .orElse(false);
      default -> true;
    };
  }
  
  private static boolean canAccess(Visibility visibility, UUID createdBy, FamilyContext ctx) {
    return switch (visibility) {
      case FAMILY -> true;
      case PRIVATE -> createdBy.equals(ctx.user().getId());
      case OWNER_ONLY -> ctx.owner();
    };
  }
  
  private static String humanize(AuditAction action) {
    String text = action.name().replace('_', ' ').toLowerCase();
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
