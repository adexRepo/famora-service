package com.famora.emergency.dto;

import com.famora.emergency.entity.EmergencyContact;
import com.famora.emergency.helper.EmergencyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EmergencyDtos {
  
  public record Request(
      @NotBlank @Size(max = 150) String name,
      @NotBlank @Size(max = 50) String phone,
      @NotNull EmergencyCategory category,
      @Size(max = 150) String location,
      @Size(max = 2000) String notes) {
    
  }
  
  public record Response(UUID id, String name, String phone, EmergencyCategory category,
                         String location, String notes, OffsetDateTime createdAt,
                         OffsetDateTime updatedAt) {
    
    public static Response from(EmergencyContact e) {
      return new Response(e.getId(), e.getName(), e.getPhone(), e.getCategory(), e.getLocation(),
          e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
  }
}
