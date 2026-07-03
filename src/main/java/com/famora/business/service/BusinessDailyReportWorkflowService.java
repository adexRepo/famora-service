package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.BUSINESS_ID;
import static com.famora.business.constant.BusinessAuditConstants.DAILY_REPORT;
import static com.famora.business.constant.BusinessAuditConstants.NEW_STATUS;
import static com.famora.business.constant.BusinessAuditConstants.OLD_STATUS;
import static com.famora.business.constant.BusinessAuditConstants.REASON;
import static com.famora.business.constant.BusinessAuditConstants.REPORT_ID;
import static com.famora.business.constant.BusinessAuditConstants.REVISION_NUMBER;

import com.famora.audit.entity.AuditAction;
import com.famora.business.dto.response.DailyReportRevisionDetailResponse;
import com.famora.business.dto.response.DailyReportRevisionListResponse;
import com.famora.business.dto.response.DailyReportWorkflowResponse;
import com.famora.business.dto.response.SubmitDailyReportResponse;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailyReportRevision;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessDailyReportRevisionChangeType;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.DailyReportStatus;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessDailyReportRepository;
import com.famora.business.repository.BusinessDailyReportRevisionRepository;
import com.famora.business.spec.BusinessDailyReportRevisionSpecifications;
import com.famora.business.validator.BusinessDailyReportWorkflowValidator;
import com.famora.common.exception.BusinessDailyReportWorkflowException;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BusinessDailyReportWorkflowService {
  
  private final CurrentUserProvider currentUserProvider;
  private final BusinessPermissionService permissionService;
  private final BusinessDailyReportRepository reportRepository;
  private final BusinessDailyReportRevisionRepository revisionRepository;
  private final BusinessDailyReportWorkflowValidator workflowValidator;
  private final BusinessDailyReportCalculationService calculationService;
  private final BusinessDailyReportRevisionService revisionService;
  private final BusinessAuditPublisher auditPublisher;
  private final ObjectMapper objectMapper;
  
  @Transactional
  public SubmitDailyReportResponse submitReport(UUID businessId, UUID reportId) {
    User currentUser = currentUserProvider.getCurrentUser();
    UUID currentUserId = currentUser.getId();
    BusinessMember member = permissionService.requireAnyRole(
        businessId,
        currentUserId,
        BusinessRole.OWNER,
        BusinessRole.PARTNER,
        BusinessRole.MANAGER,
        BusinessRole.STAFF
    );
    
    BusinessDailyReport report = getReportForUpdate(businessId, reportId);
    DailyReportStatus oldStatus = report.getReportStatus();
    
    workflowValidator.ensureCanSubmit(report, member.getRole(), currentUserId);
    workflowValidator.validateTransition(oldStatus, DailyReportStatus.SUBMITTED);
    
    calculationService.recalculateAndValidateForSubmit(report);
    
    BusinessDailyReportRevisionChangeType changeType =
        oldStatus == DailyReportStatus.REVISION_REQUESTED
            ? BusinessDailyReportRevisionChangeType.REVISION_SUBMITTED
            : BusinessDailyReportRevisionChangeType.SUBMITTED;
    
    report.setReportStatus(DailyReportStatus.SUBMITTED);
    report.setSubmittedAt(OffsetDateTime.now());
    report.setUpdatedBy(currentUser);
    
    if (oldStatus == DailyReportStatus.REVISION_REQUESTED) {
      report.setRevisionRequestedAt(null);
      report.setRevisionRequestedByUserId(null);
      report.setRevisionReason(null);
    }
    
    BusinessDailyReport saved = reportRepository.save(report);
    
    BusinessDailyReportRevision revision = revisionService.createRevision(
        saved,
        changeType,
        oldStatus,
        DailyReportStatus.SUBMITTED,
        currentUser,
        null
    );
    
    AuditAction action = changeType == BusinessDailyReportRevisionChangeType.REVISION_SUBMITTED
        ? AuditAction.BUSINESS_DAILY_REPORT_REVISION_SUBMITTED
        : AuditAction.BUSINESS_DAILY_REPORT_SUBMITTED;
    
    publishReportAudit(
        currentUserId,
        businessId,
        action,
        reportId,
        oldStatus,
        DailyReportStatus.SUBMITTED,
        null,
        revision.getRevisionNumber()
    );
    
    return new SubmitDailyReportResponse(
        saved.getId(),
        saved.getBusiness().getId(),
        oldStatus,
        saved.getReportStatus(),
        revision.getRevisionNumber(),
        saved.getSubmittedAt(),
        "Daily report submitted successfully."
    );
  }
  
  @Transactional
  public DailyReportWorkflowResponse requestRevision(
      UUID businessId,
      UUID reportId,
      String reason
  ) {
    requireReason(reason, "Revision reason is required.");
    
    User currentUser = currentUserProvider.getCurrentUser();
    UUID currentUserId = currentUser.getId();
    BusinessMember access = permissionService.requireAnyRole(
        businessId,
        currentUserId,
        BusinessRole.OWNER,
        BusinessRole.PARTNER
    );
    
    BusinessDailyReport report = getReportForUpdate(businessId, reportId);
    DailyReportStatus oldStatus = report.getReportStatus();
    
    workflowValidator.ensureCanRequestRevision(report, access.getRole());
    workflowValidator.validateTransition(oldStatus, DailyReportStatus.REVISION_REQUESTED);
    
    report.setReportStatus(DailyReportStatus.REVISION_REQUESTED);
    report.setRevisionRequestedAt(OffsetDateTime.now());
    report.setRevisionRequestedByUserId(currentUserId);
    report.setRevisionReason(reason);
    report.setUpdatedBy(currentUser);
    
    BusinessDailyReport saved = reportRepository.save(report);
    
    BusinessDailyReportRevision revision = revisionService.createRevision(
        saved,
        BusinessDailyReportRevisionChangeType.REVISION_REQUESTED,
        oldStatus,
        DailyReportStatus.REVISION_REQUESTED,
        currentUser,
        reason
    );
    
    publishReportAudit(
        currentUserId,
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_REVISION_REQUESTED,
        reportId,
        oldStatus,
        DailyReportStatus.REVISION_REQUESTED,
        reason,
        revision.getRevisionNumber()
    );
    
    return workflowResponse(
        saved,
        oldStatus,
        DailyReportStatus.REVISION_REQUESTED,
        revision.getRevisionNumber(),
        "Daily report revision requested successfully."
    );
  }
  
  @Transactional
  public DailyReportWorkflowResponse approveReport(UUID businessId, UUID reportId) {
    User currentUser = currentUserProvider.getCurrentUser();
    UUID currentUserId = currentUser.getId();
    BusinessMember access = permissionService.requireAnyRole(
        businessId,
        currentUserId,
        BusinessRole.OWNER,
        BusinessRole.PARTNER
    );
    
    BusinessDailyReport report = getReportForUpdate(businessId, reportId);
    DailyReportStatus oldStatus = report.getReportStatus();
    
    workflowValidator.ensureCanApprove(report, access.getRole());
    workflowValidator.validateTransition(oldStatus, DailyReportStatus.APPROVED);
    
    calculationService.recalculateAndValidateForSubmit(report);
    
    report.setReportStatus(DailyReportStatus.APPROVED);
    report.setApprovedByUserId(currentUserId);
    report.setApprovedAt(OffsetDateTime.now());
    report.setUpdatedBy(currentUser);
    
    BusinessDailyReport saved = reportRepository.save(report);
    
    BusinessDailyReportRevision revision = revisionService.createRevision(
        saved,
        BusinessDailyReportRevisionChangeType.APPROVED,
        oldStatus,
        DailyReportStatus.APPROVED,
        currentUser,
        null
    );
    
    publishReportAudit(
        currentUserId,
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_APPROVED,
        reportId,
        oldStatus,
        DailyReportStatus.APPROVED,
        null,
        revision.getRevisionNumber()
    );
    
    return workflowResponse(
        saved,
        oldStatus,
        DailyReportStatus.APPROVED,
        revision.getRevisionNumber(),
        "Daily report approved successfully."
    );
  }
  
  @Transactional
  public DailyReportWorkflowResponse rejectReport(
      UUID businessId,
      UUID reportId,
      String reason
  ) {
    requireReason(reason, "Reject reason is required.");
    
    User currentUser = currentUserProvider.getCurrentUser();
    UUID currentUserId = currentUser.getId();
    BusinessMember access = permissionService.requireAnyRole(
        businessId,
        currentUserId,
        BusinessRole.OWNER,
        BusinessRole.PARTNER
    );
    
    BusinessDailyReport report = getReportForUpdate(businessId, reportId);
    DailyReportStatus oldStatus = report.getReportStatus();
    
    workflowValidator.ensureCanReject(report, access.getRole());
    workflowValidator.validateTransition(oldStatus, DailyReportStatus.REJECTED);
    
    report.setReportStatus(DailyReportStatus.REJECTED);
    report.setRejectedByUserId(currentUserId);
    report.setRejectedAt(OffsetDateTime.now());
    report.setRejectionReason(reason);
    report.setUpdatedBy(currentUser);
    
    BusinessDailyReport saved = reportRepository.save(report);
    
    BusinessDailyReportRevision revision = revisionService.createRevision(
        saved,
        BusinessDailyReportRevisionChangeType.REJECTED,
        oldStatus,
        DailyReportStatus.REJECTED,
        currentUser,
        reason
    );
    
    publishReportAudit(
        currentUserId,
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_REJECTED,
        reportId,
        oldStatus,
        DailyReportStatus.REJECTED,
        reason,
        revision.getRevisionNumber()
    );
    
    return workflowResponse(
        saved,
        oldStatus,
        DailyReportStatus.REJECTED,
        revision.getRevisionNumber(),
        "Daily report rejected successfully."
    );
  }
  
  @Transactional
  public DailyReportWorkflowResponse voidReport(
      UUID businessId,
      UUID reportId,
      String reason
  ) {
    requireReason(reason, "Void reason is required.");
    
    User currentUser = currentUserProvider.getCurrentUser();
    UUID currentUserId = currentUser.getId();
    BusinessMember access = permissionService.requireAnyRole(
        businessId,
        currentUserId,
        BusinessRole.OWNER,
        BusinessRole.PARTNER
    );
    
    BusinessDailyReport report = getReportForUpdate(businessId, reportId);
    DailyReportStatus oldStatus = report.getReportStatus();
    
    workflowValidator.ensureCanVoid(report, access.getRole());
    workflowValidator.validateTransition(oldStatus, DailyReportStatus.VOIDED);
    
    report.setReportStatus(DailyReportStatus.VOIDED);
    report.setVoidedByUserId(currentUserId);
    report.setVoidedAt(OffsetDateTime.now());
    report.setVoidReason(reason);
    report.setUpdatedBy(currentUser);
    
    BusinessDailyReport saved = reportRepository.save(report);
    
    BusinessDailyReportRevision revision = revisionService.createRevision(
        saved,
        BusinessDailyReportRevisionChangeType.VOIDED,
        oldStatus,
        DailyReportStatus.VOIDED,
        currentUser,
        reason
    );
    
    publishReportAudit(
        currentUserId,
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_VOIDED,
        reportId,
        oldStatus,
        DailyReportStatus.VOIDED,
        reason,
        revision.getRevisionNumber()
    );
    
    return workflowResponse(
        saved,
        oldStatus,
        DailyReportStatus.VOIDED,
        revision.getRevisionNumber(),
        "Daily report voided successfully."
    );
  }
  
  @Transactional(readOnly = true)
  public Page<DailyReportRevisionListResponse> getRevisionHistory(
      UUID businessId,
      UUID reportId,
      Pageable pageable
  ) {
    UUID currentUserId = currentUserProvider.getCurrentUserId();
    BusinessMember access = permissionService.requireActiveMember(businessId, currentUserId);
    BusinessDailyReport report = getReport(businessId, reportId);
    
    workflowValidator.ensureCanViewRevision(report, access.getRole(), currentUserId);
    
    return revisionRepository
        .findAll(BusinessDailyReportRevisionSpecifications.belongsToBusiness(businessId)
                .and(BusinessDailyReportRevisionSpecifications.belongsToReport(reportId))
                .and(BusinessDailyReportRevisionSpecifications.status(Status.ACTIVE)),
            defaultRevisionSort(pageable))
        .map(DailyReportRevisionListResponse::from);
  }
  
  @Transactional(readOnly = true)
  public DailyReportRevisionDetailResponse getRevisionDetail(
      UUID businessId,
      UUID reportId,
      UUID revisionId
  ) {
    UUID currentUserId = currentUserProvider.getCurrentUserId();
    BusinessMember access = permissionService.requireActiveMember(businessId, currentUserId);
    BusinessDailyReport report = getReport(businessId, reportId);
    
    workflowValidator.ensureCanViewRevision(report, access.getRole(), currentUserId);
    
    BusinessDailyReportRevision revision = revisionRepository
        .findByIdAndBusiness_IdAndDailyReport_Id(revisionId, businessId, reportId)
        .orElseThrow(
            () -> new BusinessDailyReportWorkflowException("Daily report revision not found."));
    
    try {
      JsonNode snapshot = objectMapper.readTree(revision.getSnapshotJson());
      return DailyReportRevisionDetailResponse.from(revision, snapshot);
    } catch (JsonProcessingException exception) {
      throw new BusinessDailyReportWorkflowException(
          "Failed to parse daily report revision snapshot.");
    }
  }
  
  private BusinessDailyReport getReportForUpdate(UUID businessId, UUID reportId) {
    return reportRepository.findByIdAndBusinessIdForUpdate(businessId, reportId)
        .orElseThrow(() -> new BusinessDailyReportWorkflowException("Daily report not found."));
  }
  
  private BusinessDailyReport getReport(UUID businessId, UUID reportId) {
    return reportRepository.findByIdAndBusinessId(businessId, reportId)
        .orElseThrow(() -> new BusinessDailyReportWorkflowException("Daily report not found."));
  }
  
  private void requireReason(String reason, String message) {
    if (!StringUtils.hasText(reason)) {
      throw new BusinessDailyReportWorkflowException(message);
    }
  }
  
  private Pageable defaultRevisionSort(Pageable pageable) {
    if (pageable.getSort().isSorted()) {
      return pageable;
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "revisionNumber"));
  }
  
  private DailyReportWorkflowResponse workflowResponse(
      BusinessDailyReport report,
      DailyReportStatus oldStatus,
      DailyReportStatus newStatus,
      Integer revisionNumber,
      String message
  ) {
    return new DailyReportWorkflowResponse(
        report.getId(),
        report.getBusiness().getId(),
        oldStatus,
        newStatus,
        revisionNumber,
        OffsetDateTime.now(),
        message
    );
  }
  
  private void publishReportAudit(
      UUID currentUserId,
      UUID businessId,
      AuditAction action,
      UUID reportId,
      DailyReportStatus oldStatus,
      DailyReportStatus newStatus,
      String reason,
      Integer revisionNumber
  ) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(BUSINESS_ID, businessId);
    metadata.put(REPORT_ID, reportId);
    metadata.put(OLD_STATUS, oldStatus);
    metadata.put(NEW_STATUS, newStatus);
    metadata.put(REVISION_NUMBER, revisionNumber);
    if (StringUtils.hasText(reason)) {
      metadata.put(REASON, reason);
    }
    
    auditPublisher.publishBusinessEvent(
        currentUserId,
        businessId,
        action,
        DAILY_REPORT,
        reportId,
        metadata
    );
  }
}
