package com.famora.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVaultItemRequest(
    @NotBlank
    @Size(max = 180)
    String title,
    
    @Size(max = 180)
    String username,
    
    String secret,
    
    String url,
    
    String notes
) {
}
