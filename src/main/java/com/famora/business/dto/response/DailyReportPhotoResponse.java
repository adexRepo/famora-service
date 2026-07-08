package com.famora.business.dto.response;

import com.famora.business.entity.BusinessDailyReportPhoto;
import com.famora.file.helper.FileType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DailyReportPhotoResponse(UUID id,
                                       String originalName,
                                       FileType fileType,
                                       String mimeType,
                                       long fileSize,
                                       String downloadPath,
                                       OffsetDateTime createdAt) {
  
  public static DailyReportPhotoResponse from(BusinessDailyReportPhoto photo) {
    UUID businessId = photo.getBusiness().getId();
    UUID reportId = photo.getDailyReport().getId();
    String downloadPath =
        "api/v1/businesses/%s/daily-reports/%s/photos/%s/download".formatted(
            businessId,
            reportId,
            photo.getId()
        );
    return new DailyReportPhotoResponse(photo.getId(), photo.getOriginalName(),
        photo.getFileType(), photo.getMimeType(), photo.getFileSize(), downloadPath,
        photo.getCreatedAt());
  }
}
