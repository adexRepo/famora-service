package com.famora.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
    @NotBlank
    @Size(max = 180)
    String title,
    
    @NotBlank
    String content,
    
    @Size(max = 80)
    String category
) {
}
