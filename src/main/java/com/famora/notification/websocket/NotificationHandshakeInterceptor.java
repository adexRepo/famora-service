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
  
  static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";
  
  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) {
    String authHeader = request.getHeaders().getFirst("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      attributes.put(ACCESS_TOKEN_ATTRIBUTE, authHeader.substring(7));
      return true;
    }
    
    String query = request.getURI().getRawQuery();
    String token = accessTokenFromQuery(query);
    if (token != null) {
      attributes.put(ACCESS_TOKEN_ATTRIBUTE, token);
    }
    return true;
  }
  
  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
  }
  
  private String accessTokenFromQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    for (String pair : query.split("&")) {
      int separator = pair.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      String key = decode(pair.substring(0, separator));
      if ("access_token".equals(key)) {
        return decode(pair.substring(separator + 1));
      }
    }
    return null;
  }
  
  private String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
