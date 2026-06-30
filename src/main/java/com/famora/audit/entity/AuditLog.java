package com.famora.audit.entity;

import com.famora.business.entity.Business;
import com.famora.common.entity.BaseEntity;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "family_id")
  private Family family;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_id")
  private Business business;
  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false)
  private AuditAction action;
  @Column(name = "entity_type", length = 100)
  private String entityType;
  @Column(name = "entity_id", columnDefinition = "uuid")
  private UUID entityId;
  @Column(name = "ip_address", length = 80)
  private String ipAddress;
  @Column(name = "user_agent", columnDefinition = "text")
  private String userAgent;
  @Column(name = "metadata", columnDefinition = "text")
  private String metadata;
}
