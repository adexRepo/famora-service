package com.famora.auth.controller;

import com.famora.auth.dto.AuthResponse;
import com.famora.auth.dto.LoginRequest;
import com.famora.auth.dto.RefreshTokenRequest;
import com.famora.auth.dto.RegisterRequest;
import com.famora.auth.service.AuthService;
import com.famora.common.dto.ApiResponse;
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
  
  @PostMapping("/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(authService.register(request));
  }
  
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }
  
  @PostMapping("/refresh")
  public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(authService.refresh(request));
  }
  
  @PostMapping("/logout")
  public ApiResponse<Boolean> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.refreshToken());
    return ApiResponse.ok("Success", true);
  }
}
