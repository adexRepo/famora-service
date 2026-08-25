package com.famora.common.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.redis")
public record RedisCacheProperties(
    boolean enabled,
    Duration financeDashboardTtl,
    Duration businessSummaryTtl
) {
}
