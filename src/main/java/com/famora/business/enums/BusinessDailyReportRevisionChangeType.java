package com.famora.business.enums;

/**
 * Full snapshots are stored only for important report lifecycle events.
 * Do not use audit_logs for full report details.
 */
public enum BusinessDailyReportRevisionChangeType {
  CREATED,
  UPDATED_DRAFT,
  SUBMITTED,
  REVISION_REQUESTED,
  REVISION_SUBMITTED,
  APPROVED,
  REJECTED,
  VOIDED
}
