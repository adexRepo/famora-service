package com.famora.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(@NotBlank @Size(max = 150) String name) {}
