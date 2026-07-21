package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.DAILY_REPORT;
import static com.famora.business.constant.BusinessAuditConstants.REPORT_DATE;
import static com.famora.business.constant.BusinessAuditConstants.SHIFT;

import com.famora.audit.entity.AuditAction;
import com.famora.business.constant.BusinessDefaults;
import com.famora.business.dto.request.SubmitDailyReportRequest;
import com.famora.business.dto.response.DailyReportDetailResponse;
import com.famora.business.dto.response.DailyReportSummaryResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessDailyLossItem;
import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailySalesItem;
import com.famora.business.entity.BusinessExpense;
import com.famora.business.entity.BusinessMember;
import com.famora.business.entity.BusinessProduct;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.DailyReportStatus;
import com.famora.business.enums.LossReason;
import com.famora.business.enums.PaymentMethod;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessDailyLossItemRepository;
import com.famora.business.repository.BusinessDailyPaymentBreakdownRepository;
import com.famora.business.repository.BusinessDailyReportPhotoRepository;
import com.famora.business.repository.BusinessDailyReportRepository;
import com.famora.business.repository.BusinessDailySalesItemRepository;
import com.famora.business.repository.BusinessExpenseRepository;
import com.famora.business.repository.BusinessProductRepository;
import com.famora.business.spec.BusinessDailyReportSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.MoneyUtil;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessDailyReportService {
  
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessDailyReportRepository reportRepository;
  private final BusinessDailyReportPhotoRepository photoRepository;
  private final BusinessProductRepository productRepository;
  private final BusinessDailySalesItemRepository salesRepo;
  private final BusinessDailyPaymentBreakdownRepository paymentRepo;
  private final BusinessDailyLossItemRepository lossRepo;
  private final BusinessExpenseRepository expenseRepo;
  private final BusinessDailyReportCalculationService calculationService;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional
  public DailyReportSummaryResponse createDraft(UUID businessId, SubmitDailyReportRequest req) {
    User user = currentUserProvider.getCurrentUser();
    Business business = permissionService.requireActiveBusiness(businessId);
    BusinessMember member = permissionService.requireCanSubmitDailyReport(businessId, user.getId());
    String shift = blank(req.shift()) ? BusinessDefaults.SHIFT : req.shift().trim();
    
    if (reportRepository.existsByBusinessIdAndReportDateAndShiftAndReportStatusNot(businessId,
        req.reportDate(), shift, DailyReportStatus.VOIDED)) {
      throw BusinessException.conflict(
          "Daily report already exists for this business, date, and shift");
    }
    
    BusinessDailyReport report = new BusinessDailyReport();
    report.setBusiness(business);
    report.setReportDate(req.reportDate());
    report.setShift(shift);
    report.setCurrency(business.getDefaultCurrency());
    report.setReportedByUserId(user.getId());
    report.setDailyCapitalAmount(MoneyUtil.nvl(req.dailyCapitalAmount()));
    report.setDailyCapitalNote(req.dailyCapitalNote());
    report.setNotes(req.notes());
    report.setReportStatus(DailyReportStatus.DRAFT);
    report.setCreatedBy(user);
    
    BusinessDailyReport saved = reportRepository.saveAndFlush(report);
    
    List<BusinessDailySalesItem> sales = buildSales(business, saved.getId(), user,
        member.getRole(),
        req.salesItems());
    List<BusinessDailyPaymentBreakdown> payments = buildPayments(business, saved.getId(), user,
        req.paymentBreakdowns());
    List<BusinessDailyLossItem> losses = buildLosses(business, saved.getId(), user,
        req.lossItems());
    List<BusinessExpense> expenses = buildExpenses(business, saved.getId(), user, req.expenses());
    
    salesRepo.saveAll(sales);
    paymentRepo.saveAll(payments);
    lossRepo.saveAll(losses);
    expenseRepo.saveAll(expenses);
    calculationService.recalculateDraftTotals(saved);
    saved = reportRepository.save(saved);
    
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_CREATED,
        DAILY_REPORT,
        saved.getId(),
        Map.of(REPORT_DATE, saved.getReportDate(), SHIFT, saved.getShift())
    );
    
    return BusinessMapper.reportSummary(saved);
  }
  
  @Transactional
  public DailyReportSummaryResponse updateDraft(UUID businessId, UUID reportId,
      SubmitDailyReportRequest req) {
    User user = currentUserProvider.getCurrentUser();
    Business business = permissionService.requireActiveBusiness(businessId);
    BusinessMember member = permissionService.requireCanSubmitDailyReport(businessId, user.getId());
    BusinessDailyReport report = requireReport(businessId, reportId);
    
    if (report.getReportStatus() != DailyReportStatus.DRAFT
        && report.getReportStatus() != DailyReportStatus.REVISION_REQUESTED) {
      throw BusinessException.validation(
          "Only DRAFT or REVISION_REQUESTED daily report can be updated");
    }
    
    String shift = blank(req.shift()) ? BusinessDefaults.SHIFT : req.shift().trim();
    if ((!report.getReportDate().equals(req.reportDate()) || !report.getShift().equals(shift))
        && reportRepository.existsByBusinessIdAndReportDateAndShiftAndReportStatusNot(businessId,
        req.reportDate(), shift, DailyReportStatus.VOIDED)) {
      throw BusinessException.conflict(
          "Daily report already exists for this business, date, and shift");
    }
    
    report.setReportDate(req.reportDate());
    report.setShift(shift);
    report.setCurrency(business.getDefaultCurrency());
    report.setDailyCapitalAmount(MoneyUtil.nvl(req.dailyCapitalAmount()));
    report.setDailyCapitalNote(req.dailyCapitalNote());
    report.setNotes(req.notes());
    report.setUpdatedBy(user);
    
    softDeleteChildren(reportId, user);
    
    List<BusinessDailySalesItem> sales = buildSales(business, reportId, user, member.getRole(),
        req.salesItems());
    List<BusinessDailyPaymentBreakdown> payments = buildPayments(business, reportId, user,
        req.paymentBreakdowns());
    List<BusinessDailyLossItem> losses = buildLosses(business, reportId, user, req.lossItems());
    List<BusinessExpense> expenses = buildExpenses(business, reportId, user, req.expenses());
    
    salesRepo.saveAll(sales);
    paymentRepo.saveAll(payments);
    lossRepo.saveAll(losses);
    expenseRepo.saveAll(expenses);
    calculationService.recalculateDraftTotals(report);
    BusinessDailyReport saved = reportRepository.save(report);
    
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        AuditAction.BUSINESS_DAILY_REPORT_UPDATED,
        DAILY_REPORT,
        saved.getId(),
        Map.of(REPORT_DATE, saved.getReportDate(), SHIFT, saved.getShift())
    );
    
    return BusinessMapper.reportSummary(saved);
  }
  
  @Transactional(readOnly = true)
  public Page<DailyReportSummaryResponse> list(UUID businessId, Pageable pageable) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return reportRepository.findAll(
        BusinessDailyReportSpecifications.belongsToBusiness(businessId)
            .and(BusinessDailyReportSpecifications.reportStatusNot(DailyReportStatus.VOIDED)),
        pageable).map(BusinessMapper::reportSummary);
  }
  
  @Transactional(readOnly = true)
  public DailyReportDetailResponse get(UUID businessId, UUID reportId) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    BusinessDailyReport report = requireReport(businessId, reportId);
    return BusinessMapper.reportDetail(report,
        salesRepo.findByDailyReportIdAndStatus(reportId, Status.ACTIVE),
        paymentRepo.findByDailyReportIdAndStatus(reportId, Status.ACTIVE),
        lossRepo.findByDailyReportIdAndStatus(reportId, Status.ACTIVE),
        expenseRepo.findByDailyReportIdAndStatus(reportId, Status.ACTIVE),
        photoRepository
            .findByBusiness_IdAndDailyReport_IdAndStatusOrderByCreatedAtAsc(businessId, reportId,
                Status.ACTIVE)
            .stream()
            .map(com.famora.business.dto.response.DailyReportPhotoResponse::from)
            .toList());
  }
  
  private BusinessDailyReport requireReport(UUID businessId, UUID reportId) {
    return reportRepository.findByIdAndBusinessIdAndReportStatusNot(reportId, businessId,
            DailyReportStatus.VOIDED)
        .orElseThrow(() -> BusinessException.notFound("Daily report not found"));
  }
  
  private List<BusinessDailySalesItem> buildSales(Business business, UUID reportId,
      User user, BusinessRole role, List<SubmitDailyReportRequest.SalesItemRequest> reqs) {
    List<BusinessDailySalesItem> out = new ArrayList<>();
    for (SubmitDailyReportRequest.SalesItemRequest req : reqs) {
      MoneyUtil.requirePositive(req.quantitySold(), "quantitySold");
      if (role == BusinessRole.STAFF) {
        if (req.productId() == null) {
          throw BusinessException.validation("STAFF must provide productId");
        }
        if (!blank(req.itemName()) || !blank(req.unit()) || req.unitPrice() != null) {
          throw BusinessException.validation("STAFF cannot provide itemName, unit, or unitPrice");
        }
      }
      
      BusinessDailySalesItem item = new BusinessDailySalesItem();
      item.setBusiness(business);
      item.setDailyReportId(reportId);
      item.setCreatedBy(user);
      item.setQuantitySold(MoneyUtil.nvl(req.quantitySold()));
      item.setNotes(req.notes());
      
      if (req.productId() != null) {
        BusinessProduct p = productRepository.findByIdAndBusinessIdAndStatus(req.productId(),
            business.getId(), Status.ACTIVE).orElseThrow(() -> BusinessException.validation(
            "Product is not active or does not belong to this business"));
        item.setProductId(p.getId());
        item.setItemName(
            role.canCreateManualSalesItem() && !blank(req.itemName()) ? req.itemName().trim()
                : p.getProductName());
        item.setUnit(role.canCreateManualSalesItem() && !blank(req.unit()) ? req.unit().trim()
            : p.getUnit());
        item.setUnitPrice(
            role.canCreateManualSalesItem() && req.unitPrice() != null ? MoneyUtil.nvl(
                req.unitPrice()) : MoneyUtil.nvl(p.getDefaultSellingPrice()));
      } else {
        if (!role.canCreateManualSalesItem()) {
          throw BusinessException.validation("Only OWNER or PARTNER can create manual sales item");
        }
        if (blank(req.itemName()) || req.unitPrice() == null) {
          throw BusinessException.validation("Manual sales item requires itemName and unitPrice");
        }
        item.setItemName(req.itemName().trim());
        item.setUnit(blank(req.unit()) ? BusinessDefaults.UNIT : req.unit().trim());
        item.setUnitPrice(MoneyUtil.nvl(req.unitPrice()));
      }
      
      MoneyUtil.requireNonNegative(item.getUnitPrice(), "unitPrice");
      item.setTotalAmount(MoneyUtil.multiply(item.getQuantitySold(), item.getUnitPrice()));
      out.add(item);
    }
    return out;
  }
  
  private List<BusinessDailyPaymentBreakdown> buildPayments(Business business, UUID reportId,
      User user, List<SubmitDailyReportRequest.PaymentBreakdownRequest> reqs) {
    List<BusinessDailyPaymentBreakdown> out = new ArrayList<>();
    for (SubmitDailyReportRequest.PaymentBreakdownRequest req : reqs) {
      MoneyUtil.requireNonNegative(req.amount(), "payment amount");
      BusinessDailyPaymentBreakdown p = new BusinessDailyPaymentBreakdown();
      p.setBusiness(business);
      p.setDailyReportId(reportId);
      p.setCreatedBy(user);
      p.setPaymentMethod(req.paymentMethod());
      p.setAmount(MoneyUtil.nvl(req.amount()));
      p.setNotes(req.notes());
      out.add(p);
    }
    return out;
  }
  
  private List<BusinessDailyLossItem> buildLosses(Business business, UUID reportId,
      User user, List<SubmitDailyReportRequest.LossItemRequest> reqs) {
    if (reqs == null) {
      return List.of();
    }
    List<BusinessDailyLossItem> out = new ArrayList<>();
    for (SubmitDailyReportRequest.LossItemRequest req : reqs) {
      MoneyUtil.requirePositive(req.quantityLoss(), "quantityLoss");
      MoneyUtil.requireNonNegative(req.estimatedUnitValue(), "estimatedUnitValue");
      BusinessDailyLossItem l = new BusinessDailyLossItem();
      l.setBusiness(business);
      l.setDailyReportId(reportId);
      l.setCreatedBy(user);
      l.setItemName(req.itemName().trim());
      l.setUnit(blank(req.unit()) ? BusinessDefaults.UNIT : req.unit().trim());
      l.setQuantityLoss(MoneyUtil.nvl(req.quantityLoss()));
      l.setEstimatedUnitValue(MoneyUtil.nvl(req.estimatedUnitValue()));
      l.setEstimatedTotalValue(MoneyUtil.multiply(l.getQuantityLoss(), l.getEstimatedUnitValue()));
      l.setReason(req.reason() == null ? LossReason.UNSOLD : req.reason());
      l.setNotes(req.notes());
      out.add(l);
    }
    return out;
  }
  
  private List<BusinessExpense> buildExpenses(Business business, UUID reportId, User user,
      List<SubmitDailyReportRequest.ExpenseItemRequest> reqs) {
    if (reqs == null) {
      return List.of();
    }
    List<BusinessExpense> out = new ArrayList<>();
    for (SubmitDailyReportRequest.ExpenseItemRequest req : reqs) {
      MoneyUtil.requireNonNegative(req.amount(), "expense amount");
      if (req.quantity() != null) {
        MoneyUtil.requirePositive(req.quantity(), "expense quantity");
      }
      BusinessExpense e = new BusinessExpense();
      e.setBusiness(business);
      e.setDailyReportId(reportId);
      e.setCreatedBy(user);
      e.setExpenseDate(req.expenseDate());
      e.setExpenseName(req.expenseName().trim());
      e.setCategory(req.category());
      e.setQuantity(req.quantity() == null ? null : MoneyUtil.nvl(req.quantity()));
      e.setUnit(req.unit());
      e.setAmount(MoneyUtil.nvl(req.amount()));
      e.setPaymentMethod(req.paymentMethod() == null ? PaymentMethod.CASH : req.paymentMethod());
      e.setNotes(req.notes());
      out.add(e);
    }
    return out;
  }
  
  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
  
  private void softDeleteChildren(UUID reportId, User user) {
    List<BusinessDailySalesItem> sales = salesRepo.findByDailyReportIdAndStatus(reportId,
        Status.ACTIVE);
    sales.forEach(item -> {
      item.setStatus(Status.DELETED);
      item.setUpdatedBy(user);
    });
    salesRepo.saveAll(sales);
    
    List<BusinessDailyPaymentBreakdown> payments = paymentRepo.findByDailyReportIdAndStatus(
        reportId, Status.ACTIVE);
    payments.forEach(item -> {
      item.setStatus(Status.DELETED);
      item.setUpdatedBy(user);
    });
    paymentRepo.saveAll(payments);
    
    List<BusinessDailyLossItem> losses = lossRepo.findByDailyReportIdAndStatus(reportId,
        Status.ACTIVE);
    losses.forEach(item -> {
      item.setStatus(Status.DELETED);
      item.setUpdatedBy(user);
    });
    lossRepo.saveAll(losses);
    
    List<BusinessExpense> expenses = expenseRepo.findByDailyReportIdAndStatus(reportId,
        Status.ACTIVE);
    expenses.forEach(item -> {
      item.setStatus(Status.DELETED);
      item.setUpdatedBy(user);
    });
    expenseRepo.saveAll(expenses);
  }
}
