package com.famora.admin.dto;

import java.util.UUID;

public record AdminBootstrapResponse(UUID userId, String email, String role) {

}
