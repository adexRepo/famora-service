package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.AMOUNT;
import static com.famora.business.constant.BusinessAuditConstants.EXPENSE;
import static com.famora.business.constant.BusinessAuditConstants.EXPENSE_NAME;
import static com.famora.business.constant.BusinessAuditConstants.STATUS;

import com.famora.audit.entity.AuditAction;
import com.famora.business.dto.request.CreateExpenseRequest;
import com.famora.business.dto.request.UpdateExpenseRequest;
import com.famora.business.dto.response.ExpenseResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessExpense;
import com.famora.business.enums.PaymentMethod;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessExpenseRepository;
import com.famora.business.spec.BusinessExpenseSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.MoneyUtil;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessExpenseService {
  
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessExpenseRepository expenseRepository;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional
  public ExpenseResponse create(UUID businessId, CreateExpenseRequest req) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageExpense(businessId, user.getId());
    MoneyUtil.requireNonNegative(req.amount(), "amount");
    if (req.quantity() != null) {
      MoneyUtil.requirePositive(req.quantity(), "quantity");
    }
    
    Business business = permissionService.requireActiveBusiness(businessId);
    
    BusinessExpense expense = buildBusinessExpense(req, business, user);
    
    expense = expenseRepository.save(expense);
    publishExpenseAudit(user, businessId, AuditAction.BUSINESS_EXPENSE_CREATED, expense);
    return BusinessMapper.expense(expense);
  }
  
  private static @NotNull BusinessExpense buildBusinessExpense(CreateExpenseRequest req,
      Business business, User user) {
    BusinessExpense expense = new BusinessExpense();
    expense.setBusiness(business);
    expense.setDailyReportId(null); // manual expense outside daily report
    expense.setExpenseDate(req.expenseDate());
    expense.setExpenseName(req.expenseName().trim());
    expense.setCategory(req.category());
    expense.setQuantity(req.quantity() == null ? null : MoneyUtil.nvl(req.quantity()));
    expense.setUnit(req.unit());
    expense.setAmount(MoneyUtil.nvl(req.amount()));
    expense.setPaymentMethod(
        req.paymentMethod() == null ? PaymentMethod.CASH : req.paymentMethod());
    expense.setNotes(req.notes());
    expense.setCreatedBy(user);
    return expense;
  }
  
  @Transactional(readOnly = true)
  public Page<ExpenseResponse> list(UUID businessId, Pageable pageable) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return expenseRepository.findAll(
        BusinessExpenseSpecifications.belongsToBusiness(businessId)
            .and(BusinessExpenseSpecifications.statusNot(Status.DELETED)),
        pageable).map(BusinessMapper::expense);
  }
  
  @Transactional(readOnly = true)
  public ExpenseResponse get(UUID businessId, UUID expenseId) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return BusinessMapper.expense(requireExpense(businessId, expenseId));
  }
  
  @Transactional
  public ExpenseResponse update(UUID businessId, UUID expenseId, UpdateExpenseRequest req) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageExpense(businessId, user.getId());
    BusinessExpense expense = requireExpense(businessId, expenseId);
    rejectDailyReportExpenseMutation(expense);
    
    if (req.expenseDate() != null) {
      expense.setExpenseDate(req.expenseDate());
    }
    if (req.expenseName() != null && !req.expenseName().isBlank()) {
      expense.setExpenseName(req.expenseName().trim());
    }
    if (req.category() != null) {
      expense.setCategory(req.category());
    }
    if (req.quantity() != null) {
      MoneyUtil.requirePositive(req.quantity(), "quantity");
      expense.setQuantity(MoneyUtil.nvl(req.quantity()));
    }
    if (req.unit() != null) {
      expense.setUnit(req.unit());
    }
    if (req.amount() != null) {
      MoneyUtil.requireNonNegative(req.amount(), "amount");
      expense.setAmount(MoneyUtil.nvl(req.amount()));
    }
    if (req.paymentMethod() != null) {
      expense.setPaymentMethod(req.paymentMethod());
    }
    if (req.notes() != null) {
      expense.setNotes(req.notes());
    }
    expense.setUpdatedBy(user);
    
    expense = expenseRepository.save(expense);
    publishExpenseAudit(user, businessId, AuditAction.BUSINESS_EXPENSE_UPDATED, expense);
    return BusinessMapper.expense(expense);
  }
  
  @Transactional
  public void delete(UUID businessId, UUID expenseId) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageExpense(businessId, user.getId());
    BusinessExpense expense = requireExpense(businessId, expenseId);
    rejectDailyReportExpenseMutation(expense);
    expense.setStatus(Status.DELETED);
    expense.setUpdatedBy(user);
    expenseRepository.save(expense);
    publishExpenseAudit(user, businessId, AuditAction.BUSINESS_EXPENSE_DELETED, expense);
  }
  
  private void publishExpenseAudit(User user, UUID businessId, AuditAction action,
      BusinessExpense expense) {
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        action,
        EXPENSE,
        expense.getId(),
        Map.of(
            EXPENSE_NAME, expense.getExpenseName(),
            AMOUNT, expense.getAmount(),
            STATUS, expense.getStatus()
        )
    );
  }
  
  private BusinessExpense requireExpense(UUID businessId, UUID expenseId) {
    return expenseRepository.findByIdAndBusinessIdAndStatusNot(expenseId, businessId,
        Status.DELETED).orElseThrow(() -> BusinessException.notFound("Expense not found"));
  }
  
  private void rejectDailyReportExpenseMutation(BusinessExpense expense) {
    if (expense.getDailyReportId() != null) {
      throw BusinessException.validation(
          "Expense linked to daily report cannot be modified from manual expense endpoint");
    }
  }
}
