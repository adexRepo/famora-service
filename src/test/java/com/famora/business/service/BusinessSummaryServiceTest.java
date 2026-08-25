package com.famora.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.famora.business.dto.response.BusinessSummaryResponse;
import com.famora.business.repository.BusinessDailyLossItemRepository;
import com.famora.business.repository.BusinessDailyReportRepository;
import com.famora.business.repository.BusinessDailySalesItemRepository;
import com.famora.business.repository.BusinessExpenseRepository;
import com.famora.common.cache.TenantRedisCache;
import com.famora.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessSummaryServiceTest {

  @Mock
  private BusinessPermissionService permissionService;
  @Mock
  private CurrentUserProvider currentUserProvider;
  @Mock
  private BusinessDailyReportRepository reportRepository;
  @Mock
  private BusinessExpenseRepository expenseRepository;
  @Mock
  private BusinessDailySalesItemRepository salesItemRepository;
  @Mock
  private BusinessDailyLossItemRepository lossItemRepository;
  @Mock
  private TenantRedisCache tenantRedisCache;
  @InjectMocks
  private BusinessSummaryService service;

  @Test
  void cacheHitStillRequiresBusinessPermission() {
    UUID businessId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    LocalDate fromDate = LocalDate.of(2026, 8, 1);
    LocalDate toDate = LocalDate.of(2026, 8, 25);
    BusinessSummaryResponse response = new BusinessSummaryResponse(
        businessId, fromDate, toDate,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
        BigDecimal.TEN);
    when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    when(tenantRedisCache.findBusinessSummary(businessId, fromDate, toDate))
        .thenReturn(TenantRedisCache.CacheLookup.hit(4L, response));

    BusinessSummaryResponse result = service.summarize(businessId, fromDate, toDate);

    assertThat(result).isSameAs(response);
    verify(permissionService).requireCanView(businessId, userId);
    verifyNoInteractions(reportRepository, expenseRepository, salesItemRepository,
        lossItemRepository);
  }
}
