package com.famora.business.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.DailyReportStatus;
import com.famora.common.exception.BusinessDailyReportAccessDeniedException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessDailyReportWorkflowValidatorTest {

  private final BusinessDailyReportWorkflowValidator validator =
      new BusinessDailyReportWorkflowValidator();

  @Test
  void managerCanSubmitOwnReport() {
    UUID managerId = UUID.randomUUID();
    BusinessDailyReport report = draftReportedBy(managerId);

    assertThatCode(() -> validator.ensureCanSubmit(report, BusinessRole.MANAGER, managerId))
        .doesNotThrowAnyException();
  }

  @Test
  void managerCannotSubmitAnotherMembersReport() {
    BusinessDailyReport report = draftReportedBy(UUID.randomUUID());

    assertThatThrownBy(
        () -> validator.ensureCanSubmit(report, BusinessRole.MANAGER, UUID.randomUUID()))
        .isInstanceOf(BusinessDailyReportAccessDeniedException.class);
  }

  @Test
  void ownerCanSubmitAnotherMembersReport() {
    BusinessDailyReport report = draftReportedBy(UUID.randomUUID());

    assertThatCode(
        () -> validator.ensureCanSubmit(report, BusinessRole.OWNER, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  private BusinessDailyReport draftReportedBy(UUID userId) {
    BusinessDailyReport report = new BusinessDailyReport();
    report.setReportStatus(DailyReportStatus.DRAFT);
    report.setReportedByUserId(userId);
    return report;
  }
}
