package com.famora.note.repository;

import com.famora.note.entity.Note;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoteRepository extends JpaRepository<Note, UUID> {
  
  Optional<Note> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  @Query("""
          select n
          from Note n
          where n.family.id = :familyId
            and n.deletedAt is null
            and (
                :keyword is null
                or lower(n.title) like concat('%', lower(:keyword), '%')
                or lower(n.content) like concat('%', lower(:keyword), '%')
            )
            and (
                :category is null
                or lower(n.category) = lower(:category)
            )
          order by n.createdAt desc
      """)
  List<Note> search(UUID familyId, String keyword, String category);
}
