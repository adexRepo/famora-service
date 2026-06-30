package com.famora.user.service;

import com.famora.security.CurrentUserProvider;
import com.famora.user.dto.UpdateUserProfileRequest;
import com.famora.user.dto.UserProfileResponse;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final CurrentUserProvider currentUserProvider;
  private final UserRepository userRepository;
  
  @Transactional(readOnly = true)
  public UserProfileResponse getMe() {
    User user = currentUserProvider.getCurrentUser();
    return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
        user.getStatus().name(), user.getCreatedAt());
  }
  
  @Transactional
  public UserProfileResponse updateMe(UpdateUserProfileRequest request) {
    User user = currentUserProvider.getCurrentUser();
    user.setFullName(request.fullName().trim());
    userRepository.save(user);
    return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
        user.getStatus().name(), user.getCreatedAt());
  }
}
