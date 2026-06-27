package com.famora.note.spec;

import com.famora.note.entity.Note;
import org.springframework.data.jpa.domain.Specification;

public final class NoteSpecifications {
  
  private NoteSpecifications() {
  }
  
  public static Specification<Note> keyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) {
        return cb.conjunction();
      }
      
      String like = "%" + keyword.trim().toLowerCase() + "%";
      
      return cb.or(
          cb.like(cb.lower(root.get("title")), like),
          cb.like(cb.lower(cb.coalesce(root.get("content"), "")), like)
      );
    };
  }
  
  public static Specification<Note> category(String category) {
    return (root, query, cb) -> {
      if (category == null || category.isBlank()) {
        return cb.conjunction();
      }
      
      return cb.equal(
          cb.lower(cb.coalesce(root.get("category"), "")),
          category.trim().toLowerCase()
      );
    };
  }
}
