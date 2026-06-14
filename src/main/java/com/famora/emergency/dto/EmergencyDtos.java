package com.famora.emergency.dto;

import com.famora.emergency.entity.EmergencyContact;
import com.famora.emergency.helper.EmergencyCategory;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EmergencyDtos {
  
  public record Request(String name, String phone, EmergencyCategory category, String location,
                        String notes) {
    
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
