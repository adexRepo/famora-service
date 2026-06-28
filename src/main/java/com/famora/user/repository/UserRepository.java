package com.famora.user.repository;

import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  
  Optional<User> findByEmailAndStatus(String email, UserStatus status);
  
  Optional<User> findByIdAndStatus(UUID id, UserStatus status);
  
  boolean existsByEmail(String email);
}
