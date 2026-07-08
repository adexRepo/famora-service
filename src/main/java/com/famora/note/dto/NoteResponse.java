package com.famora.note.dto;

import com.famora.common.helper.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    String title,
    String content,
    String category,
    Visibility visibility,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
