package com.famora.notification.websocket;

import java.security.Principal;
import java.util.UUID;

public record NotificationWebSocketPrincipal(UUID userId) implements Principal {
  
  @Override
  public String getName() {
    return userId.toString();
  }
}
