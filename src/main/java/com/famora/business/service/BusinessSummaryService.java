package com.famora.business.service;

import com.famora.business.dto.response.BusinessSummaryResponse;
import com.famora.business.dto.response.BusinessDashboardSummaryResponse;
import com.famora.business.dto.response.BusinessPeriodSummaryResponse;
import com.famora.business.dto.response.CashFlowResponse;
import com.famora.business.dto.response.ExpenseCategorySummaryResponse;
import com.famora.business.dto.response.LossSummaryResponse;
import com.famora.business.dto.response.TopSalesItemResponse;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessExpense;
import com.famora.business.enums.DailyReportStatus;
import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.LossReason;
import com.famora.business.enums.PaymentMethod;
import com.famora.business.enums.BusinessSummaryPeriod;
import com.famora.business.repository.BusinessDailyLossItemRepository;
import com.famora.business.repository.BusinessDailyReportRepository;
import com.famora.business.repository.BusinessDailySalesItemRepository;
import com.famora.business.repository.BusinessExpenseRepository;
import com.famora.business.spec.BusinessDailyReportSpecifications;
import com.famora.business.spec.BusinessExpenseSpecifications;
import com.famora.common.helper.MoneyUtil;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessSummaryService {
  
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessDailyReportRepository reportRepository;
  private final BusinessExpenseRepository expenseRepository;
  private final BusinessDailySalesItemRepository salesItemRepository;
  private final BusinessDailyLossItemRepository lossItemRepository;
  
  @Value("${app.business.dashboard.time-zone:Asia/Makassar}")
  private String dashboardTimeZone;
  
  @Transactional(readOnly = true)
  public BusinessSummaryResponse daily(UUID businessId, LocalDate date) {
    return summarize(businessId, date, date);
  }
  
  @Transactional(readOnly = true)
  public BusinessSummaryResponse monthly(UUID businessId, YearMonth month) {
    return summarize(businessId, month.atDay(1), month.atEndOfMonth());
  }
  
  @Transactional(readOnly = true)
  public BusinessDashboardSummaryResponse dashboardPresets(UUID businessId) {
    ZoneId zoneId = ZoneId.of(dashboardTimeZone);
    LocalDate today = LocalDate.now(zoneId);
    return dashboardPresets(businessId, today);
  }
  
  @Transactional(readOnly = true)
  public BusinessDashboardSummaryResponse dashboardPresets(UUID businessId, LocalDate today) {
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    LocalDate monthStart = today.withDayOfMonth(1);
    LocalDate yearStart = today.withDayOfYear(1);
    LocalDate last30DaysStart = today.minusDays(29);
    
    return new BusinessDashboardSummaryResponse(businessId, dashboardTimeZone, today, List.of(
        periodSummary(businessId, BusinessSummaryPeriod.TODAY, today, today),
        periodSummary(businessId, BusinessSummaryPeriod.THIS_WEEK, weekStart, today),
        periodSummary(businessId, BusinessSummaryPeriod.THIS_MONTH, monthStart, today),
        periodSummary(businessId, BusinessSummaryPeriod.THIS_YEAR, yearStart, today),
        periodSummary(businessId, BusinessSummaryPeriod.LAST_30_DAYS, last30DaysStart, today)
    ));
  }
  
  @Transactional(readOnly = true)
  public BusinessDashboardSummaryResponse dashboardCustom(UUID businessId, LocalDate fromDate,
      LocalDate toDate) {
    return new BusinessDashboardSummaryResponse(businessId, dashboardTimeZone, toDate, List.of(
        periodSummary(businessId, BusinessSummaryPeriod.CUSTOM, fromDate, toDate)
    ));
  }
  
  @Transactional(readOnly = true)
  public BusinessSummaryResponse summarize(UUID businessId, LocalDate fromDate, LocalDate toDate) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    List<BusinessDailyReport> reports = reportRepository
        .findAll(BusinessDailyReportSpecifications.belongsToBusiness(businessId)
            .and(BusinessDailyReportSpecifications.reportDateBetween(fromDate, toDate))
            .and(BusinessDailyReportSpecifications.reportStatusNot(DailyReportStatus.VOIDED)));
    List<BusinessExpense> manualExpenses = expenseRepository
        .findAll(BusinessExpenseSpecifications.belongsToBusiness(businessId)
            .and(BusinessExpenseSpecifications.expenseDateBetween(fromDate, toDate))
            .and(BusinessExpenseSpecifications.status(Status.ACTIVE)))
        .stream()
        .filter(e -> e.getDailyReportId() == null)
        .toList();
    
    BigDecimal totalSales = sumReports(reports, BusinessDailyReport::getTotalSalesAmount);
    BigDecimal cashSales = sumReports(reports, BusinessDailyReport::getTotalCashSalesAmount);
    BigDecimal qrisSales = sumReports(reports, BusinessDailyReport::getTotalQrisSalesAmount);
    BigDecimal transferSales = sumReports(reports,
        BusinessDailyReport::getTotalTransferSalesAmount);
    BigDecimal otherSales = sumReports(reports, BusinessDailyReport::getTotalOtherSalesAmount);
    BigDecimal reportExpense = sumReports(reports, BusinessDailyReport::getTotalExpenseAmount);
    BigDecimal reportCashExpense = sumReports(reports,
        BusinessDailyReport::getTotalCashExpenseAmount);
    BigDecimal reportNonCashExpense = sumReports(reports,
        BusinessDailyReport::getTotalNonCashExpenseAmount);
    BigDecimal totalLoss = sumReports(reports, BusinessDailyReport::getTotalLossAmount);
    BigDecimal dailyCapital = sumReports(reports, BusinessDailyReport::getDailyCapitalAmount);
    
    BigDecimal manualExpense = manualExpenses.stream().map(BusinessExpense::getAmount)
        .reduce(MoneyUtil.ZERO, BigDecimal::add);
    BigDecimal manualCashExpense = manualExpenses.stream()
        .filter(e -> e.getPaymentMethod() == PaymentMethod.CASH)
        .map(BusinessExpense::getAmount).reduce(MoneyUtil.ZERO, BigDecimal::add);
    BigDecimal manualNonCashExpense = manualExpense.subtract(manualCashExpense);
    
    BigDecimal totalExpense = reportExpense.add(manualExpense);
    BigDecimal totalCashExpense = reportCashExpense.add(manualCashExpense);
    BigDecimal totalNonCashExpense = reportNonCashExpense.add(manualNonCashExpense);
    BigDecimal netOperating = totalSales.subtract(totalExpense);
    BigDecimal expectedCash = dailyCapital.add(cashSales).subtract(totalCashExpense);
    
    return new BusinessSummaryResponse(businessId, fromDate, toDate,
        totalSales, cashSales, qrisSales, transferSales, otherSales,
        totalExpense, totalCashExpense, totalNonCashExpense,
        totalLoss, netOperating, expectedCash);
  }
  
  private BusinessPeriodSummaryResponse periodSummary(UUID businessId, BusinessSummaryPeriod period,
      LocalDate fromDate, LocalDate toDate) {
    return new BusinessPeriodSummaryResponse(period, fromDate, toDate,
        summarize(businessId, fromDate, toDate));
  }
  
  @Transactional(readOnly = true)
  public CashFlowResponse cashFlow(UUID businessId, LocalDate fromDate, LocalDate toDate) {
    BusinessSummaryResponse s = summarize(businessId, fromDate, toDate);
    BigDecimal dailyCapital = reportRepository
        .findAll(BusinessDailyReportSpecifications.belongsToBusiness(businessId)
            .and(BusinessDailyReportSpecifications.reportDateBetween(fromDate, toDate))
            .and(BusinessDailyReportSpecifications.reportStatusNot(DailyReportStatus.VOIDED)))
        .stream().map(BusinessDailyReport::getDailyCapitalAmount)
        .reduce(MoneyUtil.ZERO, BigDecimal::add);
    BigDecimal cashIn = dailyCapital.add(s.totalCashSalesAmount());
    BigDecimal cashOut = s.totalCashExpenseAmount();
    return new CashFlowResponse(businessId, fromDate, toDate,
        cashIn, cashOut, cashIn.subtract(cashOut), s.totalSalesAmount(), s.totalExpenseAmount());
  }
  
  @Transactional(readOnly = true)
  public List<TopSalesItemResponse> topSalesItems(UUID businessId, LocalDate fromDate,
      LocalDate toDate, int limit) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    int safeLimit = Math.max(1, Math.min(limit, 100));
    return salesItemRepository.findTopSalesItems(businessId, fromDate, toDate,
            PageRequest.of(0, safeLimit))
        .stream()
        .map(row -> new TopSalesItemResponse((String) row[0], bd(row[1]), bd(row[2])))
        .toList();
  }
  
  @Transactional(readOnly = true)
  public List<ExpenseCategorySummaryResponse> expenseByCategory(UUID businessId, LocalDate fromDate,
      LocalDate toDate) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return expenseRepository.summarizeByCategory(businessId, fromDate, toDate)
        .stream()
        .map(row -> new ExpenseCategorySummaryResponse((ExpenseCategory) row[0], bd(row[1])))
        .toList();
  }
  
  @Transactional(readOnly = true)
  public List<LossSummaryResponse> lossSummary(UUID businessId, LocalDate fromDate,
      LocalDate toDate) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return lossItemRepository.summarizeLoss(businessId, fromDate, toDate)
        .stream()
        .map(row -> new LossSummaryResponse((LossReason) row[0], bd(row[1]), bd(row[2])))
        .toList();
  }
  
  private BigDecimal sumReports(List<BusinessDailyReport> reports,
      java.util.function.Function<BusinessDailyReport, BigDecimal> getter) {
    return reports.stream().map(getter).filter(Objects::nonNull)
        .reduce(MoneyUtil.ZERO, BigDecimal::add);
  }
  
  private BigDecimal bd(Object value) {
    if (value == null) {
      return MoneyUtil.ZERO;
    }
    if (value instanceof BigDecimal b) {
      return b;
    }
    if (value instanceof Number n) {
      return BigDecimal.valueOf(n.doubleValue());
    }
    return new BigDecimal(value.toString());
  }
}
