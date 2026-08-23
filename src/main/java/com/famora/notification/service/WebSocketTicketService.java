package com.famora.notification.service;

import com.famora.notification.dto.WebSocketTicketResponse;
import com.famora.notification.entity.WebSocketTicket;
import com.famora.notification.repository.WebSocketTicketRepository;
import com.famora.security.AbuseRateLimitService;
import com.famora.security.CurrentUserProvider;
import com.famora.security.TokenHashService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

  private static final long TICKET_LIFETIME_SECONDS = 30;

  private final WebSocketTicketRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final TokenHashService tokenHashService;
  private final AbuseRateLimitService rateLimitService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public WebSocketTicketResponse issue() {
    User user = currentUserProvider.getCurrentUser();
    rateLimitService.checkWebSocketTicket(user.getId());
    String rawTicket = generateTicket();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(TICKET_LIFETIME_SECONDS);
    WebSocketTicket ticket = new WebSocketTicket();
    ticket.setTicketHash(tokenHashService.sha256(rawTicket));
    ticket.setUser(user);
    ticket.setExpiresAt(expiresAt);
    repository.save(ticket);
    return new WebSocketTicketResponse(rawTicket, expiresAt);
  }

  @Transactional
  public User consume(String rawTicket) {
    OffsetDateTime now = OffsetDateTime.now();
    WebSocketTicket ticket = repository.findByTicketHashForUpdate(
            tokenHashService.sha256(rawTicket))
        .orElseThrow(() -> invalidTicket());
    if (ticket.getConsumedAt() != null || !ticket.getExpiresAt().isAfter(now)
        || ticket.getUser().getStatus() != UserStatus.ACTIVE) {
      throw invalidTicket();
    }
    ticket.setConsumedAt(now);
    repository.save(ticket);
    return ticket.getUser();
  }

  @Scheduled(cron = "${app.security.websocket-ticket-cleanup-cron:0 29 3 * * *}")
  @Transactional
  public void cleanup() {
    repository.deleteExpiredOrConsumed(OffsetDateTime.now());
  }

  private String generateTicket() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private AuthenticationCredentialsNotFoundException invalidTicket() {
    return new AuthenticationCredentialsNotFoundException("AUTH_INVALID");
  }
}
