package com.famora.file.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.common.helper.Visibility;
import com.famora.file.dto.FileDtos;
import com.famora.file.dto.FileDtos.FileResponse;
import com.famora.file.helper.FileType;
import com.famora.file.service.FileService;
import com.famora.security.FamilyContextService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
  
  private final FamilyContextService families;
  private final FileService service;
  
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<FileResponse> upload(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String notes,
      @RequestParam(defaultValue = "PRIVATE") Visibility visibility) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(FileDtos.FileResponse.from(
        service.upload(file, category, notes, visibility, ctx, "files")));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<FileResponse>> list(@RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) FileType fileType,
      @RequestParam(required = false) Visibility visibility,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    var ctx = families.require(familyId);
    
    return ApiResponse.ok(PageResponse.from(service.list(ctx, keyword, fileType, visibility,
            PageRequest.of(page, size, Sort.by("createdAt").descending()))
        .map(FileDtos.FileResponse::from)));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<FileDtos.FileResponse> get(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(FileDtos.FileResponse.from(service.get(id, ctx)));
  }
  
  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    var d = service.download(id, ctx);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.file().getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(d.file().getOriginalName()).build().toString())
        .body(d.resource());
  }
  
  @PutMapping("/{id}")
  public ApiResponse<FileDtos.FileResponse> update(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id, @RequestBody FileDtos.UpdateFileRequest req) {
    var ctx = families.require(familyId);
    return ApiResponse.ok(FileDtos.FileResponse.from(service.update(id, req, ctx)));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<String> delete(@RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    var ctx = families.require(familyId);
    service.delete(id, ctx);
    return ApiResponse.ok("Deleted");
  }
}
