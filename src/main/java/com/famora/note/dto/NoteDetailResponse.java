package com.famora.note.dto;

import com.famora.common.helper.Visibility;
import com.famora.note.helper.NoteType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteDetailResponse(
    UUID id,
    String title,
    String content,
    String category,
    Visibility visibility,
    NoteType noteType,
    JsonNode contentJson,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
