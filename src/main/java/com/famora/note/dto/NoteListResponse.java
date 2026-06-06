package com.famora.note.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteListResponse(
    UUID id,
    String title,
    String category,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
