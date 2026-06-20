package com.famora.note.entity;

import com.famora.common.entity.VisibleFamilyScopedEntity;
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
@Table(name = "notes")
public class Note extends VisibleFamilyScopedEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @Column(name = "title", nullable = false, length = 180)
  private String title;
  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;
  @Column(name = "category", length = 80)
  private String category;
  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;
}
