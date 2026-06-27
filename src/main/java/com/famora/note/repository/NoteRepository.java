package com.famora.note.repository;

import com.famora.common.helper.Status;
import com.famora.note.entity.Note;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NoteRepository extends JpaRepository<Note, UUID>,
    JpaSpecificationExecutor<Note> {
  
  Optional<Note> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
}
