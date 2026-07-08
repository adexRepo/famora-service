package com.famora.dashboard;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecentActivityResponse(UUID id, UUID userId, String action, String entityType,
                                     UUID entityId, String message, OffsetDateTime createdAt) {}
