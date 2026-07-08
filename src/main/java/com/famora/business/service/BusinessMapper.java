package com.famora.business.service;

import com.famora.business.dto.response.BusinessInvitationResponse;
import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.dto.response.BusinessProductResponse;
import com.famora.business.dto.response.BusinessResponse;
import com.famora.business.dto.response.DailyReportDetailResponse;
import com.famora.business.dto.response.DailyReportPhotoResponse;
import com.famora.business.dto.response.DailyReportSummaryResponse;
import com.famora.business.dto.response.ExpenseResponse;
import com.famora.business.dto.response.LossItemResponse;
import com.famora.business.dto.response.PaymentBreakdownResponse;
import com.famora.business.dto.response.SalesItemResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessDailyLossItem;
import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailySalesItem;
import com.famora.business.entity.BusinessExpense;
import com.famora.business.entity.BusinessInvitation;
import com.famora.business.entity.BusinessMember;
import com.famora.business.entity.BusinessProduct;
import com.famora.business.enums.BusinessRole;
import java.util.List;

public final class BusinessMapper {
  
  private BusinessMapper() {
  }
  
  public static BusinessResponse business(Business b) {
    return business(b, false);
  }
  
  public static BusinessResponse business(Business b, boolean isDefault) {
    return new BusinessResponse(b.getId(), b.getName(), b.getBusinessType(), b.getDefaultCurrency(),
        b.getOwnerUserId(),
        b.getPrimaryFamilyId(), b.getDescription(), b.getAddress(), b.getContact(), b.getStatus(),
        isDefault, b.getCreatedAt(), b.getUpdatedAt());
  }
  
  public static BusinessMemberResponse member(BusinessMember m) {
    return new BusinessMemberResponse(m.getId(), m.getBusiness().getId(), m.getUserId(),
        m.getRole(),
        m.getStatus(), m.isDefaultBusiness(), m.getInvitedByUserId(), m.getJoinedAt());
  }
  
  public static BusinessInvitationResponse invitation(BusinessInvitation i) {
    return new BusinessInvitationResponse(i.getId(), i.getBusiness().getId(), i.getInvitedEmail(),
        i.getInvitedPhone(), i.getRole(), i.getInvitationCode(),
        i.getInvitationStatus(), i.getExpiresAt(), i.getInvitedByUserId(), i.getAcceptedByUserId());
  }
  
  public static BusinessProductResponse product(BusinessProduct p, BusinessRole role) {
    return new BusinessProductResponse(p.getId(), p.getBusiness().getId(), p.getProductName(),
        p.getCategory(), p.getUnit(),
        p.getDefaultSellingPrice(), role.canSeeCostPrice() ? p.getCostPrice() : null, p.getStatus(),
        p.getCreatedAt(), p.getUpdatedAt());
  }
  
  public static DailyReportSummaryResponse reportSummary(BusinessDailyReport r) {
    return new DailyReportSummaryResponse(r.getId(), r.getBusiness().getId(), r.getReportDate(),
        r.getShift(), r.getCurrency(), r.getReportedByUserId(),
        r.getDailyCapitalAmount(), r.getTotalSalesAmount(), r.getTotalCashSalesAmount(),
        r.getTotalQrisSalesAmount(),
        r.getTotalTransferSalesAmount(), r.getTotalOtherSalesAmount(), r.getTotalExpenseAmount(),
        r.getTotalCashExpenseAmount(),
        r.getTotalNonCashExpenseAmount(), r.getTotalLossAmount(), r.getExpectedCashAmount(),
        r.getNetOperatingAmount(), r.getReportStatus(),
        r.getNotes(), r.getCreatedAt(), r.getUpdatedAt());
  }
  
  public static SalesItemResponse salesItem(BusinessDailySalesItem i) {
    return new SalesItemResponse(i.getId(), i.getProductId(), i.getItemName(), i.getUnit(),
        i.getQuantitySold(), i.getUnitPrice(), i.getTotalAmount(), i.getNotes());
  }
  
  public static PaymentBreakdownResponse payment(BusinessDailyPaymentBreakdown p) {
    return new PaymentBreakdownResponse(p.getId(), p.getPaymentMethod(), p.getAmount(),
        p.getNotes());
  }
  
  public static LossItemResponse loss(BusinessDailyLossItem l) {
    return new LossItemResponse(l.getId(), l.getItemName(), l.getUnit(), l.getQuantityLoss(),
        l.getEstimatedUnitValue(), l.getEstimatedTotalValue(), l.getReason(), l.getNotes());
  }
  
  public static ExpenseResponse expense(BusinessExpense e) {
    return new ExpenseResponse(e.getId(), e.getBusiness().getId(), e.getDailyReportId(),
        e.getExpenseDate(), e.getExpenseName(), e.getCategory(),
        e.getQuantity(), e.getUnit(), e.getAmount(), e.getPaymentMethod(), e.getNotes(),
        e.getStatus(), e.getCreatedBy().getId(), e.getCreatedAt(), e.getUpdatedAt());
  }
  
  public static DailyReportDetailResponse reportDetail(BusinessDailyReport r,
      List<BusinessDailySalesItem> sales,
      List<BusinessDailyPaymentBreakdown> payments,
      List<BusinessDailyLossItem> losses,
      List<BusinessExpense> expenses,
      List<DailyReportPhotoResponse> photos) {
    return new DailyReportDetailResponse(reportSummary(r),
        sales.stream().map(BusinessMapper::salesItem).toList(),
        payments.stream().map(BusinessMapper::payment).toList(),
        losses.stream().map(BusinessMapper::loss).toList(),
        expenses.stream().map(BusinessMapper::expense).toList(),
        photos);
  }
}
