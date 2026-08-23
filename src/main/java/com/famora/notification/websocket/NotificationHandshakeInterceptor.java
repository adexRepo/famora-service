package com.famora.notification.websocket;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {
  
  static final String WEBSOCKET_TICKET_ATTRIBUTE = "webSocketTicket";
  
  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) {
    String query = request.getURI().getRawQuery();
    String ticket = ticketFromQuery(query);
    if (ticket != null) {
      attributes.put(WEBSOCKET_TICKET_ATTRIBUTE, ticket);
    }
    return true;
  }
  
  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
  }
  
  private String ticketFromQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    for (String pair : query.split("&")) {
      int separator = pair.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      String key = decode(pair.substring(0, separator));
      if ("ticket".equals(key)) {
        return decode(pair.substring(separator + 1));
      }
    }
    return null;
  }
  
  private String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
