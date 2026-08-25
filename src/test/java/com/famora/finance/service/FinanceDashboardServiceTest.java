package com.famora.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;

import com.famora.common.helper.Status;
import com.famora.common.cache.TenantRedisCache;
import com.famora.currency.service.CurrencyConversionService;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.finance.dto.FinanceDashboardDtos.CumulativeChartResponse;
import com.famora.finance.dto.FinanceDashboardDtos.AllocationResponse;
import com.famora.finance.dto.FinanceDashboardDtos.DashboardResponse;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.repository.FinanceDebtRepository;
import com.famora.finance.repository.FinanceTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceDashboardServiceTest {

  @Mock
  private FinanceTransactionRepository transactionRepository;
  @Mock
  private FinanceDebtRepository debtRepository;
  @Mock
  private FinanceService financeService;
  @Mock
  private CurrencyConversionService currencyConversionService;
  @Mock
  private TenantRedisCache tenantRedisCache;
  @InjectMocks
  private FinanceDashboardService service;

  @BeforeEach
  void disableRedisCache() {
    when(tenantRedisCache.findFinanceDashboard(any(), anyString(), any()))
        .thenReturn(TenantRedisCache.CacheLookup.disabled());
  }

  @Test
  void dashboardReturnsComparableCashFlowAmountsAndSavingsRate() {
    UUID familyId = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    List<FinanceTransaction> transactions = List.of(
        transaction(FinanceTransactionType.INCOME, "1000.00", today.minusDays(6)),
        transaction(FinanceTransactionType.EXPENSE, "200.00", today.minusDays(6)),
        transaction(FinanceTransactionType.INCOME, "500.00", today),
        transaction(FinanceTransactionType.EXPENSE, "400.00", today)
    );
    FamilyContext context = familyContext(familyId);

    when(financeService.normalizeCurrency("IDR")).thenReturn("IDR");
    when(transactionRepository
        .findAllByFamilyIdAndStatusOrderByTransactionDateAscCreatedAtAsc(familyId, Status.ACTIVE))
        .thenReturn(transactions);
    when(debtRepository.findAllByFamilyIdAndStatus(familyId, Status.ACTIVE))
        .thenReturn(List.of());

    CumulativeChartResponse cashFlow = service.dashboard(context, "IDR")
        .cumulativeTransactionChart().get("1W");

    assertThat(cashFlow.totalIncome()).isEqualByComparingTo("1500.00");
    assertThat(cashFlow.totalExpense()).isEqualByComparingTo("600.00");
    assertThat(cashFlow.netAmount()).isEqualByComparingTo("900.00");
    assertThat(cashFlow.savingsRatePercent()).isEqualByComparingTo("60.00");
    assertThat(cashFlow.points().getFirst().incomeAmount()).isEqualByComparingTo("1000.00");
    assertThat(cashFlow.points().getFirst().expenseAmount()).isEqualByComparingTo("200.00");
    assertThat(cashFlow.points().getLast().incomeAmount()).isEqualByComparingTo("1500.00");
    assertThat(cashFlow.points().getLast().expenseAmount()).isEqualByComparingTo("600.00");
  }

  @Test
  void dashboardLeavesSavingsRateUndefinedWhenThereIsNoIncome() {
    UUID familyId = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    FamilyContext context = familyContext(familyId);

    when(financeService.normalizeCurrency("IDR")).thenReturn("IDR");
    when(transactionRepository
        .findAllByFamilyIdAndStatusOrderByTransactionDateAscCreatedAtAsc(familyId, Status.ACTIVE))
        .thenReturn(List.of(transaction(FinanceTransactionType.EXPENSE, "100.00", today)));
    when(debtRepository.findAllByFamilyIdAndStatus(familyId, Status.ACTIVE))
        .thenReturn(List.of());

    CumulativeChartResponse cashFlow = service.dashboard(context, "IDR")
        .cumulativeTransactionChart().get("1M");

    assertThat(cashFlow.netAmount()).isEqualByComparingTo("-100.00");
    assertThat(cashFlow.savingsRatePercent()).isNull();
  }

  @Test
  void dashboardReturnsTenantScopedCachedResponseWithoutQueryingRepositories() {
    UUID familyId = UUID.randomUUID();
    FamilyContext context = familyContext(familyId);
    DashboardResponse response = new DashboardResponse(
        "IDR", BigDecimal.TEN, Map.of(), Map.of(),
        new AllocationResponse(BigDecimal.TEN, List.of()));
    when(financeService.normalizeCurrency("IDR")).thenReturn("IDR");
    when(tenantRedisCache.findFinanceDashboard(familyId, "IDR", LocalDate.now()))
        .thenReturn(TenantRedisCache.CacheLookup.hit(2L, response));

    DashboardResponse result = service.dashboard(context, "IDR");

    assertThat(result).isSameAs(response);
    verifyNoInteractions(transactionRepository, debtRepository, currencyConversionService);
  }

  private FamilyContext familyContext(UUID familyId) {
    Family family = org.mockito.Mockito.mock(Family.class);
    when(family.getId()).thenReturn(familyId);
    return new FamilyContext(family, null, null, true);
  }

  private FinanceTransaction transaction(FinanceTransactionType type, String amount,
      LocalDate date) {
    return FinanceTransaction.builder()
        .type(type)
        .amount(new BigDecimal(amount))
        .currency("IDR")
        .category("OTHER")
        .transactionDate(date)
        .build();
  }
}
