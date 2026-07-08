package com.famora.business.service;

import com.famora.business.dto.response.DailyReportPhotoResponse;
import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.entity.BusinessDailyReportPhoto;
import com.famora.business.repository.BusinessDailyReportPhotoRepository;
import com.famora.business.repository.BusinessDailyReportRepository;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import com.famora.file.dto.StoredFile;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
import com.famora.file.service.StorageService;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BusinessDailyReportPhotoService {
  
  private static final String STORAGE_FOLDER = "daily_reports";
  
  private final BusinessDailyReportPhotoRepository photoRepository;
  private final BusinessDailyReportRepository reportRepository;
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final StorageService storageService;
  
  @Value("${app.storage.type:MINIO}")
  private StorageType defaultStorageType;
  
  public List<DailyReportPhotoResponse> saveSubmitPhotos(BusinessDailyReport report, User user,
      List<MultipartFile> photos) {
    if (photos == null || photos.isEmpty()) {
      return List.of();
    }
    
    return photos.stream()
        .filter(photo -> photo != null && !photo.isEmpty())
        .map(photo -> savePhoto(report, user, photo))
        .map(DailyReportPhotoResponse::from)
        .toList();
  }
  
  @Transactional(readOnly = true)
  public List<DailyReportPhotoResponse> listPhotos(UUID businessId, UUID reportId) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return photoRepository
        .findByBusiness_IdAndDailyReport_IdAndStatusOrderByCreatedAtAsc(businessId, reportId,
            Status.ACTIVE)
        .stream()
        .map(DailyReportPhotoResponse::from)
        .toList();
  }
  
  @Transactional(readOnly = true)
  public Download download(UUID businessId, UUID reportId, UUID photoId) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    reportRepository.findByIdAndBusinessId(businessId, reportId)
        .orElseThrow(() -> BusinessException.notFound("Daily report not found"));
    BusinessDailyReportPhoto photo = photoRepository
        .findByIdAndBusiness_IdAndDailyReport_IdAndStatus(photoId, businessId, reportId,
            Status.ACTIVE)
        .orElseThrow(() -> BusinessException.notFound("Daily report photo not found"));
    
    String objectKeyOrPath = photo.getStorageType() == StorageType.MINIO
        ? photo.getObjectKey()
        : photo.getStoragePath();
    Resource resource = storageService.load(photo.getStorageType(), photo.getBucketName(),
        objectKeyOrPath);
    return new Download(photo, resource);
  }
  
  private BusinessDailyReportPhoto savePhoto(BusinessDailyReport report, User user,
      MultipartFile photo) {
    if (!isImage(photo)) {
      throw BusinessException.validation("Daily report photo must be an image file");
    }
    
    StoredFile stored = storageService.storeBusiness(defaultStorageType, photo,
        report.getBusiness().getId(), STORAGE_FOLDER);
    
    BusinessDailyReportPhoto entity = new BusinessDailyReportPhoto();
    entity.setBusiness(report.getBusiness());
    entity.setDailyReport(report);
    entity.setCreatedBy(user);
    entity.setOriginalName(StringUtils.cleanPath(
        photo.getOriginalFilename() == null ? "photo" : photo.getOriginalFilename()));
    entity.setStoredName(stored.storedName());
    entity.setStorageType(defaultStorageType);
    entity.setStoragePath(stored.storagePath());
    entity.setBucketName(stored.bucketName());
    entity.setObjectKey(stored.objectKey());
    entity.setFileType(stored.fileType());
    entity.setMimeType(stored.mimeType());
    entity.setFileSize(photo.getSize());
    entity.setFileHash(stored.sha256());
    
    return photoRepository.save(entity);
  }
  
  private static boolean isImage(MultipartFile file) {
    String contentType = file.getContentType();
    return StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/");
  }
  
  public record Download(BusinessDailyReportPhoto photo, Resource resource) {
    
  }
}
