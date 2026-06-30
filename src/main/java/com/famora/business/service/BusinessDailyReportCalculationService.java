package com.famora.business.service;

import com.famora.business.entity.BusinessDailyLossItem;
import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailySalesItem;
import com.famora.business.entity.BusinessExpense;
import com.famora.business.enums.PaymentMethod;
import com.famora.business.repository.BusinessDailyLossItemRepository;
import com.famora.business.repository.BusinessDailyPaymentBreakdownRepository;
import com.famora.business.repository.BusinessDailySalesItemRepository;
import com.famora.business.repository.BusinessExpenseRepository;
import com.famora.common.helper.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete replacement for the adapter interface that was generated in the workflow enhancement.
 * <p>
 * Use this if your existing MVP 3A module does not already have
 * BusinessDailyReportCalculationService.
 * <p>
 * Important: - This service recalculates totals from persisted child rows. - It does not trust
 * frontend totals. - Submit validation enforces total sales == payment breakdown total. - Draft
 * calculation may skip payment validation because draft can be incomplete.
 * <p>
 * Adjust package names and exception type to match your project.
 */
@Service
@RequiredArgsConstructor
public class BusinessDailyReportCalculationService {
  
  private static final BigDecimal ZERO = BigDecimal.ZERO;
  
  private final BusinessDailySalesItemRepository salesItemRepository;
  private final BusinessDailyPaymentBreakdownRepository paymentBreakdownRepository;
  private final BusinessDailyLossItemRepository lossItemRepository;
  private final BusinessExpenseRepository expenseRepository;
  
  @Transactional(propagation = Propagation.MANDATORY)
  public void recalculateDraftTotals(BusinessDailyReport report) {
    recalculate(report, false);
  }
  
  @Transactional(propagation = Propagation.MANDATORY)
  public void recalculateAndValidateForSubmit(BusinessDailyReport report) {
    recalculate(report, true);
  }
  
  private void recalculate(BusinessDailyReport report, boolean validatePaymentMatch) {
    List<BusinessDailySalesItem> salesItems = salesItemRepository.findByDailyReportId(report.getId());
    List<BusinessDailyPaymentBreakdown> payments = paymentBreakdownRepository.findByDailyReportId(report.getId());
    List<BusinessDailyLossItem> lossItems = lossItemRepository.findByDailyReportId(report.getId());
    List<BusinessExpense> expenses = expenseRepository.findByDailyReportIdAndStatus(report.getId(), Status.ACTIVE);
    
    BigDecimal totalSales = salesItems.stream()
        .map(this::resolveSalesItemTotal)
        .reduce(ZERO, BigDecimal::add);
    
    BigDecimal paymentTotal = payments.stream()
        .map(BusinessDailyPaymentBreakdown::getAmount)
        .filter(Objects::nonNull)
        .reduce(ZERO, BigDecimal::add);
    
    if (validatePaymentMatch) {
      if (totalSales.signum() > 0 && payments.isEmpty()) {
        throw new IllegalArgumentException(
            "Payment breakdown is required when total sales is greater than zero.");
      }
      
      if (totalSales.compareTo(paymentTotal) != 0) {
        throw new IllegalArgumentException("Total payment breakdown must equal total sales.");
      }
    }
    
    BigDecimal totalCashSales = sumPayment(payments, PaymentMethod.CASH);
    BigDecimal totalQrisSales = sumPayment(payments, PaymentMethod.QRIS);
    BigDecimal totalTransferSales = sumPayment(payments, PaymentMethod.BANK_TRANSFER);
    BigDecimal totalOtherSales = paymentTotal
        .subtract(totalCashSales)
        .subtract(totalQrisSales)
        .subtract(totalTransferSales);
    
    BigDecimal totalExpense = expenses.stream()
        .map(BusinessExpense::getAmount)
        .filter(Objects::nonNull)
        .reduce(ZERO, BigDecimal::add);
    
    BigDecimal totalCashExpense = expenses.stream()
        .filter(expense -> expense.getPaymentMethod() == PaymentMethod.CASH)
        .map(BusinessExpense::getAmount)
        .filter(Objects::nonNull)
        .reduce(ZERO, BigDecimal::add);
    
    BigDecimal totalNonCashExpense = totalExpense.subtract(totalCashExpense);
    
    BigDecimal totalLoss = lossItems.stream()
        .map(this::resolveLossItemTotal)
        .reduce(ZERO, BigDecimal::add);
    
    BigDecimal dailyCapital = nvl(report.getDailyCapitalAmount());
    BigDecimal expectedCash = dailyCapital
        .add(totalCashSales)
        .subtract(totalCashExpense);
    
    BigDecimal netOperating = totalSales.subtract(totalExpense);
    
    report.setTotalSalesAmount(totalSales);
    report.setTotalCashSalesAmount(totalCashSales);
    report.setTotalQrisSalesAmount(totalQrisSales);
    report.setTotalTransferSalesAmount(totalTransferSales);
    report.setTotalOtherSalesAmount(totalOtherSales);
    
    report.setTotalExpenseAmount(totalExpense);
    report.setTotalCashExpenseAmount(totalCashExpense);
    report.setTotalNonCashExpenseAmount(totalNonCashExpense);
    
    report.setTotalLossAmount(totalLoss);
    report.setExpectedCashAmount(expectedCash);
    report.setNetOperatingAmount(netOperating);
  }
  
  private BigDecimal sumPayment(List<BusinessDailyPaymentBreakdown> payments,
      PaymentMethod paymentMethod) {
    return payments.stream()
        .filter(payment -> payment.getPaymentMethod() == paymentMethod)
        .map(BusinessDailyPaymentBreakdown::getAmount)
        .filter(Objects::nonNull)
        .reduce(ZERO, BigDecimal::add);
  }
  
  private BigDecimal resolveSalesItemTotal(BusinessDailySalesItem item) {
    if (item.getTotalAmount() != null) {
      return item.getTotalAmount();
    }
    return nvl(item.getQuantitySold()).multiply(nvl(item.getUnitPrice()));
  }
  
  private BigDecimal resolveLossItemTotal(BusinessDailyLossItem item) {
    if (item.getEstimatedTotalValue() != null) {
      return item.getEstimatedTotalValue();
    }
    return nvl(item.getQuantityLoss()).multiply(nvl(item.getEstimatedUnitValue()));
  }
  
  private BigDecimal nvl(BigDecimal value) {
    return value == null ? ZERO : value;
  }
}
