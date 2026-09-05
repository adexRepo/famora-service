package com.famora.user.repository;

import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  
  Optional<User> findByEmailAndStatus(String email, UserStatus status);
  
  Optional<User> findByIdAndStatus(UUID id, UserStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id in :ids order by u.id")
  List<User> findAllByIdForUpdate(Collection<UUID> ids);
  
  boolean existsByEmail(String email);

}
