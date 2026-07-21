package com.famora.tracker.service;

import com.famora.business.dto.response.LookupItemResponse;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerFrequency;
import com.famora.tracker.enums.TrackerLogStatus;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerType;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrackerLookupService {
  
  public List<LookupItemResponse> scopeTypes() {
    return fromEnum(TrackerScopeType.values());
  }
  
  public List<LookupItemResponse> sourceModules() {
    return fromEnum(TrackerSourceModule.values());
  }
  
  public List<LookupItemResponse> trackerTypes() {
    return fromEnum(TrackerType.values());
  }
  
  public List<LookupItemResponse> categories() {
    return fromEnum(TrackerCategory.values());
  }
  
  public List<LookupItemResponse> frequencies() {
    return fromEnum(TrackerFrequency.values());
  }
  
  public List<LookupItemResponse> logStatuses() {
    return fromEnum(TrackerLogStatus.values());
  }
  
  public List<LookupItemResponse> notificationStatuses() {
    return fromEnum(NotificationDeliveryStatus.values());
  }
  
  private List<LookupItemResponse> fromEnum(Enum<?>[] values) {
    return Arrays.stream(values)
        .map(value -> new LookupItemResponse(value.name(), label(value.name())))
        .toList();
  }
  
  private String label(String value) {
    String lower = value.replace('_', ' ').toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
