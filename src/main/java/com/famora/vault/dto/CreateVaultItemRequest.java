package com.famora.vault.dto;

import com.famora.common.helper.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVaultItemRequest(@NotBlank @Size(max = 180) String title,
                                     @Size(max = 180) String username,
                                     @NotBlank String secret,
                                     String url,
                                     String notes,
                                     @NotBlank
                                     Visibility visibility) {
  
}
