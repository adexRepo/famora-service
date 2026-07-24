package com.famora.finance.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.file.entity.FileAsset;
import com.famora.file.service.FileService;
import com.famora.finance.dto.FinanceDebtDtos.CreateDebtPaymentRequest;
import com.famora.finance.dto.FinanceDebtDtos.CreateDebtRequest;
import com.famora.finance.dto.FinanceDebtDtos.DebtDetailResponse;
import com.famora.finance.dto.FinanceDebtDtos.DebtListResponse;
import com.famora.finance.dto.FinanceDebtDtos.DebtPaymentResponse;
import com.famora.finance.dto.FinanceDebtDtos.UpdateDebtRequest;
import com.famora.finance.entity.FinanceDebt;
import com.famora.finance.entity.FinanceDebtPayment;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.helper.FinanceCategoryCode;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import com.famora.finance.repository.FinanceDebtPaymentRepository;
import com.famora.finance.repository.FinanceDebtRepository;
import com.famora.finance.spec.FinanceDebtSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FinanceDebtService {
  
  private final FinanceDebtRepository debtRepository;
  private final FinanceDebtPaymentRepository paymentRepository;
  private final FinanceService financeService;
  private final FileService fileService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public DebtDetailResponse create(FamilyContext ctx, CreateDebtRequest request,
      MultipartFile attachment) {
    FileAsset attachmentFile = uploadAttachment(ctx, attachment, "Finance debt attachment");
    
    FinanceDebt debt = FinanceDebt.builder()
        .family(ctx.family())
        .createdBy(ctx.user())
        .debtType(request.type())
        .debtStatus(FinanceDebtStatus.OPEN)
        .counterpartyName(cleanRequired(request.counterpartyName(), "counterpartyName"))
        .principalAmount(scale(request.principalAmount()))
        .paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        .remainingAmount(scale(request.principalAmount()))
        .currency(financeService.normalizeCurrency(request.currency()))
        .borrowedDate(request.borrowedDate())
        .dueDate(request.dueDate())
        .notes(clean(request.notes()))
        .attachmentFile(attachmentFile)
        .build();
    
    debtRepository.save(debt);
    
    FinanceTransaction principalTransaction = createPrincipalTransaction(ctx, debt);
    debt.setPrincipalFinanceTransaction(principalTransaction);
    debtRepository.save(debt);
    
    auditLogService.log(ctx.family(), ctx.user(), AuditAction.FINANCE_DEBT_CREATED,
        "finance_debts", debt.getId(), "{\"debtId\":\"" + debt.getId() + "\"}");
    
    return toDetail(debt);
  }
  
  @Transactional(readOnly = true)
  public Page<DebtListResponse> list(FamilyContext ctx, FinanceDebtType type,
      FinanceDebtStatus status, String keyword, Pageable pageable) {
    Specification<FinanceDebt> spec = Specification
        .where(FinanceDebtSpecifications.family(ctx.family().getId()))
        .and(FinanceDebtSpecifications.status(Status.ACTIVE))
        .and(FinanceDebtSpecifications.debtType(type))
        .and(FinanceDebtSpecifications.debtStatus(status))
        .and(FinanceDebtSpecifications.excludeCancelledWhenNoStatus(status))
        .and(FinanceDebtSpecifications.keyword(keyword));
    
    return debtRepository.findAll(spec, pageable).map(DebtListResponse::from);
  }
  
  @Transactional(readOnly = true)
  public DebtDetailResponse getDetail(FamilyContext ctx, UUID id) {
    return toDetail(getActiveDebt(ctx, id));
  }
  
  @Transactional
  public DebtDetailResponse update(FamilyContext ctx, UUID id, UpdateDebtRequest request) {
    FinanceDebt debt = getActiveDebt(ctx, id);
    assertNotCancelled(debt);
    
    debt.setCounterpartyName(cleanRequired(request.counterpartyName(), "counterpartyName"));
    debt.setDueDate(request.dueDate());
    debt.setNotes(clean(request.notes()));
    debt.setUpdatedBy(ctx.user());
    debtRepository.save(debt);
    
    auditLogService.log(ctx.family(), ctx.user(), AuditAction.FINANCE_DEBT_UPDATED,
        "finance_debts", debt.getId(), "{\"debtId\":\"" + debt.getId() + "\"}");
    
    return toDetail(debt);
  }
  
  @Transactional
  public void cancel(FamilyContext ctx, UUID id) {
    FinanceDebt debt = getActiveDebt(ctx, id);
    if (debt.getDebtStatus() == FinanceDebtStatus.CANCELLED) {
      return;
    }
    
    for (FinanceDebtPayment payment : activePayments(debt)) {
      payment.setStatus(Status.DELETED);
      payment.setUpdatedBy(ctx.user());
      paymentRepository.save(payment);
      financeService.deleteSystemTransaction(ctx, payment.getFinanceTransaction());
    }
    
    financeService.deleteSystemTransaction(ctx, debt.getPrincipalFinanceTransaction());
    debt.setDebtStatus(FinanceDebtStatus.CANCELLED);
    debt.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    debt.setRemainingAmount(debt.getPrincipalAmount());
    debt.setUpdatedBy(ctx.user());
    debtRepository.save(debt);
    
    auditLogService.log(ctx.family(), ctx.user(), AuditAction.FINANCE_DEBT_CANCELLED,
        "finance_debts", debt.getId(), "{\"debtId\":\"" + debt.getId() + "\"}");
  }
  
  @Transactional
  public DebtPaymentResponse addPayment(FamilyContext ctx, UUID debtId,
      CreateDebtPaymentRequest request, MultipartFile attachment) {
    FinanceDebt debt = getActiveDebt(ctx, debtId);
    assertNotCancelled(debt);
    if (debt.getDebtStatus() == FinanceDebtStatus.PAID) {
      throw new AppException(HttpStatus.CONFLICT, "Debt is already paid");
    }
    
    BigDecimal amount = scale(request.amount());
    if (amount.compareTo(debt.getRemainingAmount()) > 0) {
      throw new AppException(HttpStatus.BAD_REQUEST,
          "Payment amount cannot exceed remaining amount");
    }
    
    FileAsset attachmentFile = uploadAttachment(ctx, attachment, "Finance debt payment attachment");
    FinanceDebtPayment payment = FinanceDebtPayment.builder()
        .family(ctx.family())
        .createdBy(ctx.user())
        .debt(debt)
        .amount(amount)
        .paymentDate(request.paymentDate())
        .notes(clean(request.notes()))
        .attachmentFile(attachmentFile)
        .build();
    
    paymentRepository.save(payment);
    FinanceTransaction transaction = createPaymentTransaction(ctx, debt, payment);
    payment.setFinanceTransaction(transaction);
    paymentRepository.save(payment);
    
    recalculateDebt(ctx, debt);
    
    auditLogService.log(ctx.family(), ctx.user(), AuditAction.FINANCE_DEBT_PAYMENT_CREATED,
        "finance_debt_payments", payment.getId(),
        "{\"debtId\":\"" + debt.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}");
    
    return DebtPaymentResponse.from(payment);
  }
  
  @Transactional
  public void deletePayment(FamilyContext ctx, UUID debtId, UUID paymentId) {
    FinanceDebt debt = getActiveDebt(ctx, debtId);
    assertNotCancelled(debt);
    
    FinanceDebtPayment payment = paymentRepository
        .findByIdAndDebtIdAndFamilyIdAndStatus(paymentId, debtId, ctx.family().getId(),
            Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Debt payment not found"));
    
    payment.setStatus(Status.DELETED);
    payment.setUpdatedBy(ctx.user());
    paymentRepository.save(payment);
    financeService.deleteSystemTransaction(ctx, payment.getFinanceTransaction());
    recalculateDebt(ctx, debt);
    
    auditLogService.log(ctx.family(), ctx.user(), AuditAction.FINANCE_DEBT_PAYMENT_DELETED,
        "finance_debt_payments", payment.getId(),
        "{\"debtId\":\"" + debt.getId() + "\",\"paymentId\":\"" + payment.getId() + "\"}");
  }
  
  private DebtDetailResponse toDetail(FinanceDebt debt) {
    return DebtDetailResponse.from(debt, activePayments(debt));
  }
  
  private List<FinanceDebtPayment> activePayments(FinanceDebt debt) {
    return paymentRepository.findAllByDebtIdAndStatusOrderByPaymentDateAscCreatedAtAsc(
        debt.getId(), Status.ACTIVE);
  }
  
  private FinanceDebt getActiveDebt(FamilyContext ctx, UUID id) {
    return debtRepository.findByIdAndFamilyIdAndStatus(id, ctx.family().getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Finance debt not found"));
  }
  
  private FinanceTransaction createPrincipalTransaction(FamilyContext ctx, FinanceDebt debt) {
    FinanceTransactionType type = debt.getDebtType() == FinanceDebtType.RECEIVABLE
        ? FinanceTransactionType.EXPENSE
        : FinanceTransactionType.INCOME;
    String category = debt.getDebtType() == FinanceDebtType.RECEIVABLE
        ? FinanceCategoryCode.RECEIVABLE_DISBURSEMENT
        : FinanceCategoryCode.DEBT_RECEIVED;
    
    return financeService.createTransaction(ctx, type, debt.getPrincipalAmount(),
        debt.getCurrency(), category, debt.getCounterpartyName(), debt.getBorrowedDate());
  }
  
  private FinanceTransaction createPaymentTransaction(FamilyContext ctx, FinanceDebt debt,
      FinanceDebtPayment payment) {
    FinanceTransactionType type = debt.getDebtType() == FinanceDebtType.RECEIVABLE
        ? FinanceTransactionType.INCOME
        : FinanceTransactionType.EXPENSE;
    String category = debt.getDebtType() == FinanceDebtType.RECEIVABLE
        ? FinanceCategoryCode.RECEIVABLE_REPAYMENT
        : FinanceCategoryCode.DEBT_REPAYMENT;
    
    return financeService.createTransaction(ctx, type, payment.getAmount(), debt.getCurrency(),
        category, debt.getCounterpartyName(), payment.getPaymentDate());
  }
  
  private void recalculateDebt(FamilyContext ctx, FinanceDebt debt) {
    BigDecimal paid = scale(paymentRepository.sumActivePayments(debt.getId()));
    BigDecimal remaining = debt.getPrincipalAmount().subtract(paid)
        .max(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP);
    
    debt.setPaidAmount(paid);
    debt.setRemainingAmount(remaining);
    debt.setDebtStatus(resolveDebtStatus(debt.getPrincipalAmount(), paid, remaining));
    debt.setUpdatedBy(ctx.user());
    debtRepository.save(debt);
  }
  
  private FinanceDebtStatus resolveDebtStatus(BigDecimal principal, BigDecimal paid,
      BigDecimal remaining) {
    if (remaining.compareTo(BigDecimal.ZERO) == 0) {
      return FinanceDebtStatus.PAID;
    }
    if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(principal) < 0) {
      return FinanceDebtStatus.PARTIAL;
    }
    return FinanceDebtStatus.OPEN;
  }
  
  private void assertNotCancelled(FinanceDebt debt) {
    if (debt.getDebtStatus() == FinanceDebtStatus.CANCELLED) {
      throw new AppException(HttpStatus.CONFLICT, "Debt is cancelled");
    }
  }
  
  private FileAsset uploadAttachment(FamilyContext ctx, MultipartFile attachment, String notes) {
    if (attachment == null || attachment.isEmpty()) {
      return null;
    }
    String contentType = attachment.getContentType();
    if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
      throw new AppException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          "Finance debt attachment must be an image");
    }
    return fileService.upload(attachment, "FINANCE_DEBT", notes, Visibility.FAMILY, ctx,
        "finance");
  }
  
  private String cleanRequired(String value, String field) {
    String clean = clean(value);
    if (clean == null) {
      throw new AppException(HttpStatus.BAD_REQUEST, field + " is required");
    }
    return clean;
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
  
  private BigDecimal scale(BigDecimal amount) {
    return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
  }
}
