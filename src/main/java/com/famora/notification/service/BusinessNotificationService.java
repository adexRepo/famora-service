package com.famora.notification.service;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.common.helper.Status;
import com.famora.notification.dto.CreateNotificationCommand;
import com.famora.notification.enums.NotificationEntityType;
import com.famora.notification.enums.NotificationType;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessNotificationService {
  
  private static final Set<BusinessRole> DAILY_REPORT_REVIEWER_ROLES = Set.of(
      BusinessRole.OWNER, BusinessRole.PARTNER);
  
  private final BusinessMemberRepository businessMemberRepository;
  private final NotificationService notificationService;
  
  public void dailyReportSubmitted(BusinessDailyReport report) {
    List<UUID> recipients = businessMemberRepository
        .findByBusinessIdAndStatus(report.getBusiness().getId(), Status.ACTIVE).stream()
        .filter(member -> DAILY_REPORT_REVIEWER_ROLES.contains(member.getRole()))
        .map(BusinessMember::getUserId)
        .toList();
    
    notificationService.notifyUsers(recipients, command(
        NotificationType.BUSINESS_DAILY_REPORT_SUBMITTED,
        "Daily report submitted",
        "A daily report is ready for review.",
        report));
  }
  
  public void dailyReportApproved(BusinessDailyReport report) {
    notificationService.notifyUsers(List.of(report.getReportedByUserId()), command(
        NotificationType.BUSINESS_DAILY_REPORT_APPROVED,
        "Daily report approved",
        "Your daily report has been approved.",
        report));
  }
  
  public void dailyReportRejected(BusinessDailyReport report) {
    notificationService.notifyUsers(List.of(report.getReportedByUserId()), command(
        NotificationType.BUSINESS_DAILY_REPORT_REJECTED,
        "Daily report rejected",
        "Your daily report has been rejected.",
        report));
  }
  
  public void dailyReportRevisionRequested(BusinessDailyReport report) {
    notificationService.notifyUsers(List.of(report.getReportedByUserId()), command(
        NotificationType.BUSINESS_DAILY_REPORT_REVISION_REQUESTED,
        "Daily report revision requested",
        "Your daily report needs revision.",
        report));
  }
  
  private CreateNotificationCommand command(NotificationType type, String title, String message,
      BusinessDailyReport report) {
    return new CreateNotificationCommand(
        type,
        title,
        message,
        TrackerScopeType.BUSINESS,
        null,
        report.getBusiness(),
        TrackerSourceModule.BUSINESS,
        NotificationEntityType.BUSINESS_DAILY_REPORT,
        report.getId(),
        null,
        Map.of(
            "businessId", report.getBusiness().getId(),
            "reportId", report.getId(),
            "reportDate", report.getReportDate(),
            "shift", report.getShift(),
            "reportStatus", report.getReportStatus()
        )
    );
  }
}
