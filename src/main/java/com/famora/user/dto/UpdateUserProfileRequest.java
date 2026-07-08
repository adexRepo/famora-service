package com.famora.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateUserProfileRequest(@NotBlank @Size(max = 150) String fullName,
                                       LocalDate dateOfBirth) {}
