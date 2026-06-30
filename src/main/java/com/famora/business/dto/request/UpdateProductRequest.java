package com.famora.business.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(@NotBlank @Size(max = 150) String productName,
                                   @Size(max = 80) String category,
                                   @Size(max = 50) String unit,
                                   @NotNull @DecimalMin("0.00") BigDecimal defaultSellingPrice,
                                   @DecimalMin("0.00") BigDecimal costPrice) {}
