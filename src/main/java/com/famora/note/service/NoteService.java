package com.famora.note.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.family.entity.Family;
import com.famora.note.dto.CreateNoteRequest;
import com.famora.note.dto.NoteListResponse;
import com.famora.note.dto.NoteResponse;
import com.famora.note.dto.UpdateNoteRequest;
import com.famora.note.entity.Note;
import com.famora.note.repository.NoteRepository;
import com.famora.security.CurrentUserService;
import com.famora.security.FamilyContextService;
import com.famora.user.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {
  
  private final NoteRepository noteRepository;
  private final CurrentUserService currentUserService;
  private final FamilyContextService familyContextService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public NoteResponse create(CreateNoteRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = Note.builder()
        .family(family)
        .title(request.title().trim())
        .content(request.content().trim())
        .category(clean(request.category()))
        .createdBy(user)
        .build();
    
    noteRepository.save(note);
    
    auditLogService.log(
        family,
        user,
        AuditAction.NOTE_CREATED,
        "notes",
        note.getId(),
        null
    );
    
    return toResponse(note);
  }
  
  @Transactional(readOnly = true)
  public List<NoteListResponse> list(String keyword, String category) {
    Family family = familyContextService.getCurrentFamily();
    
    String cleanKeyword = clean(keyword);
    String cleanCategory = clean(category);
    
    List<Note> notes;
    
    if (cleanKeyword == null && cleanCategory == null) {
      notes = noteRepository.findByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDesc(
          family.getId()
      );
    } else if (cleanKeyword == null) {
      notes = noteRepository.findByFamilyIdAndCategoryIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(
          family.getId(),
          cleanCategory
      );
    } else if (cleanCategory == null) {
      notes = noteRepository.searchByKeyword(
          family.getId(),
          cleanKeyword
      );
    } else {
      notes = noteRepository.searchByKeywordAndCategory(
          family.getId(),
          cleanKeyword,
          cleanCategory
      );
    }
    
    return notes.stream()
        .map(this::toListResponse)
        .toList();
  }
  
  @Transactional(readOnly = true)
  public NoteResponse getDetail(UUID id) {
    Family family = familyContextService.getCurrentFamily();
    
    Note note = noteRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
    
    return toResponse(note);
  }
  
  @Transactional
  public NoteResponse update(UUID id, UpdateNoteRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = noteRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
    
    note.setTitle(request.title().trim());
    note.setContent(request.content().trim());
    note.setCategory(clean(request.category()));
    note.setUpdatedBy(user);
    
    noteRepository.save(note);
    
    auditLogService.log(
        family,
        user,
        AuditAction.NOTE_UPDATED,
        "notes",
        note.getId(),
        null
    );
    
    return toResponse(note);
  }
  
  @Transactional
  public void delete(UUID id) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = noteRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
    
    note.setDeletedAt(OffsetDateTime.now());
    note.setUpdatedBy(user);
    
    noteRepository.save(note);
    
    auditLogService.log(
        family,
        user,
        AuditAction.NOTE_DELETED,
        "notes",
        note.getId(),
        null
    );
  }
  
  private NoteResponse toResponse(Note note) {
    return new NoteResponse(
        note.getId(),
        note.getTitle(),
        note.getContent(),
        note.getCategory(),
        note.getCreatedAt(),
        note.getUpdatedAt()
    );
  }
  
  private NoteListResponse toListResponse(Note note) {
    return new NoteListResponse(
        note.getId(),
        note.getTitle(),
        note.getCategory(),
        note.getCreatedAt(),
        note.getUpdatedAt()
    );
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
