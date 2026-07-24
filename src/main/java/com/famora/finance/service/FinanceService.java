package com.famora.finance.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.common.helper.Status;
import com.famora.currency.service.CurrencyConversionService;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.finance.dto.CreateFinanceTransactionRequest;
import com.famora.finance.dto.CurrencyAmountProjection;
import com.famora.finance.dto.FinanceSummaryResponse;
import com.famora.finance.dto.FinanceTransactionResponse;
import com.famora.finance.dto.UpdateFinanceTransactionRequest;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.helper.FinanceCategoryCode;
import com.famora.finance.repository.FinanceTransactionRepository;
import com.famora.finance.spec.FinanceTransactionSpecifications;
import com.famora.user.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {
  
  private final FinanceTransactionRepository financeTransactionRepository;
  private final AuditLogService auditLogService;
  private final CurrencyConversionService currencyConversionService;
  
  @Transactional
  public FinanceTransactionResponse create(FamilyContext ctx, CreateFinanceTransactionRequest request) {
    FinanceTransaction transaction = createTransaction(
        ctx,
        request.type(),
        request.amount(),
        request.currency(),
        request.category(),
        request.description(),
        request.transactionDate()
    );
    
    return toResponse(transaction);
  }
  
  @Transactional(readOnly = true)
  public Page<FinanceTransactionResponse> list(
      FamilyContext familyContext,
      String month,
      FinanceTransactionType type,
      String category,
      Pageable pageable
  ) {
    UUID familyId = familyContext.family().getId();
    
    YearMonth yearMonth = parseMonthOrCurrent(month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    
    String cleanCategory = clean(category);
    
    Specification<FinanceTransaction> spec = Specification
        .where(FinanceTransactionSpecifications.family(familyId))
        .and(FinanceTransactionSpecifications.status(Status.ACTIVE))
        .and(FinanceTransactionSpecifications.transactionDateBetween(startDate, endDate))
        .and(FinanceTransactionSpecifications.type(type))
        .and(FinanceTransactionSpecifications.category(cleanCategory));
    
    Page<FinanceTransaction> page = financeTransactionRepository.findAll(spec, pageable);
    
    return page.map(this::toResponse);
  }
  
  @Transactional(readOnly = true)
  public FinanceTransactionResponse getDetail(FamilyContext ctx, UUID id) {
    FinanceTransaction transaction = getFinanceTransaction(id, ctx.family());
    
    return toResponse(transaction);
  }
  
  private FinanceTransaction getFinanceTransaction(UUID id, Family family) {
    return financeTransactionRepository
        .findByIdAndFamilyIdAndStatus(id, family.getId(), Status.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Finance transaction not found"));
  }
  
  @Transactional
  public FinanceTransactionResponse update(FamilyContext ctx, UUID id,
      UpdateFinanceTransactionRequest request) {
    User user = ctx.user();
    Family family = ctx.family();
    
    FinanceTransaction transaction = getFinanceTransaction(id, family);
    
    transaction.setType(request.type());
    transaction.setAmount(request.amount());
    transaction.setCurrency(request.currency().trim().toUpperCase());
    transaction.setCategory(FinanceCategoryCode.normalizeAndValidate(request.type(),
        request.category()));
    transaction.setDescription(clean(request.description()));
    transaction.setTransactionDate(request.transactionDate());
    transaction.setUpdatedBy(user);
    
    financeTransactionRepository.save(transaction);
    
    auditLogService.log(
        family,
        user,
        AuditAction.FINANCE_TRANSACTION_UPDATED,
        "finance_transactions",
        transaction.getId(),
        null
    );
    
    return toResponse(transaction);
  }
  
  @Transactional
  public void delete(FamilyContext ctx, UUID id) {
    User user = ctx.user();
    Family family = ctx.family();
    
    FinanceTransaction transaction = getFinanceTransaction(id, family);
    
    transaction.setStatus(Status.DELETED);
    transaction.setUpdatedBy(user);
    
    financeTransactionRepository.save(transaction);
    
    auditLogService.log(
        family,
        user,
        AuditAction.FINANCE_TRANSACTION_DELETED,
        "finance_transactions",
        transaction.getId(),
        null
    );
  }
  
  @Transactional(readOnly = true)
  public FinanceSummaryResponse getSummary(FamilyContext ctx, String month, String currency) {
    Family family = ctx.family();
    YearMonth yearMonth = parseMonthOrCurrent(month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    
    String targetCurrency = normalizeCurrency(currency);
    
    BigDecimal totalIncome = calculateTotalByTargetCurrency(
        family.getId(),
        startDate,
        endDate,
        FinanceTransactionType.INCOME,
        targetCurrency
    );
    
    BigDecimal totalExpense = calculateTotalByTargetCurrency(
        family.getId(),
        startDate,
        endDate,
        FinanceTransactionType.EXPENSE,
        targetCurrency
    );
    
    BigDecimal balance = totalIncome.subtract(totalExpense)
        .setScale(2, RoundingMode.HALF_UP);
    
    return new FinanceSummaryResponse(
        yearMonth,
        targetCurrency,
        totalIncome,
        totalExpense,
        balance
    );
  }
  
  @Transactional
  public FinanceTransaction createTransaction(
      FamilyContext ctx,
      FinanceTransactionType type,
      BigDecimal amount,
      String currency,
      String category,
      String description,
      LocalDate transactionDate
  ) {
    User user = ctx.user();
    Family family = ctx.family();
    String normalizedCurrency = normalizeCurrency(currency);
    String normalizedCategory = FinanceCategoryCode.normalizeAndValidate(type, category);
    
    FinanceTransaction transaction = FinanceTransaction.builder()
        .family(family)
        .type(type)
        .amount(amount)
        .currency(normalizedCurrency)
        .category(normalizedCategory)
        .description(clean(description))
        .transactionDate(transactionDate)
        .createdBy(user)
        .build();
    
    financeTransactionRepository.save(transaction);
    
    auditLogService.log(
        family,
        user,
        AuditAction.FINANCE_TRANSACTION_CREATED,
        "finance_transactions",
        transaction.getId(),
        null
    );
    
    return transaction;
  }
  
  @Transactional
  public void deleteSystemTransaction(FamilyContext ctx, FinanceTransaction transaction) {
    if (transaction == null || transaction.getStatus() == Status.DELETED) {
      return;
    }
    
    transaction.setStatus(Status.DELETED);
    transaction.setUpdatedBy(ctx.user());
    financeTransactionRepository.save(transaction);
    
    auditLogService.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FINANCE_TRANSACTION_DELETED,
        "finance_transactions",
        transaction.getId(),
        null
    );
  }
  
  private FinanceTransactionResponse toResponse(FinanceTransaction transaction) {
    return new FinanceTransactionResponse(
        transaction.getId(),
        transaction.getType().name(),
        transaction.getAmount(),
        transaction.getCurrency(),
        transaction.getCategory(),
        transaction.getDescription(),
        transaction.getTransactionDate(),
        transaction.getCreatedAt(),
        transaction.getUpdatedAt()
    );
  }
  
  private YearMonth parseMonthOrCurrent(String month) {
    if (month == null || month.isBlank()) {
      return YearMonth.now();
    }
    
    return YearMonth.parse(month);
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
  
  public String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return "MYR";
    }
    
    return currency.trim().toUpperCase(Locale.ROOT);
  }
  
  private BigDecimal calculateTotalByTargetCurrency(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type,
      String targetCurrency
  ) {
    List<CurrencyAmountProjection> summaries =
        financeTransactionRepository.sumAmountByTypeGroupByCurrency(
            familyId,
            startDate,
            endDate,
            type
        );
    
    BigDecimal total = BigDecimal.ZERO;
    
    for (CurrencyAmountProjection summary : summaries) {
      String sourceCurrency = normalizeCurrency(summary.getCurrency());
      
      BigDecimal amount = summary.getTotalAmount() == null
          ? BigDecimal.ZERO
          : summary.getTotalAmount();
      
      BigDecimal amountInTargetCurrency;
      
      if (sourceCurrency.equals(targetCurrency)) {
        amountInTargetCurrency = amount;
      } else {
        amountInTargetCurrency = currencyConversionService.convert(
            sourceCurrency,
            targetCurrency,
            amount
        );
      }
      
      total = total.add(amountInTargetCurrency);
    }
    
    BigDecimal oriAmt = summaries.isEmpty() ? null : summaries.getFirst().getTotalAmount();
    
    log.debug("[Calculate] type: [{}], originalAmt: [{}], convertedAmt:[{}]", type.name(), oriAmt,
        total);
    
    return total.setScale(2, RoundingMode.UP);
  }
}
