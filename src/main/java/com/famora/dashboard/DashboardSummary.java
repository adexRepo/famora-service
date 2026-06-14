package com.famora.dashboard;

public record DashboardSummary(long totalDocuments, long totalBackupFiles, long storageUsedBytes,
                               long expiringDocuments, long totalEmergencyContacts) {
  
}
