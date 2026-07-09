package com.famora.note.dto;

import com.famora.common.helper.Visibility;
import com.famora.note.helper.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateNoteRequest(
    @NotBlank
    @Size(max = 180)
    String title,
    
    @NotBlank
    String content,
    
    @Size(max = 80)
    String category,
    
    @NotNull
    Visibility visibility,
    
    NoteType noteType,
    
    Map<String, Object> contentJson
) {
}
