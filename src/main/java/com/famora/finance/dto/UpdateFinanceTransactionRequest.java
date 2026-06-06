package com.famora.finance.dto;

import com.famora.finance.entity.FinanceTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFinanceTransactionRequest(
    @NotNull
    FinanceTransactionType type,
    
    @NotNull
    @DecimalMin("0.01")
    BigDecimal amount,
    
    @NotBlank
    @Size(min = 3, max = 3)
    String currency,
    
    @NotBlank
    @Size(max = 100)
    String category,
    
    String description,
    
    @NotNull
    LocalDate transactionDate
) {

}
