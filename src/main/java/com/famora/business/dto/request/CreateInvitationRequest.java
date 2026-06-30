package com.famora.business.dto.request;

import com.famora.business.enums.BusinessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateInvitationRequest(@Email @Size(max = 150) String invitedEmail,
                                      @Size(max = 50) String invitedPhone,
                                      @NotNull BusinessRole role,
                                      LocalDateTime expiresAt) {}
