package com.famora.business.dto.request;

import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.LossReason;
import com.famora.business.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SubmitDailyReportRequest(@NotNull LocalDate reportDate,
                                       @Size(max = 50) String shift,
                                       @DecimalMin("0.00") BigDecimal dailyCapitalAmount,
                                       String dailyCapitalNote,
                                       String notes,
                                       @NotEmpty @Valid List<SalesItemRequest> salesItems,
                                       @NotEmpty @Valid List<PaymentBreakdownRequest> paymentBreakdowns,
                                       @Valid List<LossItemRequest> lossItems,
                                       @Valid List<ExpenseItemRequest> expenses) {
  
  public record SalesItemRequest(UUID productId,
                                 @Size(max = 150) String itemName,
                                 @Size(max = 50) String unit,
                                 @NotNull @DecimalMin("0.01") BigDecimal quantitySold,
                                 @DecimalMin("0.00") BigDecimal unitPrice,
                                 String notes) {
    
  }
  
  public record PaymentBreakdownRequest(@NotNull PaymentMethod paymentMethod,
                                        @NotNull @DecimalMin("0.00") BigDecimal amount,
                                        String notes) {
    
  }
  
  public record LossItemRequest(@NotBlank @Size(max = 150) String itemName,
                                @Size(max = 50) String unit,
                                @NotNull @DecimalMin("0.01") BigDecimal quantityLoss,
                                @NotNull @DecimalMin("0.00") BigDecimal estimatedUnitValue,
                                LossReason reason,
                                String notes) {
    
  }
  
  public record ExpenseItemRequest(@NotNull LocalDate expenseDate,
                                   @NotBlank @Size(max = 150) String expenseName,
                                   @NotNull ExpenseCategory category,
                                   @DecimalMin("0.01") BigDecimal quantity,
                                   @Size(max = 50) String unit,
                                   @NotNull @DecimalMin("0.00") BigDecimal amount,
                                   PaymentMethod paymentMethod,
                                   String notes) {
    
  }
}
