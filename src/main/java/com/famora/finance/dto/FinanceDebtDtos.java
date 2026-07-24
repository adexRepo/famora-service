package com.famora.finance.dto;

import com.famora.finance.entity.FinanceDebt;
import com.famora.finance.entity.FinanceDebtPayment;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FinanceDebtDtos {
  
  private FinanceDebtDtos() {
  }
  
  public record CreateDebtRequest(
      @NotNull FinanceDebtType type,
      @NotBlank @Size(max = 180) String counterpartyName,
      @NotNull @DecimalMin("0.01") BigDecimal principalAmount,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotNull LocalDate borrowedDate,
      LocalDate dueDate,
      @Size(max = 2000) String notes
  ) {
  
  }
  
  public record UpdateDebtRequest(
      @NotBlank @Size(max = 180) String counterpartyName,
      LocalDate dueDate,
      @Size(max = 2000) String notes
  ) {
  
  }
  
  public record CreateDebtPaymentRequest(
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull LocalDate paymentDate,
      @Size(max = 2000) String notes
  ) {
  
  }
  
  public record DebtListResponse(
      UUID id,
      FinanceDebtType type,
      String counterpartyName,
      BigDecimal principalAmount,
      BigDecimal paidAmount,
      BigDecimal remainingAmount,
      String currency,
      LocalDate dueDate,
      FinanceDebtStatus status,
      OffsetDateTime updatedAt
  ) {
    public static DebtListResponse from(FinanceDebt debt) {
      return new DebtListResponse(
          debt.getId(),
          debt.getDebtType(),
          debt.getCounterpartyName(),
          debt.getPrincipalAmount(),
          debt.getPaidAmount(),
          debt.getRemainingAmount(),
          debt.getCurrency(),
          debt.getDueDate(),
          debt.getDebtStatus(),
          debt.getUpdatedAt()
      );
    }
  }
  
  public record DebtDetailResponse(
      UUID id,
      FinanceDebtType type,
      String counterpartyName,
      BigDecimal principalAmount,
      BigDecimal paidAmount,
      BigDecimal remainingAmount,
      String currency,
      LocalDate borrowedDate,
      LocalDate dueDate,
      FinanceDebtStatus status,
      String notes,
      UUID attachmentFileId,
      UUID principalFinanceTransactionId,
      List<DebtPaymentResponse> payments,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {
    public static DebtDetailResponse from(FinanceDebt debt, List<FinanceDebtPayment> payments) {
      return new DebtDetailResponse(
          debt.getId(),
          debt.getDebtType(),
          debt.getCounterpartyName(),
          debt.getPrincipalAmount(),
          debt.getPaidAmount(),
          debt.getRemainingAmount(),
          debt.getCurrency(),
          debt.getBorrowedDate(),
          debt.getDueDate(),
          debt.getDebtStatus(),
          debt.getNotes(),
          debt.getAttachmentFile() == null ? null : debt.getAttachmentFile().getId(),
          debt.getPrincipalFinanceTransaction() == null
              ? null
              : debt.getPrincipalFinanceTransaction().getId(),
          payments.stream().map(DebtPaymentResponse::from).toList(),
          debt.getCreatedAt(),
          debt.getUpdatedAt()
      );
    }
  }
  
  public record DebtPaymentResponse(
      UUID id,
      BigDecimal amount,
      LocalDate paymentDate,
      String notes,
      UUID attachmentFileId,
      UUID financeTransactionId,
      OffsetDateTime createdAt
  ) {
    public static DebtPaymentResponse from(FinanceDebtPayment payment) {
      return new DebtPaymentResponse(
          payment.getId(),
          payment.getAmount(),
          payment.getPaymentDate(),
          payment.getNotes(),
          payment.getAttachmentFile() == null ? null : payment.getAttachmentFile().getId(),
          payment.getFinanceTransaction() == null ? null : payment.getFinanceTransaction().getId(),
          payment.getCreatedAt()
      );
    }
  }
}
