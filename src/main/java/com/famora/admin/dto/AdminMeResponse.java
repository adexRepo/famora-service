package com.famora.admin.dto;

import java.util.UUID;

public record AdminMeResponse(UUID userId, String email, String role) {

}
