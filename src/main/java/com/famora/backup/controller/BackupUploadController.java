package com.famora.backup.controller;

import com.famora.backup.dto.BackupUploadDtos.BackupItemResponse;
import com.famora.backup.dto.BackupUploadDtos.BackupSessionDetailResponse;
import com.famora.backup.dto.BackupUploadDtos.BackupSessionResponse;
import com.famora.backup.dto.BackupUploadDtos.CreateBackupSessionRequest;
import com.famora.backup.service.BackupUploadService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.security.FamilyContextService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/backup/sessions")
@RequiredArgsConstructor
public class BackupUploadController {
  
  private final FamilyContextService families;
  private final BackupUploadService service;
  
  @PostMapping
  public ApiResponse<BackupSessionDetailResponse> createSession(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @Valid @RequestBody CreateBackupSessionRequest request) {
    return ApiResponse.ok(service.createSession(request, families.require(familyId)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<BackupSessionResponse>> listSessions(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(PageResponse.from(service.listSessions(
        families.require(familyId),
        PageRequest.of(page, size, Sort.by("createdAt").descending())
    )));
  }
  
  @GetMapping("/{sessionId}")
  public ApiResponse<BackupSessionDetailResponse> getSession(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @PathVariable UUID sessionId) {
    return ApiResponse.ok(service.getSession(sessionId, families.require(familyId)));
  }
  
  @PutMapping(
      value = "/{sessionId}/items/{itemId}/chunks/{chunkNumber}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ApiResponse<BackupItemResponse> uploadChunk(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @PathVariable UUID sessionId,
      @PathVariable UUID itemId,
      @PathVariable int chunkNumber,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) String sha256) {
    return ApiResponse.ok(service.uploadChunk(sessionId, itemId, chunkNumber, file, sha256,
        families.require(familyId)));
  }
  
  @PostMapping("/{sessionId}/items/{itemId}/complete")
  public ApiResponse<BackupItemResponse> completeItem(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @PathVariable UUID sessionId,
      @PathVariable UUID itemId) {
    return ApiResponse.ok(service.completeItem(sessionId, itemId, families.require(familyId)));
  }
  
  @PostMapping("/{sessionId}/cancel")
  public ApiResponse<BackupSessionDetailResponse> cancelSession(
      @RequestHeader(FamilyContextService.FAMILY_ID_HEADER) String familyId,
      @PathVariable UUID sessionId) {
    return ApiResponse.ok(service.cancelSession(sessionId, families.require(familyId)));
  }
}
