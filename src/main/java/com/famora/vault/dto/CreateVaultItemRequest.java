package com.famora.vault.dto;

import com.famora.common.helper.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVaultItemRequest(@NotBlank @Size(max = 180) String title,
                                     @Size(max = 180) String username,
                                     @NotBlank @Size(max = 5000) String secret,
                                     @Size(max = 500) String url,
                                     @Size(max = 2000) String notes,
                                     Visibility visibility) {
  
}
