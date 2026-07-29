package com.famora.security.jwt;

import com.famora.common.dto.ApiErrorResponse;
import com.famora.security.UserPrincipal;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  
  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = authHeader.substring(7);
    if (!jwtService.isTokenValid(token)) {
      unauthorized(response, request, "Invalid or expired access token");
      return;
    }
    UUID userId = jwtService.extractUserId(token);
    User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).orElse(null);
    if (user == null) {
      unauthorized(response, request, "Invalid or expired access token");
      return;
    }
    if (jwtService.isTokenIssuedBefore(token, user.getPasswordChangedAt())) {
      unauthorized(response, request, "Invalid or expired access token");
      return;
    }
    UserPrincipal principal = UserPrincipal.from(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    filterChain.doFilter(request, response);
  }
  
  private void unauthorized(HttpServletResponse response, HttpServletRequest request,
      String message) throws IOException {
    SecurityContextHolder.clearContext();
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(OffsetDateTime.now())
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message(message)
        .path(request.getRequestURI())
        .build();
    objectMapper.writeValue(response.getWriter(), body);
  }
}
