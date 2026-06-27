package com.famora.user.repository;

import com.famora.common.helper.Status;
import com.famora.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  
  Optional<User> findByEmailAndStatus(String email, Status status);
  
  Optional<User> findByIdAndStatus(UUID id, Status status);
  
  boolean existsByEmail(String email);
}
