package com.famora.user.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.user.dto.UpdateUserProfileRequest;
import com.famora.user.dto.UserProfileResponse;
import com.famora.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {
  
  private final UserService userService;
  
  @GetMapping
  public ApiResponse<UserProfileResponse> getMe() {
    return ApiResponse.ok(userService.getMe());
  }
  
  @PutMapping
  public ApiResponse<UserProfileResponse> updateMe(
      @Valid @RequestBody UpdateUserProfileRequest request) {
    return ApiResponse.ok(userService.updateMe(request));
  }
}
