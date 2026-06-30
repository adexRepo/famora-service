package com.famora.note.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.common.spec.VisibleFamilyScopedSpecifications;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.note.dto.CreateNoteRequest;
import com.famora.note.dto.NoteListResponse;
import com.famora.note.dto.NoteResponse;
import com.famora.note.dto.UpdateNoteRequest;
import com.famora.note.entity.Note;
import com.famora.note.repository.NoteRepository;
import com.famora.note.spec.NoteSpecifications;
import com.famora.security.CurrentUserProvider;
import com.famora.security.FamilyContextService;
import com.famora.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {
  
  private final NoteRepository noteRepository;
  private final CurrentUserProvider currentUserProvider;
  private final FamilyContextService familyContextService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public NoteResponse create(CreateNoteRequest request) {
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = Note.builder().family(family).title(request.title().trim())
        .content(request.content().trim()).category(clean(request.category())).createdBy(user)
        .visibility(request.visibility()).build();
    
    noteRepository.save(note);
    
    auditLogService.log(family, user, AuditAction.NOTE_CREATED, "notes", note.getId(), null);
    
    return toResponse(note);
  }
  
  @Transactional(readOnly = true)
  public Page<NoteListResponse> list(FamilyContext ctx, String keyword, String category,
      Visibility visibility, Pageable pageable) {
    UUID familyId = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    Specification<Note> spec = Specification.where(
            VisibleFamilyScopedSpecifications.<Note>visibleToUser(familyId, userId, isOwner,
                Status.ACTIVE, visibility)).and(NoteSpecifications.keyword(keyword))
        .and(NoteSpecifications.category(category));
    
    Page<Note> page = noteRepository.findAll(spec, pageable);
    
    return page.map(this::toListResponse);
  }
  
  @Transactional(readOnly = true)
  public NoteResponse getDetail(UUID id) {
    Family family = familyContextService.getCurrentFamily();
    
    Note note = getNoteActive(id, family);
    
    return toResponse(note);
  }
  
  private Note getNoteActive(UUID id, Family family) {
    return noteRepository.findByIdAndFamilyIdAndStatus(id, family.getId(), Status.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
  }
  
  @Transactional
  public NoteResponse update(UUID id, UpdateNoteRequest request) {
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = getNoteActive(id, family);
    
    note.setTitle(request.title().trim());
    note.setContent(request.content().trim());
    note.setCategory(clean(request.category()));
    note.setUpdatedBy(user);
    note.setVisibility(request.visibility());
    
    noteRepository.save(note);
    
    auditLogService.log(family, user, AuditAction.NOTE_UPDATED, "notes", note.getId(), null);
    
    return toResponse(note);
  }
  
  @Transactional
  public void delete(UUID id) {
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Note note = getNoteActive(id, family);
    
    note.setStatus(Status.DELETED);
    note.setUpdatedBy(user);
    
    noteRepository.save(note);
    
    auditLogService.log(family, user, AuditAction.NOTE_DELETED, "notes", note.getId(), null);
  }
  
  private NoteResponse toResponse(Note note) {
    return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getCategory(),
        note.getCreatedAt(), note.getUpdatedAt());
  }
  
  private NoteListResponse toListResponse(Note note) {
    return new NoteListResponse(note.getId(), note.getTitle(), note.getCategory(),
        note.getCreatedAt(), note.getUpdatedAt());
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
