package com.famora.vault.entity;

import com.famora.common.entity.BaseTimeEntity;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "vault_items")
public class VaultItem extends BaseTimeEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "family_id", nullable = false)
  private Family family;
  @Column(name = "title", nullable = false, length = 180)
  private String title;
  @Column(name = "username", length = 180)
  private String username;
  @Column(name = "encrypted_secret", nullable = false, columnDefinition = "text")
  private String encryptedSecret;
  @Column(name = "url", columnDefinition = "text")
  private String url;
  @Column(name = "notes", columnDefinition = "text")
  private String notes;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;
  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;
}
