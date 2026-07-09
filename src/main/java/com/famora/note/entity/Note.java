package com.famora.note.entity;

import com.famora.common.entity.VisibleFamilyScopedEntity;
import com.famora.note.helper.NoteType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notes")
public class Note extends VisibleFamilyScopedEntity {
  
  @Column(name = "title", nullable = false, length = 180)
  private String title;
  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;
  @Enumerated(EnumType.STRING)
  @Column(name = "note_type", nullable = false, length = 30)
  private NoteType noteType = NoteType.TEXT;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content_json", columnDefinition = "jsonb")
  private JsonNode contentJson;
  @Column(name = "category", length = 80)
  private String category;
}
