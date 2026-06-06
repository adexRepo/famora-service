package com.famora.note.repository;

import com.famora.note.entity.Note;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoteRepository extends JpaRepository<Note, UUID> {
  
  Optional<Note> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  List<Note> findByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID familyId);
  
  List<Note> findByFamilyIdAndCategoryIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID familyId,
      String category
  );
  
  @Query("""
          select n
          from Note n
          where n.family.id = :familyId
            and n.deletedAt is null
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(n.content) like lower(concat('%', :keyword, '%'))
            )
          order by n.createdAt desc
      """)
  List<Note> searchByKeyword(UUID familyId, String keyword);
  
  @Query("""
          select n
          from Note n
          where n.family.id = :familyId
            and n.deletedAt is null
            and lower(n.category) = lower(:category)
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(n.content) like lower(concat('%', :keyword, '%'))
            )
          order by n.createdAt desc
      """)
  List<Note> searchByKeywordAndCategory(UUID familyId, String keyword, String category);
}
