package com.famora.note.dto;

import com.famora.common.helper.Visibility;
import com.famora.note.helper.NoteType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NoteDetailResponse(
    UUID id,
    String title,
    String content,
    String category,
    Visibility visibility,
    NoteType noteType,
    Map<String, Object> contentJson,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
