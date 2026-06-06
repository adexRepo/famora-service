package com.famora.finance.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.family.entity.Family;
import com.famora.finance.dto.CreateFinanceTransactionRequest;
import com.famora.finance.dto.FinanceSummaryResponse;
import com.famora.finance.dto.FinanceTransactionResponse;
import com.famora.finance.dto.UpdateFinanceTransactionRequest;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.repository.FinanceTransactionRepository;
import com.famora.security.CurrentUserService;
import com.famora.security.FamilyContextService;
import com.famora.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {
  
  private final FinanceTransactionRepository financeTransactionRepository;
  private final CurrentUserService currentUserService;
  private final FamilyContextService familyContextService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public FinanceTransactionResponse create(CreateFinanceTransactionRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    FinanceTransaction transaction = FinanceTransaction.builder()
        .family(family)
        .type(request.type())
        .amount(request.amount())
        .currency(request.currency().trim().toUpperCase())
        .category(request.category().trim())
        .description(clean(request.description()))
        .transactionDate(request.transactionDate())
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
    
    return toResponse(transaction);
  }
  
  @Transactional(readOnly = true)
  public List<FinanceTransactionResponse> list(
      String month,
      FinanceTransactionType type,
      String category
  ) {
    Family family = familyContextService.getCurrentFamily();
    
    YearMonth yearMonth = parseMonthOrCurrent(month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    
    String cleanCategory = clean(category);
    
    List<FinanceTransaction> transactions;
    
    if (type == null && cleanCategory == null) {
      transactions = financeTransactionRepository
          .findByFamilyIdAndDeletedAtIsNullAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
              family.getId(),
              startDate,
              endDate
          );
    } else if (type != null && cleanCategory == null) {
      transactions = financeTransactionRepository
          .findByFamilyIdAndDeletedAtIsNullAndTransactionDateBetweenAndTypeOrderByTransactionDateDescCreatedAtDesc(
              family.getId(),
              startDate,
              endDate,
              type
          );
    } else if (type == null) {
      transactions = financeTransactionRepository
          .findByFamilyIdAndDeletedAtIsNullAndTransactionDateBetweenAndCategoryIgnoreCaseOrderByTransactionDateDescCreatedAtDesc(
              family.getId(),
              startDate,
              endDate,
              cleanCategory
          );
    } else {
      transactions = financeTransactionRepository
          .findByFamilyIdAndDeletedAtIsNullAndTransactionDateBetweenAndTypeAndCategoryIgnoreCaseOrderByTransactionDateDescCreatedAtDesc(
              family.getId(),
              startDate,
              endDate,
              type,
              cleanCategory
          );
    }
    
    return transactions.stream()
        .map(this::toResponse)
        .toList();
  }
  
  @Transactional(readOnly = true)
  public FinanceTransactionResponse getDetail(UUID id) {
    Family family = familyContextService.getCurrentFamily();
    
    FinanceTransaction transaction = financeTransactionRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Finance transaction not found"));
    
    return toResponse(transaction);
  }
  
  @Transactional
  public FinanceTransactionResponse update(UUID id, UpdateFinanceTransactionRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    FinanceTransaction transaction = financeTransactionRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Finance transaction not found"));
    
    transaction.setType(request.type());
    transaction.setAmount(request.amount());
    transaction.setCurrency(request.currency().trim().toUpperCase());
    transaction.setCategory(request.category().trim());
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
  public void delete(UUID id) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    FinanceTransaction transaction = financeTransactionRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Finance transaction not found"));
    
    transaction.setDeletedAt(OffsetDateTime.now());
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
  public FinanceSummaryResponse getSummary(String month, String currency) {
    Family family = familyContextService.getCurrentFamily();
    
    YearMonth yearMonth = parseMonthOrCurrent(month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    
    String selectedCurrency = currency == null || currency.isBlank()
        ? "MYR"
        : currency.trim().toUpperCase();
    
    BigDecimal totalIncome = financeTransactionRepository.sumAmountByType(
        family.getId(),
        startDate,
        endDate,
        FinanceTransactionType.INCOME,
        selectedCurrency
    );
    
    BigDecimal totalExpense = financeTransactionRepository.sumAmountByType(
        family.getId(),
        startDate,
        endDate,
        FinanceTransactionType.EXPENSE,
        selectedCurrency
    );
    
    if (totalIncome == null) {
      totalIncome = BigDecimal.ZERO;
    }
    
    if (totalExpense == null) {
      totalExpense = BigDecimal.ZERO;
    }
    
    return new FinanceSummaryResponse(
        yearMonth,
        selectedCurrency,
        totalIncome,
        totalExpense,
        totalIncome.subtract(totalExpense)
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
}
