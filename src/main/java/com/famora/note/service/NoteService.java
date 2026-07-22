package com.famora.note.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.common.helper.PermissionHelper;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.common.spec.VisibleFamilyScopedSpecifications;
import com.famora.family.dto.FamilyContext;
import com.famora.note.dto.CreateNoteRequest;
import com.famora.note.dto.NoteDetailResponse;
import com.famora.note.dto.NoteListResponse;
import com.famora.note.dto.UpdateNoteRequest;
import com.famora.note.entity.Note;
import com.famora.note.helper.NoteType;
import com.famora.note.repository.NoteRepository;
import com.famora.note.spec.NoteSpecifications;
import com.famora.user.entity.User;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {
  
  private final NoteRepository noteRepository;
  private final AuditLogService auditLogService;
  
  private static final int NOTE_CONTENT_PREVIEW_MAX_LENGTH = 50;
  
  @Transactional
  public NoteDetailResponse create(FamilyContext ctx, CreateNoteRequest request) {
    User user = ctx.user();
    NoteType noteType = resolveNoteType(request.noteType());
    validateContentJson(noteType, request.contentJson());
    
    Note note = Note.builder().family(ctx.family()).title(request.title().trim())
        .content(request.content().trim()).category(clean(request.category())).createdBy(user)
        .visibility(resolveVisibility(request.visibility())).noteType(noteType)
        .contentJson(request.contentJson()).build();
    
    noteRepository.save(note);
    
    auditLogService.log(ctx.family(), user, AuditAction.NOTE_CREATED, "notes", note.getId(), null);
    
    return toResponse(ctx, note);
  }
  
  @Transactional(readOnly = true)
  public Page<NoteListResponse> list(FamilyContext ctx, String keyword, String category,
      Visibility visibility, NoteType noteType, Pageable pageable) {
    UUID familyId = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    Specification<Note> spec = Specification.where(
            VisibleFamilyScopedSpecifications.<Note>visibleToUser(familyId, userId, isOwner,
                Status.ACTIVE, visibility)).and(NoteSpecifications.keyword(keyword))
        .and(NoteSpecifications.category(category))
        .and(NoteSpecifications.noteType(noteType));
    
    Page<Note> page = noteRepository.findAll(spec, pageable);
    
    return page.map(note -> toListResponse(ctx, note));
  }
  
  @Transactional(readOnly = true)
  public NoteDetailResponse getDetail(FamilyContext ctx, UUID id) {
    Note note = getNoteActive(ctx, id);
    PermissionHelper.assertCanAccess(note.getVisibility(), note.getStatus(),
        note.getCreatedBy().getId(), ctx);
    
    return toResponse(ctx, note);
  }
  
  private Note getNoteActive(FamilyContext ctx, UUID id) {
    return noteRepository.findByIdAndFamilyIdAndStatus(id, ctx.family().getId(), Status.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
  }
  
  @Transactional
  public NoteDetailResponse update(FamilyContext ctx, UUID id, UpdateNoteRequest request) {
    User user = ctx.user();
    NoteType noteType = resolveNoteType(request.noteType());
    validateContentJson(noteType, request.contentJson());
    
    Note note = getNoteActive(ctx, id);
    assertCanMutate(ctx, note);
    
    note.setTitle(request.title().trim());
    note.setContent(request.content().trim());
    note.setCategory(clean(request.category()));
    note.setNoteType(noteType);
    note.setContentJson(request.contentJson());
    note.setUpdatedBy(user);
    note.setVisibility(request.visibility());
    
    noteRepository.save(note);
    
    auditLogService.log(ctx.family(), user, AuditAction.NOTE_UPDATED, "notes", note.getId(), null);
    
    return toResponse(ctx, note);
  }
  
  @Transactional
  public void delete(FamilyContext ctx, UUID id) {
    User user = ctx.user();
    Note note = getNoteActive(ctx, id);
    assertCanMutate(ctx, note);
    
    note.setStatus(Status.DELETED);
    note.setUpdatedBy(user);
    
    noteRepository.save(note);
    
    auditLogService.log(ctx.family(), user, AuditAction.NOTE_DELETED, "notes", note.getId(), null);
  }
  
  private NoteDetailResponse toResponse(FamilyContext ctx, Note note) {
    return new NoteDetailResponse(note.getId(), note.getTitle(), note.getContent(),
        note.getCategory(), note.getVisibility(), note.getNoteType(), note.getContentJson(),
        note.getCreatedBy().getId(), note.getCreatedBy().getFullName(),
        canMutate(ctx, note), canMutate(ctx, note), canToggleChecklist(ctx, note),
        note.getCreatedAt(), note.getUpdatedAt());
  }
  
  private NoteListResponse toListResponse(FamilyContext ctx, Note note) {
    return new NoteListResponse(note.getId(), note.getTitle(), note.getCategory(),
        note.getVisibility(), note.getNoteType(), noteContentPreview(note.getContent()),
        note.getCreatedBy().getId(), note.getCreatedBy().getFullName(),
        canMutate(ctx, note), canMutate(ctx, note), canToggleChecklist(ctx, note),
        note.getUpdatedAt());
  }
  
  public static String noteContentPreview(String content) {
    String normalized = content == null
        ? ""
        : content.trim().replaceAll("\\s+", " ");
    
    if (normalized.length() <= NOTE_CONTENT_PREVIEW_MAX_LENGTH) {
      return normalized;
    }
    
    String suffix = "...";
    return normalized.substring(
        0,
        NOTE_CONTENT_PREVIEW_MAX_LENGTH - suffix.length()
    ) + suffix;
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
  
  private NoteType resolveNoteType(NoteType noteType) {
    return noteType == null ? NoteType.TEXT : noteType;
  }
  
  private Visibility resolveVisibility(Visibility visibility) {
    return visibility == null ? Visibility.PRIVATE : visibility;
  }
  
  private void assertCanMutate(FamilyContext ctx, Note note) {
    if (canMutate(ctx, note)) {
      return;
    }
    throw new AppException(HttpStatus.FORBIDDEN,
        "Only note creator or family owner can modify this note");
  }
  
  private boolean canMutate(FamilyContext ctx, Note note) {
    return ctx.owner() || note.getCreatedBy().getId().equals(ctx.user().getId());
  }
  
  private boolean canToggleChecklist(FamilyContext ctx, Note note) {
    return note.getNoteType() == NoteType.CHECKLIST && canMutate(ctx, note);
  }
  
  private void validateContentJson(NoteType noteType, Map<String, Object> contentJson) {
    if (noteType == NoteType.TEXT) {
      validateOptionalString(contentJson, "text");
      return;
    }
    
    if (contentJson == null) {
      throw badContentJson("contentJson is required for " + noteType + " notes");
    }
    
    switch (noteType) {
      case CHECKLIST -> validateChecklist(contentJson);
      case INSTRUCTION -> validateInstruction(contentJson);
      case DECISION -> validateDecision(contentJson);
      case JOURNAL -> validateJournal(contentJson);
      case MINDMAP -> validateMindmap(contentJson);
      default -> {
        // Handled above. for text
      }
    }
  }
  
  private void validateChecklist(Map<String, Object> contentJson) {
    for (Map<String, Object> item : requireObjectList(contentJson, "items")) {
      requireString(item, "id");
      requireString(item, "text");
      requireBoolean(item, "checked");
      requireNumber(item, "order");
    }
  }
  
  private void validateInstruction(Map<String, Object> contentJson) {
    for (Map<String, Object> step : requireObjectList(contentJson, "steps")) {
      requireString(step, "id");
      requireString(step, "title");
      validateOptionalString(step, "description");
      requireNumber(step, "order");
    }
  }
  
  private void validateDecision(Map<String, Object> contentJson) {
    requireString(contentJson, "question");
    for (Map<String, Object> option : requireObjectList(contentJson, "options")) {
      requireString(option, "id");
      requireString(option, "title");
      validateOptionalStringList(option, "pros");
      validateOptionalStringList(option, "cons");
      requireNumber(option, "order");
    }
    validateOptionalString(contentJson, "decision");
    validateOptionalString(contentJson, "reason");
  }
  
  private void validateJournal(Map<String, Object> contentJson) {
    String entryDate = requireString(contentJson, "entryDate");
    try {
      LocalDate.parse(entryDate);
    } catch (DateTimeParseException ex) {
      throw badContentJson("contentJson.entryDate must use yyyy-MM-dd format");
    }
    validateOptionalString(contentJson, "mood");
    validateOptionalString(contentJson, "content");
  }
  
  private void validateMindmap(Map<String, Object> contentJson) {
    for (Map<String, Object> node : requireObjectList(contentJson, "nodes")) {
      requireString(node, "id");
      requireString(node, "text");
      validateOptionalString(node, "parentId");
      requireNumber(node, "order");
    }
  }
  
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> requireObjectList(Map<String, Object> source, String field) {
    Object value = source.get(field);
    if (!(value instanceof List<?> values)) {
      throw badContentJson("contentJson." + field + " must be an array");
    }
    for (Object item : values) {
      if (!(item instanceof Map<?, ?>)) {
        throw badContentJson("contentJson." + field + " items must be objects");
      }
    }
    return (List<Map<String, Object>>) value;
  }
  
  private String requireString(Map<String, Object> source, String field) {
    Object value = source.get(field);
    if (!(value instanceof String text) || text.isBlank()) {
      throw badContentJson("contentJson." + field + " is required");
    }
    return text;
  }
  
  private void requireBoolean(Map<String, Object> source, String field) {
    if (!(source.get(field) instanceof Boolean)) {
      throw badContentJson("contentJson." + field + " must be boolean");
    }
  }
  
  private void requireNumber(Map<String, Object> source, String field) {
    if (!(source.get(field) instanceof Number)) {
      throw badContentJson("contentJson." + field + " must be number");
    }
  }
  
  private void validateOptionalString(Map<String, Object> source, String field) {
    if (source == null || !source.containsKey(field) || source.get(field) == null) {
      return;
    }
    if (!(source.get(field) instanceof String)) {
      throw badContentJson("contentJson." + field + " must be string");
    }
  }
  
  private void validateOptionalStringList(Map<String, Object> source, String field) {
    if (!source.containsKey(field) || source.get(field) == null) {
      return;
    }
    if (!(source.get(field) instanceof List<?> values)) {
      throw badContentJson("contentJson." + field + " must be an array");
    }
    boolean allStrings = values.stream().allMatch(String.class::isInstance);
    if (!allStrings) {
      throw badContentJson("contentJson." + field + " must contain only strings");
    }
  }
  
  private AppException badContentJson(String message) {
    return new AppException(HttpStatus.BAD_REQUEST, message);
  }
}
