package com.famora.common.cache;

import com.famora.business.dto.response.BusinessSummaryResponse;
import com.famora.common.config.EnvironmentNamespaceProperties;
import com.famora.finance.dto.FinanceDashboardDtos.DashboardResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRedisCache {
  
  private static final String FINANCE_DASHBOARD = "finance-dashboard";
  private static final String BUSINESS_SUMMARY = "business-summary";
  
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final EnvironmentNamespaceProperties namespaceProperties;
  private final RedisCacheProperties properties;
  
  public CacheLookup<DashboardResponse> findFinanceDashboard(UUID familyId, String currency,
      LocalDate date) {
    return find(FINANCE_DASHBOARD, familyId, currency + ":" + date, DashboardResponse.class);
  }
  
  public void putFinanceDashboard(UUID familyId, String currency, LocalDate date,
      CacheLookup<DashboardResponse> lookup, DashboardResponse response) {
    put(FINANCE_DASHBOARD, familyId, currency + ":" + date, lookup, response,
        properties.financeDashboardTtl());
  }
  
  public CacheLookup<BusinessSummaryResponse> findBusinessSummary(UUID businessId,
      LocalDate fromDate, LocalDate toDate) {
    return find(BUSINESS_SUMMARY, businessId, fromDate + ":" + toDate,
        BusinessSummaryResponse.class);
  }
  
  public void putBusinessSummary(UUID businessId, LocalDate fromDate, LocalDate toDate,
      CacheLookup<BusinessSummaryResponse> lookup, BusinessSummaryResponse response) {
    put(BUSINESS_SUMMARY, businessId, fromDate + ":" + toDate, lookup, response,
        properties.businessSummaryTtl());
  }
  
  public void invalidateFinanceDashboardAfterCommit(UUID familyId) {
    afterCommit(() -> incrementVersion(FINANCE_DASHBOARD, familyId));
  }
  
  public void invalidateBusinessSummaryAfterCommit(UUID businessId) {
    afterCommit(() -> incrementVersion(BUSINESS_SUMMARY, businessId));
  }
  
  private <T> CacheLookup<T> find(String cacheName, UUID tenantId, String field,
      Class<T> responseType) {
    if (!properties.enabled()) {
      return CacheLookup.disabled();
    }
    try {
      long version = currentVersion(cacheName, tenantId);
      Object json = redisTemplate.opsForHash().get(dataKey(cacheName, tenantId, version), field);
      if (json == null) {
        return CacheLookup.miss(version);
      }
      return CacheLookup.hit(version, objectMapper.readValue(json.toString(), responseType));
    } catch (DataAccessException | JsonProcessingException | IllegalArgumentException ex) {
      log.warn("Redis cache read failed for {}; continuing without cache", cacheName);
      return CacheLookup.disabled();
    }
  }
  
  private <T> void put(String cacheName, UUID tenantId, String field, CacheLookup<T> lookup,
      T response, java.time.Duration ttl) {
    if (!lookup.cacheEnabled() || response == null || ttl == null || ttl.isZero()
        || ttl.isNegative()) {
      return;
    }
    try {
      String key = dataKey(cacheName, tenantId, lookup.version());
      redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(response));
      redisTemplate.expire(key, ttl);
    } catch (DataAccessException | JsonProcessingException ex) {
      log.warn("Redis cache write failed for {}; continuing without cache", cacheName);
    }
  }
  
  private long currentVersion(String cacheName, UUID tenantId) {
    String value = redisTemplate.opsForValue().get(versionKey(cacheName, tenantId));
    return value == null ? 0L : Long.parseLong(value);
  }
  
  private void incrementVersion(String cacheName, UUID tenantId) {
    if (!properties.enabled()) {
      return;
    }
    try {
      redisTemplate.opsForValue().increment(versionKey(cacheName, tenantId));
    } catch (DataAccessException ex) {
      log.warn("Redis cache invalidation failed for {}; cached data will expire by TTL", cacheName);
    }
  }
  
  private String dataKey(String cacheName, UUID tenantId, long version) {
    return namespaceProperties.redisPrefix() + "cache:" + cacheName + ":" + tenantId
        + ":v" + version;
  }
  
  private String versionKey(String cacheName, UUID tenantId) {
    return namespaceProperties.redisPrefix() + "cache-version:" + cacheName + ":" + tenantId;
  }
  
  private void afterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()
        || !TransactionSynchronizationManager.isActualTransactionActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }
  
  public record CacheLookup<T>(T value, long version, boolean cacheEnabled) {
    
    public boolean hit() {
      return value != null;
    }
    
    public static <T> CacheLookup<T> hit(long version, T value) {
      return new CacheLookup<>(value, version, true);
    }
    
    public static <T> CacheLookup<T> miss(long version) {
      return new CacheLookup<>(null, version, true);
    }
    
    public static <T> CacheLookup<T> disabled() {
      return new CacheLookup<>(null, 0L, false);
    }
    
    
  }
}
