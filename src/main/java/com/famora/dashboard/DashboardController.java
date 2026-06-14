package com.famora.dashboard;

import com.famora.common.exception.ApiResponse;
import com.famora.common.helper.Status;
import com.famora.document.repository.DocumentRepository;
import com.famora.emergency.repository.EmergencyContactRepository;
import com.famora.file.entity.FileAsset;
import com.famora.file.repository.FileRepository;
import com.famora.security.FamilyContextService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/com.famora.dashboard")
public class DashboardController {
  
  private final FamilyContextService families;
  private final DocumentRepository documents;
  private final FileRepository files;
  private final EmergencyContactRepository emergencies;
  
  @GetMapping("/summary")
  ApiResponse<DashboardSummary> summary(@RequestHeader("X-Family-Id") String familyId) {
    var ctx = families.require(familyId);
    var fid = ctx.familyId().getId();
    long totalDocuments = documents.countByFamilyIdAndStatus(fid, Status.ACTIVE);
    long totalBackupFiles = files.countByFamilyIdAndStatus(fid, Status.ACTIVE);
    long storageUsedBytes = files.findAllByFamilyIdAndStatus(fid, Status.ACTIVE,
        org.springframework.data.domain.Pageable.unpaged()).stream().mapToLong(
        FileAsset::getFileSize).sum();
    long expiring = documents.countByFamilyIdAndStatusAndExpiryDateBetween(fid, Status.ACTIVE,
        LocalDate.now(), LocalDate.now().plusDays(90));
    long totalEmergency = emergencies.countByFamilyIdAndStatus(fid, Status.ACTIVE);
    return ApiResponse.ok(
        new DashboardSummary(totalDocuments, totalBackupFiles, storageUsedBytes, expiring,
            totalEmergency));
  }
}
