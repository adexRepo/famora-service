package com.famora.note.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    String title,
    String content,
    String category,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
