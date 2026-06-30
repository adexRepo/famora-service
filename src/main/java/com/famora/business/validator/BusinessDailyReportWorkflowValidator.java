package com.famora.business.validator;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.DailyReportStatus;
import com.famora.common.exception.BusinessDailyReportAccessDeniedException;
import com.famora.common.exception.BusinessDailyReportWorkflowException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BusinessDailyReportWorkflowValidator {
  
  private static final Map<DailyReportStatus, Set<DailyReportStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          DailyReportStatus.DRAFT,
          Set.of(DailyReportStatus.SUBMITTED),
          
          DailyReportStatus.SUBMITTED,
          Set.of(
              DailyReportStatus.REVISION_REQUESTED,
              DailyReportStatus.APPROVED,
              DailyReportStatus.REJECTED,
              DailyReportStatus.VOIDED
          ),
          
          DailyReportStatus.REVISION_REQUESTED,
          Set.of(
              DailyReportStatus.SUBMITTED,
              DailyReportStatus.REJECTED,
              DailyReportStatus.VOIDED
          ),
          
          DailyReportStatus.APPROVED,
          Set.of(DailyReportStatus.VOIDED),
          
          DailyReportStatus.REJECTED,
          Set.of(DailyReportStatus.VOIDED),
          
          DailyReportStatus.VOIDED,
          Set.of()
      );
  
  public void validateTransition(
      DailyReportStatus oldStatus,
      DailyReportStatus newStatus
  ) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(newStatus)) {
      throw new BusinessDailyReportWorkflowException(
          "Invalid daily report status transition from " + oldStatus + " to " + newStatus
      );
    }
  }
  
  public void ensureReportCanBeUpdated(
      BusinessDailyReport report,
      BusinessRole role,
      UUID currentUserId
  ) {
    DailyReportStatus status = getReportStatus(report);
    
    if (status == DailyReportStatus.REJECTED) {
      throw new BusinessDailyReportWorkflowException("Rejected report cannot be edited.");
    }
    
    if (role == BusinessRole.STAFF) {
      ensureStaffOwnsReport(report, currentUserId);
      
      boolean editableStatus = status == DailyReportStatus.DRAFT
          || status == DailyReportStatus.REVISION_REQUESTED;
      
      if (!editableStatus) {
        throw new BusinessDailyReportAccessDeniedException(
            "Staff can edit only their own draft or revision requested report."
        );
      }
    }
  }
  
  private static DailyReportStatus getReportStatus(BusinessDailyReport report) {
    DailyReportStatus status = report.getReportStatus();
    
    if (status == DailyReportStatus.APPROVED) {
      throw new BusinessDailyReportWorkflowException("Approved report cannot be edited.");
    }
    
    if (status == DailyReportStatus.VOIDED) {
      throw new BusinessDailyReportWorkflowException("Voided report cannot be edited.");
    }
    
    if (status == DailyReportStatus.SUBMITTED) {
      throw new BusinessDailyReportWorkflowException(
          "Submitted report cannot be edited directly. Please request revision."
      );
    }
    return status;
  }
  
  public void ensureCanSubmit(
      BusinessDailyReport report,
      BusinessRole role,
      UUID currentUserId
  ) {
    DailyReportStatus status = report.getReportStatus();
    
    if (status != DailyReportStatus.DRAFT
        && status != DailyReportStatus.REVISION_REQUESTED) {
      throw new BusinessDailyReportWorkflowException(
          "Only draft or revision requested report can be submitted."
      );
    }
    
    if (role == BusinessRole.STAFF) {
      ensureStaffOwnsReport(report, currentUserId);
    }
  }
  
  public void ensureCanRequestRevision(BusinessDailyReport report, BusinessRole role) {
    ensureOwnerOrPartner(role, "Only owner or partner can request report revision.");
    
    if (report.getReportStatus() != DailyReportStatus.SUBMITTED) {
      throw new BusinessDailyReportWorkflowException(
          "Only submitted report can be requested for revision.");
    }
  }
  
  public void ensureCanApprove(BusinessDailyReport report, BusinessRole role) {
    ensureOwnerOrPartner(role, "Only owner or partner can approve report.");
    
    if (report.getReportStatus() != DailyReportStatus.SUBMITTED) {
      throw new BusinessDailyReportWorkflowException("Only submitted report can be approved.");
    }
  }
  
  public void ensureCanReject(BusinessDailyReport report, BusinessRole role) {
    ensureOwnerOrPartner(role, "Only owner or partner can reject report.");
    
    DailyReportStatus status = report.getReportStatus();
    if (status != DailyReportStatus.SUBMITTED
        && status != DailyReportStatus.REVISION_REQUESTED) {
      throw new BusinessDailyReportWorkflowException(
          "Only submitted or revision requested report can be rejected."
      );
    }
  }
  
  public void ensureCanVoid(BusinessDailyReport report, BusinessRole role) {
    ensureOwnerOrPartner(role, "Only owner or partner can void report.");
    
    DailyReportStatus status = report.getReportStatus();
    boolean canVoid = status == DailyReportStatus.SUBMITTED
        || status == DailyReportStatus.APPROVED
        || status == DailyReportStatus.REJECTED
        || status == DailyReportStatus.REVISION_REQUESTED;
    
    if (!canVoid) {
      throw new BusinessDailyReportWorkflowException("This report status cannot be voided.");
    }
  }
  
  public void ensureCanViewRevision(
      BusinessDailyReport report,
      BusinessRole role,
      UUID currentUserId
  ) {
    if (role == BusinessRole.OWNER || role == BusinessRole.PARTNER) {
      return;
    }
    
    if (role == BusinessRole.STAFF) {
      ensureStaffOwnsReport(report, currentUserId);
      return;
    }
    
    if (role == BusinessRole.MANAGER) {
      return;
    }
    
    throw new BusinessDailyReportAccessDeniedException(
        "You do not have permission to view report revisions.");
  }
  
  private void ensureOwnerOrPartner(BusinessRole role, String message) {
    if (role != BusinessRole.OWNER && role != BusinessRole.PARTNER) {
      throw new BusinessDailyReportAccessDeniedException(message);
    }
  }
  
  private void ensureStaffOwnsReport(BusinessDailyReport report, UUID currentUserId) {
    if (!currentUserId.equals(report.getReportedByUserId())) {
      throw new BusinessDailyReportAccessDeniedException(
          "Staff can access only reports they created."
      );
    }
  }
}
