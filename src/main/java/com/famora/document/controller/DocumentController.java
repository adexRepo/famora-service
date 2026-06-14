package com.famora.document.controller;

import com.famora.common.exception.ApiResponse;
import com.famora.common.exception.Visibility;
import com.famora.document.dto.DocumentDtos;
import com.famora.document.dto.DocumentDtos.DocumentResponse;
import com.famora.document.helper.DocumentType;
import com.famora.document.service.DocumentService;
import com.famora.security.FamilyContextService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
  
  private final FamilyContextService families;
  private final DocumentService service;
  
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DocumentResponse> create(
      @RequestHeader("X-Family-Id") String familyId, @RequestParam("file") MultipartFile file,
      @RequestParam String title, @RequestParam DocumentType documentType,
      @RequestParam(required = false) String documentNumber,
      @RequestParam(required = false) UUID ownerUserId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
      @RequestParam(required = false) String notes,
      @RequestParam(defaultValue = "OWNER_ONLY") Visibility visibility) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(DocumentDtos.DocumentResponse.from(
        service.create(file, title, documentType, documentNumber, ownerUserId, issueDate,
            expiryDate, notes, visibility, ctx)));
  }
  
  @GetMapping
  public ApiResponse<Page<DocumentDtos.DocumentResponse>> list(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) DocumentType documentType,
      @RequestParam(required = false) Boolean expiringSoon,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(service.list(ctx, documentType, expiringSoon,
            PageRequest.of(page, size, Sort.by("createdAt").descending()))
        .map(DocumentDtos.DocumentResponse::from));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<DocumentDtos.DocumentResponse> get(
      @RequestHeader("X-Family-Id") String familyId, @PathVariable UUID id) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(DocumentDtos.DocumentResponse.from(service.get(id, ctx)));
  }
  
  @GetMapping("/{id}/download")
  public ResponseEntity<?> download(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    var d = service.download(id, ctx);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.file().getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(d.file().getOriginalName()).build().toString())
        .body(d.resource());
  }
  
  @PutMapping("/{id}")
  public ApiResponse<DocumentDtos.DocumentResponse> update(
      @RequestHeader("X-Family-Id") String familyId, @PathVariable UUID id,
      @RequestBody DocumentDtos.UpdateDocumentRequest req) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(DocumentDtos.DocumentResponse.from(service.update(id, req, ctx)));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<String> delete(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    service.delete(id, ctx);
    return ApiResponse.ok("Deleted");
  }
}
