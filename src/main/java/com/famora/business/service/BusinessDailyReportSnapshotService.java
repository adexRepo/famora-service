package com.famora.business.service;

import com.famora.business.dto.snapshot.DailyReportSnapshotDto;
import com.famora.business.dto.snapshot.DailyReportSnapshotExpenseDto;
import com.famora.business.dto.snapshot.DailyReportSnapshotHeaderDto;
import com.famora.business.dto.snapshot.DailyReportSnapshotLossItemDto;
import com.famora.business.dto.snapshot.DailyReportSnapshotPaymentBreakdownDto;
import com.famora.business.dto.snapshot.DailyReportSnapshotSalesItemDto;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.repository.BusinessDailyReportSnapshotQueryRepository;
import com.famora.common.exception.BusinessDailyReportWorkflowException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessDailyReportSnapshotService {

  private final ObjectMapper objectMapper;
  private final BusinessDailyReportSnapshotQueryRepository snapshotQueryRepository;

  @Transactional(readOnly = true)
  public String buildSnapshotJson(BusinessDailyReport report) {
    DailyReportSnapshotDto snapshot = new DailyReportSnapshotDto(
        DailyReportSnapshotHeaderDto.from(report),
        snapshotQueryRepository.findSalesItemsForSnapshot(report.getId())
            .stream()
            .map(DailyReportSnapshotSalesItemDto::from)
            .toList(),
        snapshotQueryRepository.findPaymentBreakdownsForSnapshot(report.getId())
            .stream()
            .map(DailyReportSnapshotPaymentBreakdownDto::from)
            .toList(),
        snapshotQueryRepository.findLossItemsForSnapshot(report.getId())
            .stream()
            .map(DailyReportSnapshotLossItemDto::from)
          .toList(),
        snapshotQueryRepository.findExpensesForSnapshot(report.getId())
            .stream()
            .map(DailyReportSnapshotExpenseDto::from)
            .toList()
    );

    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException exception) {
      throw new BusinessDailyReportWorkflowException("Failed to build daily report snapshot.");
    }
  }
}
