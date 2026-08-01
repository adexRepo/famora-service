package com.famora.family.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OwnershipTransferRequest(
    @NotNull UUID newOwnerUserId,
    boolean leaveAfterTransfer
) {
}
