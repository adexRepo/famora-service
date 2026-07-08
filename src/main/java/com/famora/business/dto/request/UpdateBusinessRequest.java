package com.famora.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateBusinessRequest(@NotBlank @Size(max = 150) String name,
                                    @Size(max = 80) String businessType,
                                    @Size(max = 10) String defaultCurrency,
                                    UUID primaryFamilyId,
                                    String description,
                                    String address,
                                    @Size(max = 80) String contact) {}
