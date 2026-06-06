package com.famora.user.repository;

import com.famora.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  
  Optional<User> findByEmailAndDeletedAtIsNull(String email);
  
  Optional<User> findByIdAndDeletedAtIsNull(UUID id);
  
  boolean existsByEmail(String email);
}
