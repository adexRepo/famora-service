package com.famora.business.service;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailyReportRevision;
import com.famora.business.enums.BusinessDailyReportRevisionChangeType;
import com.famora.business.enums.DailyReportStatus;
import com.famora.business.repository.BusinessDailyReportRevisionRepository;
import com.famora.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessDailyReportRevisionService {

  private final BusinessDailyReportRevisionRepository revisionRepository;
  private final BusinessDailyReportSnapshotService snapshotService;

  @Transactional
  public BusinessDailyReportRevision createRevision(
      BusinessDailyReport report,
      BusinessDailyReportRevisionChangeType changeType,
      DailyReportStatus oldStatus,
      DailyReportStatus newStatus,
      User changedBy,
      String reason
  ) {
    int nextRevisionNumber = revisionRepository
        .findMaxRevisionNumber(report.getId())
        .orElse(0) + 1;

    String snapshotJson = snapshotService.buildSnapshotJson(report);

    BusinessDailyReportRevision revision = BusinessDailyReportRevision.builder()
        .business(report.getBusiness())
        .dailyReport(report)
        .revisionNumber(nextRevisionNumber)
        .changeType(changeType)
        .oldStatus(oldStatus)
        .newStatus(newStatus)
        .changedByUserId(changedBy.getId())
        .changeReason(reason)
        .snapshotJson(snapshotJson)
        .createdBy(changedBy)
        .build();

    return revisionRepository.save(revision);
  }
}
