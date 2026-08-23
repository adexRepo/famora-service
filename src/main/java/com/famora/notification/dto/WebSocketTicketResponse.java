package com.famora.notification.dto;

import java.time.OffsetDateTime;

public record WebSocketTicketResponse(String ticket, OffsetDateTime expiresAt) {
}
