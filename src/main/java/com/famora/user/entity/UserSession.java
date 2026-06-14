package com.famora.user.entity;

import com.famora.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "user_sessions",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_sessions_refresh_token_hash",
        columnNames = "refresh_token_hash"))
public class UserSession extends BaseEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "refresh_token_hash", nullable = false, columnDefinition = "text")
  private String refreshTokenHash;
  
  @Column(name = "device_id", length = 150)
  private String deviceId;
  
  @Column(name = "device_name", length = 150)
  private String deviceName;
  
  @Column(name = "ip_address", length = 80)
  private String ipAddress;
  
  @Column(name = "user_agent", columnDefinition = "text")
  private String userAgent;
  
  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;
  
  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;
}
