package com.famora.business.dto.request;

import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(@NotNull LocalDate expenseDate,
                                   @NotBlank @Size(max = 150) String expenseName,
                                   @NotNull ExpenseCategory category,
                                   @DecimalMin("0.01") BigDecimal quantity,
                                   @Size(max = 50) String unit,
                                   @NotNull @DecimalMin("0.00") BigDecimal amount,
                                   PaymentMethod paymentMethod,
                                   String notes) {}
