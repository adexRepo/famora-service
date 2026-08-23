package com.famora.notification.websocket;

import com.famora.notification.service.WebSocketTicketService;
import com.famora.security.jwt.JwtService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationStompAuthInterceptor implements ChannelInterceptor {
  
  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final WebSocketTicketService webSocketTicketService;
  
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (accessor.getCommand() != StompCommand.CONNECT) {
      return message;
    }
    
    User user = authenticate(accessor);
    
    accessor.setUser(new NotificationWebSocketPrincipal(user.getId()));
    return message;
  }
  
  private String resolveToken(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    
    String tokenHeader = accessor.getFirstNativeHeader("access_token");
    if (tokenHeader != null && !tokenHeader.isBlank()) {
      return tokenHeader;
    }
    
    return null;
  }

  private User authenticate(StompHeaderAccessor accessor) {
    String token = resolveToken(accessor);
    if (token != null && jwtService.isTokenValid(token)) {
      UUID userId = jwtService.extractUserId(token);
      User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
          .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("AUTH_INVALID"));
      if (jwtService.isTokenIssuedBefore(token, user.getPasswordChangedAt())) {
        throw new AuthenticationCredentialsNotFoundException("AUTH_INVALID");
      }
      return user;
    }

    Map<String, Object> attributes = accessor.getSessionAttributes();
    Object ticket = attributes == null ? null
        : attributes.get(NotificationHandshakeInterceptor.WEBSOCKET_TICKET_ATTRIBUTE);
    if (ticket instanceof String value && !value.isBlank()) {
      return webSocketTicketService.consume(value);
    }
    throw new AuthenticationCredentialsNotFoundException("AUTH_INVALID");
  }
}
