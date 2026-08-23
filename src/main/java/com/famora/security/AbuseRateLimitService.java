package com.famora.security;

import com.famora.security.config.RateLimitProperties;
import com.famora.security.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AbuseRateLimitService {

  private final JdbcTemplate jdbcTemplate;
  private final TokenHashService tokenHashService;
  private final RateLimitProperties properties;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkLogin(HttpServletRequest request, String normalizedEmail) {
    check("LOGIN", properties.loginAttempts(), clientIp(request), normalizedEmail);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkRegistration(HttpServletRequest request, String normalizedEmail) {
    check("REGISTRATION", properties.registrationAttempts(), clientIp(request), normalizedEmail);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkRefresh(HttpServletRequest request) {
    check("REFRESH_IP", properties.refreshAttempts(), clientIp(request));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkRefreshAccount(UUID userId) {
    check("REFRESH_ACCOUNT", properties.refreshAttempts(), userId.toString());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkWebSocketTicket(UUID userId) {
    check("WEBSOCKET_TICKET", properties.websocketTicketAttempts(), userId.toString());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkInvitationJoin(HttpServletRequest request, String accountId, String code) {
    check("INVITATION_JOIN", properties.invitationJoinAttempts(), clientIp(request), accountId,
        code.trim().toUpperCase());
  }

  private void check(String action, int limit, String... identifiers) {
    Instant now = Instant.now();
    long windowSeconds = properties.windowSeconds();
    Instant windowStart = Instant.ofEpochSecond(
        (now.getEpochSecond() / windowSeconds) * windowSeconds);
    Instant expiresAt = windowStart.plus(windowSeconds * 2, ChronoUnit.SECONDS);

    for (String identifier : identifiers) {
      String keyHash = tokenHashService.sha256(action + ":" + identifier);
      Integer attempts = jdbcTemplate.queryForObject("""
          insert into famora.abuse_rate_limits
            (action, key_hash, window_started_at, attempts, expires_at)
          values (?, ?, ?, 1, ?)
          on conflict (action, key_hash) do update set
            attempts = case
              when famora.abuse_rate_limits.window_started_at < excluded.window_started_at then 1
              else famora.abuse_rate_limits.attempts + 1
            end,
            window_started_at = greatest(
              famora.abuse_rate_limits.window_started_at, excluded.window_started_at),
            expires_at = excluded.expires_at
          returning attempts
          """, Integer.class, action, keyHash, Timestamp.from(windowStart),
          Timestamp.from(expiresAt));
      if (attempts != null && attempts > limit) {
        long retryAfter = Math.max(1, windowStart.plusSeconds(windowSeconds).getEpochSecond()
            - now.getEpochSecond());
        throw new RateLimitExceededException(retryAfter);
      }
    }
  }

  private String clientIp(HttpServletRequest request) {
    String remoteAddress = request.getRemoteAddr();
    return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
  }

  @Scheduled(cron = "${app.security.rate-limit-cleanup-cron:0 43 3 * * *}")
  @Transactional
  public void deleteExpiredCounters() {
    jdbcTemplate.update("delete from famora.abuse_rate_limits where expires_at < now()");
  }
}
