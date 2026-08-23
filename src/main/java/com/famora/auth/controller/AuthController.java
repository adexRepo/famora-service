package com.famora.auth.controller;

import com.famora.auth.dto.AuthResponse;
import com.famora.auth.dto.LoginRequest;
import com.famora.auth.dto.RefreshTokenRequest;
import com.famora.auth.dto.RegisterRequest;
import com.famora.auth.service.AuthService;
import com.famora.common.dto.ApiResponse;
import com.famora.security.AbuseRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  
  private final AuthService authService;
  private final AbuseRateLimitService rateLimitService;
  
  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
      HttpServletRequest servletRequest) {
    rateLimitService.checkRegistration(servletRequest, normalizeEmail(request.email()));
    return ApiResponse.ok(authService.register(request));
  }
  
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest) {
    rateLimitService.checkLogin(servletRequest, normalizeEmail(request.email()));
    return ApiResponse.ok(authService.login(request));
  }
  
  @PostMapping("/refresh")
  public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
      HttpServletRequest servletRequest) {
    rateLimitService.checkRefresh(servletRequest);
    return ApiResponse.ok(authService.refresh(request));
  }
  
  @PostMapping("/logout")
  public ApiResponse<Boolean> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout();
    return ApiResponse.ok("Success", true);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(java.util.Locale.ROOT);
  }
}
