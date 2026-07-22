package com.famora.note.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.common.helper.PagingHelper;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.note.dto.CreateNoteRequest;
import com.famora.note.dto.NoteDetailResponse;
import com.famora.note.dto.NoteListResponse;
import com.famora.note.dto.UpdateNoteRequest;
import com.famora.note.helper.NoteType;
import com.famora.note.service.NoteService;
import com.famora.security.FamilyContextService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {
  
  private final FamilyContextService familyContextService;
  private final NoteService noteService;
  
  @PostMapping
  public ApiResponse<NoteDetailResponse> create(
      @RequestHeader("X-Family-Id") String familyId,
      @Valid @RequestBody CreateNoteRequest request) {
    FamilyContext ctx = familyContextService.require(familyId);
    return ApiResponse.ok(noteService.create(ctx, request));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<NoteListResponse>> list(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Visibility visibility,
      @RequestParam(required = false) NoteType noteType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    
    FamilyContext ctx = familyContextService.require(familyId);
    
    PageRequest pageRequest = PagingHelper.buildPageRequest(page, size, "updatedAt", "createdAt",
        "title"
    );
    
    return ApiResponse.ok(PageResponse.from(
        noteService.list(ctx, keyword, category, visibility, noteType, pageRequest)));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<NoteDetailResponse> getDetail(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = familyContextService.require(familyId);
    return ApiResponse.ok(noteService.getDetail(ctx, id));
  }
  
  @PutMapping("/{id}")
  public ApiResponse<NoteDetailResponse> update(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateNoteRequest request
  ) {
    FamilyContext ctx = familyContextService.require(familyId);
    return ApiResponse.ok(noteService.update(ctx, id, request));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<Boolean> delete(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = familyContextService.require(familyId);
    noteService.delete(ctx, id);
    return ApiResponse.ok("Success", true);
  }
}
