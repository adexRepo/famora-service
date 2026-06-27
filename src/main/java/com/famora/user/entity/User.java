package com.famora.user.entity;

import com.famora.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends BaseEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  
  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;
  
  @Column(name = "email", nullable = false, unique = true, length = 180)
  private String email;
  
  @Column(name = "password_hash", nullable = false, columnDefinition = "text")
  private String passwordHash;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status;
  
  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;
  
  @PrePersist
  public void prePersist() {
    if (status == null) {
      status = UserStatus.ACTIVE;
    }
  }
}
