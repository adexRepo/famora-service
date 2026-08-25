package com.famora.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.famora.common.config.EnvironmentNamespaceProperties;
import com.famora.finance.dto.FinanceDashboardDtos.AllocationResponse;
import com.famora.finance.dto.FinanceDashboardDtos.DashboardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class TenantRedisCacheTest {

  @Mock
  private StringRedisTemplate redisTemplate;
  @Mock
  private HashOperations<String, Object, Object> hashOperations;
  @Mock
  private ValueOperations<String, String> valueOperations;

  private TenantRedisCache cache;

  @BeforeEach
  void setUp() {
    cache = new TenantRedisCache(
        redisTemplate,
        new ObjectMapper().registerModule(new JavaTimeModule()),
        new EnvironmentNamespaceProperties("vsit"),
        new RedisCacheProperties(true, Duration.ofMinutes(2), Duration.ofMinutes(3))
    );
  }

  @Test
  void usesEnvironmentAndTenantInFinanceCacheKey() {
    UUID familyId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("vsit:famora:cache-version:finance-dashboard:" + familyId))
        .thenReturn(null);

    TenantRedisCache.CacheLookup<DashboardResponse> lookup =
        cache.findFinanceDashboard(familyId, "IDR", date);

    assertThat(lookup.hit()).isFalse();
    assertThat(lookup.cacheEnabled()).isTrue();
    verify(hashOperations).get(
        "vsit:famora:cache:finance-dashboard:" + familyId + ":v0",
        "IDR:2026-08-25"
    );
  }

  @Test
  void writesToTheVersionObservedBeforeCalculation() {
    UUID familyId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);
    DashboardResponse response = new DashboardResponse(
        "IDR", BigDecimal.ZERO, Map.of(), Map.of(),
        new AllocationResponse(BigDecimal.ZERO, List.of()));
    TenantRedisCache.CacheLookup<DashboardResponse> lookup =
        TenantRedisCache.CacheLookup.miss(7L);
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);

    cache.putFinanceDashboard(familyId, "IDR", date, lookup, response);

    String key = "vsit:famora:cache:finance-dashboard:" + familyId + ":v7";
    verify(hashOperations).put(org.mockito.ArgumentMatchers.eq(key),
        org.mockito.ArgumentMatchers.eq("IDR:2026-08-25"),
        org.mockito.ArgumentMatchers.contains("\"currency\":\"IDR\""));
    verify(redisTemplate).expire(key, Duration.ofMinutes(2));
  }

  @Test
  void invalidationAdvancesTenantGeneration() {
    UUID familyId = UUID.randomUUID();
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    cache.invalidateFinanceDashboardAfterCommit(familyId);

    verify(valueOperations).increment(
        "vsit:famora:cache-version:finance-dashboard:" + familyId);
  }

  @Test
  void invalidationWaitsUntilDatabaseCommit() {
    UUID familyId = UUID.randomUUID();
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      cache.invalidateFinanceDashboardAfterCommit(familyId);

      verifyNoInteractions(valueOperations);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(synchronization -> synchronization.afterCommit());

      verify(valueOperations).increment(
          "vsit:famora:cache-version:finance-dashboard:" + familyId);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(false);
    }
  }
}
