package com.famora.note.dto;

import com.famora.common.helper.Visibility;
import com.famora.note.helper.NoteType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteListResponse(
    UUID id,
    String title,
    String category,
    Visibility visibility,
    NoteType noteType,
    String contentPreview,
    UUID createdByUserId,
    String createdByName,
    boolean canUpdate,
    boolean canDelete,
    boolean canToggleChecklist,
    OffsetDateTime updatedAt
) {
}
