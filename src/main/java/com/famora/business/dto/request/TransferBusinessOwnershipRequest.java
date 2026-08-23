package com.famora.business.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferBusinessOwnershipRequest(@NotNull UUID newOwnerUserId) {
}
