package com.famora.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.notification.entity.WebSocketTicket;
import com.famora.notification.repository.WebSocketTicketRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.security.AbuseRateLimitService;
import com.famora.security.TokenHashService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebSocketTicketServiceTest {

  @Mock private WebSocketTicketRepository repository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private AbuseRateLimitService rateLimitService;

  private final TokenHashService tokenHashService = new TokenHashService();
  private WebSocketTicketService service;
  private User user;

  @BeforeEach
  void setUp() {
    service = new WebSocketTicketService(repository, currentUserProvider, tokenHashService,
        rateLimitService);
    user = User.builder().fullName("Socket User").email("socket@example.com")
        .passwordHash("hash").status(UserStatus.ACTIVE).build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
  }

  @Test
  void issuedTicketIsStoredOnlyAsHashAndExpiresQuickly() {
    when(currentUserProvider.getCurrentUser()).thenReturn(user);
    ArgumentCaptor<WebSocketTicket> captor = ArgumentCaptor.forClass(WebSocketTicket.class);

    var response = service.issue();

    verify(repository).save(captor.capture());
    verify(rateLimitService).checkWebSocketTicket(user.getId());
    WebSocketTicket stored = captor.getValue();
    assertThat(stored.getTicketHash()).isEqualTo(tokenHashService.sha256(response.ticket()));
    assertThat(stored.getTicketHash()).doesNotContain(response.ticket());
    assertThat(response.expiresAt()).isAfter(OffsetDateTime.now())
        .isBefore(OffsetDateTime.now().plusMinutes(1));
  }

  @Test
  void ticketCanOnlyBeConsumedOnce() {
    WebSocketTicket ticket = new WebSocketTicket();
    ticket.setTicketHash(tokenHashService.sha256("raw-ticket"));
    ticket.setUser(user);
    ticket.setExpiresAt(OffsetDateTime.now().plusSeconds(20));
    when(repository.findByTicketHashForUpdate(ticket.getTicketHash()))
        .thenReturn(Optional.of(ticket));

    assertThat(service.consume("raw-ticket")).isSameAs(user);
    assertThat(ticket.getConsumedAt()).isNotNull();
    assertThatThrownBy(() -> service.consume("raw-ticket"))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }
}
