package com.famora.dashboard;

import com.famora.common.dto.ApiResponse;
import com.famora.common.helper.Status;
import com.famora.document.repository.DocumentRepository;
import com.famora.emergency.repository.EmergencyContactRepository;
import com.famora.file.repository.FileRepository;
import com.famora.security.FamilyContextService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  
  private final FamilyContextService families;
  private final DocumentRepository documents;
  private final FileRepository files;
  private final EmergencyContactRepository emergencies;
  
  @GetMapping("/summary")
  ApiResponse<DashboardSummary> summary(@RequestHeader("X-Family-Id") String familyId) {
    var ctx = families.require(familyId);
    
    UUID fid = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    LocalDate today = LocalDate.now();
    LocalDate next90Days = today.plusDays(90);
    
    long totalDocuments = documents.countVisibleDocuments(
        fid,
        userId,
        isOwner,
        Status.ACTIVE
    );
    
    long totalBackupFiles = files.countVisibleFiles(
        fid,
        userId,
        isOwner,
        Status.ACTIVE
    );
    
    long storageUsedBytes = files.sumVisibleFileSize(
        fid,
        userId,
        isOwner,
        Status.ACTIVE
    );
    
    long expiring = documents.countVisibleExpiringDocuments(
        fid,
        userId,
        isOwner,
        Status.ACTIVE,
        today,
        next90Days
    );
    
    long totalEmergency = emergencies.countByFamilyIdAndStatus(
        fid,
        Status.ACTIVE
    );
    
    return ApiResponse.ok(
        new DashboardSummary(
            totalDocuments,
            totalBackupFiles,
            storageUsedBytes,
            expiring,
            totalEmergency
        )
    );
  }
}
