package com.famora.business.controller;

import com.famora.business.constant.BusinessApiMessages;
import com.famora.business.dto.request.RejectReportRequest;
import com.famora.business.dto.request.RequestRevisionRequest;
import com.famora.business.dto.request.SubmitDailyReportRequest;
import com.famora.business.dto.request.VoidReportRequest;
import com.famora.business.dto.response.DailyReportDetailResponse;
import com.famora.business.dto.response.DailyReportRevisionDetailResponse;
import com.famora.business.dto.response.DailyReportRevisionListResponse;
import com.famora.business.dto.response.DailyReportSummaryResponse;
import com.famora.business.dto.response.DailyReportWorkflowResponse;
import com.famora.business.dto.response.SubmitDailyReportResponse;
import com.famora.business.service.BusinessDailyReportPhotoService;
import com.famora.business.service.BusinessDailyReportService;
import com.famora.business.service.BusinessDailyReportWorkflowService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/daily-reports")
@RequiredArgsConstructor
public class BusinessDailyReportController {
  
  private final BusinessDailyReportService reportService;
  private final BusinessDailyReportWorkflowService workflowService;
  private final BusinessDailyReportPhotoService photoService;
  
  @PostMapping
  public ResponseEntity<ApiResponse<DailyReportSummaryResponse>> create(
      @PathVariable UUID businessId,
      @Valid @RequestBody SubmitDailyReportRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(BusinessApiMessages.CREATED,
            reportService.createDraft(businessId, request)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<DailyReportSummaryResponse>> list(@PathVariable UUID businessId,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(reportService.list(businessId, pageable)));
  }
  
  @GetMapping("/{reportId}")
  public ApiResponse<DailyReportDetailResponse> get(@PathVariable UUID businessId,
      @PathVariable UUID reportId) {
    return ApiResponse.ok(reportService.get(businessId, reportId));
  }
  
  @PutMapping("/{reportId}")
  public ApiResponse<DailyReportSummaryResponse> update(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @Valid @RequestBody SubmitDailyReportRequest request) {
    return ApiResponse.ok(reportService.updateDraft(businessId, reportId, request));
  }
  
  @PostMapping(value = "/{reportId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<SubmitDailyReportResponse> submit(@PathVariable UUID businessId,
      @PathVariable UUID reportId) {
    return ApiResponse.ok(workflowService.submitReport(businessId, reportId));
  }
  
  @PostMapping(value = "/{reportId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<SubmitDailyReportResponse> submitWithPhotos(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @RequestPart(value = "photo", required = false) MultipartFile photo,
      @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
    return ApiResponse.ok(workflowService.submitReport(businessId, reportId,
        combinePhotos(photo, photos)));
  }
  
  @GetMapping("/{reportId}/photos")
  public ApiResponse<List<com.famora.business.dto.response.DailyReportPhotoResponse>> photos(
      @PathVariable UUID businessId,
      @PathVariable UUID reportId) {
    return ApiResponse.ok(photoService.listPhotos(businessId, reportId));
  }
  
  @GetMapping("/{reportId}/photos/{photoId}/download")
  public ResponseEntity<Resource> downloadPhoto(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @PathVariable UUID photoId) {
    var download = photoService.download(businessId, reportId, photoId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(download.photo().getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(download.photo().getOriginalName()).build()
                .toString())
        .body(download.resource());
  }
  
  @PostMapping("/{reportId}/request-revision")
  public ApiResponse<DailyReportWorkflowResponse> requestRevision(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @Valid @RequestBody RequestRevisionRequest request) {
    return ApiResponse.ok(
        workflowService.requestRevision(businessId, reportId, request.reason()));
  }
  
  @PostMapping("/{reportId}/approve")
  public ApiResponse<DailyReportWorkflowResponse> approve(@PathVariable UUID businessId,
      @PathVariable UUID reportId) {
    return ApiResponse.ok(workflowService.approveReport(businessId, reportId));
  }
  
  @PostMapping("/{reportId}/reject")
  public ApiResponse<DailyReportWorkflowResponse> reject(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @Valid @RequestBody RejectReportRequest request) {
    return ApiResponse.ok(workflowService.rejectReport(businessId, reportId, request.reason()));
  }
  
  @PostMapping("/{reportId}/void")
  public ApiResponse<DailyReportWorkflowResponse> voidReport(@PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @Valid @RequestBody VoidReportRequest request) {
    return ApiResponse.ok(workflowService.voidReport(businessId, reportId, request.reason()));
  }
  
  @GetMapping("/{reportId}/revisions")
  public ApiResponse<PageResponse<DailyReportRevisionListResponse>> revisions(
      @PathVariable UUID businessId,
      @PathVariable UUID reportId,
      Pageable pageable) {
    return ApiResponse.ok(
        PageResponse.from(workflowService.getRevisionHistory(businessId, reportId, pageable)));
  }
  
  @GetMapping("/{reportId}/revisions/{revisionId}")
  public ApiResponse<DailyReportRevisionDetailResponse> revisionDetail(
      @PathVariable UUID businessId,
      @PathVariable UUID reportId,
      @PathVariable UUID revisionId) {
    return ApiResponse.ok(
        workflowService.getRevisionDetail(businessId, reportId, revisionId));
  }
  
  private List<MultipartFile> combinePhotos(MultipartFile photo, List<MultipartFile> photos) {
    List<MultipartFile> result = new ArrayList<>();
    if (photo != null && !photo.isEmpty()) {
      result.add(photo);
    }
    if (photos != null) {
      photos.stream()
          .filter(item -> item != null && !item.isEmpty())
          .forEach(result::add);
    }
    return result;
  }
}
